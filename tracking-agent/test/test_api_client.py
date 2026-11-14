"""Pruebas del cliente REST con una sesión HTTP simulada (sin red ni backend). Cubre H-01..H-08."""
import json
import os
import sys

import pytest
import requests

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from api_client import (  # noqa: E402
    ErrorApi, ErrorConflicto, ErrorRed, ErrorRespuesta, PoolHawkApiClient, parsear_partida,
)
from cola_eventos import ColaEventos  # noqa: E402
from shot_analyzer import TiroObservado  # noqa: E402


def partida(jugador_id="b1", version=3):
    return {"id": "m1", "fase": "EN_CURSO", "jugadorActual": "Luis", "jugadorActualId": jugador_id,
            "cantidadTiros": version,
            "equipoA": [{"id": "a1", "nombre": "Luis"}], "equipoB": [{"id": "b1", "nombre": "Luis"}]}


class Resp:
    def __init__(self, status=200, data=None, texto=None):
        self.status_code = status
        self._data = data
        self.text = texto if texto is not None else (json.dumps(data) if data is not None else "")

    def json(self):
        if self._data is None:
            raise ValueError("sin json")
        return self._data


class SesionFalsa:
    """GET devuelve la partida configurada; POST responde según un guion."""

    def __init__(self, posts=(), get=None):
        self.headers, self.posts, self.gets = {}, [], 0
        self._guion, self._get = list(posts), get if get is not None else Resp(200, partida())

    def request(self, metodo, url, **kw):
        if metodo == "GET":
            self.gets += 1
            if isinstance(self._get, Exception):
                raise self._get
            return self._get
        self.posts.append(kw["json"])
        r = self._guion.pop(0)
        if isinstance(r, Exception):
            raise r
        return r


def tiro(revision=False, event_id="e1"):
    return TiroObservado(2, [5], ["TOP_LEFT"], 3, revision, ["dudoso"] if revision else [], event_id)


def cliente(sesion, cola=None, **kw):
    return PoolHawkApiClient("http://x", espera=0, sesion=sesion, cola=cola, **kw)


# ---------------------------------------------------------------- H-01 importación
def test_modulo_se_importa_y_el_cliente_se_construye():
    assert cliente(SesionFalsa()).base_url == "http://x"


# ---------------------------------------------------------------- H-05 configuración
@pytest.mark.parametrize("kw", [
    {"base_url": ""}, {"base_url": "localhost:8080"}, {"base_url": "ftp://x"},
    {"max_reintentos": 0}, {"max_reintentos": True}, {"espera": -1}, {"timeout": 0},
])
def test_configuracion_invalida_se_rechaza(kw):
    args = {"base_url": "http://x", **kw}
    with pytest.raises(ValueError):
        PoolHawkApiClient(**args)


def test_token_se_envia_como_bearer():
    s = SesionFalsa()
    PoolHawkApiClient("http://x", token="abc", sesion=s)
    assert s.headers["Authorization"] == "Bearer abc"


# ---------------------------------------------------------------- H-02 identidad por id
def test_jugadores_con_el_mismo_nombre_usan_el_id_correcto():
    s = SesionFalsa([Resp(200, {"ok": 1})], get=Resp(200, partida(jugador_id="b1")))
    cliente(s).registrar_tiro("m1", tiro())
    assert s.posts[0]["jugadorId"] == "b1"  # el 2.º "Luis", no el primero


def test_sin_jugador_actual_id_falla_de_forma_controlada():
    datos = partida()
    del datos["jugadorActualId"]
    with pytest.raises(ErrorRespuesta):
        cliente(SesionFalsa(get=Resp(200, datos))).obtener_estado("m1")


# ---------------------------------------------------------------- H-03 versión del estado
def test_payload_incluye_version_esperada_y_event_id_del_tiro():
    s = SesionFalsa([Resp(200, {"ok": 1})], get=Resp(200, partida(version=7)))
    cliente(s).registrar_tiro("m1", tiro(event_id="evt-9"))
    p = s.posts[0]
    assert p["versionEsperada"] == 7 and p["eventId"] == "evt-9"
    assert p["bolasEmbolsadas"] == [5] and p["troneras"] == ["TOP_LEFT"]


def test_409_se_reporta_como_conflicto_y_no_se_reintenta():
    cola = ColaEventos()
    s = SesionFalsa([Resp(409, texto="turno cambió"), Resp(200, {})])
    with pytest.raises(ErrorConflicto):
        cliente(s, cola).registrar_tiro("m1", tiro())
    assert len(s.posts) == 1 and cola.pendientes() == [] and len(cola.rechazados()) == 1


# ---------------------------------------------------------------- revisión / reintentos
def test_tiro_en_revision_no_se_envia_ni_consulta():
    s = SesionFalsa()
    assert cliente(s).registrar_tiro("m1", tiro(revision=True)) is None
    assert s.posts == [] and s.gets == 0


def test_reintento_por_red_y_5xx_conserva_el_mismo_event_id():
    s = SesionFalsa([requests.ConnectionError(), Resp(500), Resp(200, {"ok": 1})])
    assert cliente(s).registrar_tiro("m1", tiro()) == {"ok": 1}
    assert len(s.posts) == 3 and len({p["eventId"] for p in s.posts}) == 1


def test_4xx_no_se_reintenta():
    s = SesionFalsa([Resp(400, texto="invalido"), Resp(200)])
    with pytest.raises(ErrorApi) as e:
        cliente(s).registrar_tiro("m1", tiro())
    assert e.value.status == 400 and len(s.posts) == 1


# ---------------------------------------------------------------- H-04 idempotencia entre ejecuciones
def test_respuesta_perdida_y_reinicio_reenvian_el_mismo_evento(tmp_path):
    ruta = str(tmp_path / "cola.json")
    s1 = SesionFalsa([requests.Timeout()] * 4)
    with pytest.raises(ErrorRed):
        cliente(s1, ColaEventos(ruta)).registrar_tiro("m1", tiro(event_id="evt-1"))
    # "reinicio": cliente y cola nuevos leyendo el mismo archivo
    s2 = SesionFalsa([Resp(200, {"ok": 1})])
    nuevo = cliente(s2, ColaEventos(ruta))
    assert nuevo.reenviar_pendientes() == {"enviados": 1, "rechazados": 0, "pendientes": 0}
    assert s2.posts[0] == s1.posts[0]  # mismo eventId y mismo payload exacto


def test_reenviar_pendientes_se_detiene_si_la_api_sigue_caida():
    cola = ColaEventos()
    cola.agregar("m1", {"eventId": "a"})
    cola.agregar("m1", {"eventId": "b"})
    s = SesionFalsa([requests.ConnectionError()] * 4)
    assert cliente(s, cola).reenviar_pendientes()["pendientes"] == 2
    assert len(s.posts) == 4  # no insiste con el 2.º evento


def test_registrar_el_mismo_tiro_dos_veces_reutiliza_el_payload_pendiente():
    cola = ColaEventos()
    s = SesionFalsa([requests.Timeout()] * 4 + [Resp(200, {"ok": 1})],
                    get=Resp(200, partida(version=3)))
    c = cliente(s, cola)
    t = tiro(event_id="evt-2")
    with pytest.raises(ErrorRed):
        c.registrar_tiro("m1", t)
    s._get = Resp(200, partida(version=99))  # el estado cambió, pero el evento ya está fijado
    c.registrar_tiro("m1", t)
    assert s.posts[-1]["versionEsperada"] == 3 and s.gets == 1


# ---------------------------------------------------------------- H-06 / H-07 validación
def test_respuesta_get_sin_cuerpo_o_no_json_es_error_controlado():
    with pytest.raises(ErrorRespuesta):
        cliente(SesionFalsa(get=Resp(204))).obtener_estado("m1")
    with pytest.raises(ErrorRespuesta):
        cliente(SesionFalsa(get=Resp(200, None, texto="<html>"))).obtener_estado("m1")


@pytest.mark.parametrize("datos", [None, [], "x", {"id": "m1"}, {**partida(), "cantidadTiros": "3"},
                                   {**partida(), "cantidadTiros": True}, {**partida(), "id": None},
                                   {**partida(), "jugadorActualId": "  "}])
def test_estructura_inesperada_de_partida(datos):
    with pytest.raises(ErrorRespuesta):
        parsear_partida(datos)


def test_post_exitoso_con_cuerpo_ilegible_no_reenvia_el_tiro():
    cola = ColaEventos()
    s = SesionFalsa([Resp(200, None, texto="no es json")])
    assert cliente(s, cola).registrar_tiro("m1", tiro()) == {}
    assert cola.pendientes() == [] and len(s.posts) == 1


def test_get_caido_no_encola_nada():
    cola = ColaEventos()
    with pytest.raises(ErrorRed):
        cliente(SesionFalsa(get=requests.ConnectionError()), cola).registrar_tiro("m1", tiro())
    assert cola.pendientes() == []
