"""Bloqueo de la cola, cierre controlado del reintentador y traza de transiciones (auditoría 2088938)."""
import threading
import time

import pytest
import requests

import traza
import vision_agent
from api_client import (ErrorColaBloqueada, PoolHawkApiClient, ReintentadorPendientes)
from cola_eventos import ColaEventos
from shot_analyzer import TiroObservado
from traza import TrazaTiros


class Resp:
    status_code = 200
    text = '{"ok": 1}'

    def json(self):
        return {"ok": 1}


class Sesion:
    """Backend controlable: caído/disponible, con latencia opcional por petición."""

    def __init__(self, caido=True, latencia=0.0):
        self.headers, self.caido, self.latencia, self.posts = {}, caido, latencia, []

    def request(self, metodo, url, **kw):
        if metodo == "GET":
            return type("G", (), {"status_code": 200, "text": "x", "json": lambda s: {
                "id": "m1", "jugadorActualId": "j1", "cantidadTiros": 0}})()
        self.posts.append(kw["json"])
        if self.latencia:
            time.sleep(self.latencia)
        if self.caido:
            raise requests.ConnectionError()
        return Resp()


def tiro(event_id):
    return TiroObservado(2, [5], ["TOP_LEFT"], 3, False, [], event_id)


def montar(sesion, max_intentos=2, **kw):
    cola = ColaEventos()
    cliente = PoolHawkApiClient("http://x", espera=0, max_reintentos=1, sesion=sesion, cola=cola,
                                max_intentos_evento=max_intentos, **kw)
    return cola, cliente


def agotar(cliente, pasadas=2):
    for _ in range(pasadas):
        cliente.reenviar_pendientes(reintentos=1)


# ------------------------------------------------------------------ bloqueo de la cola (H-03)
def test_agotar_reintentos_bloquea_y_no_envia_los_eventos_posteriores():
    s = Sesion()
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1", "versionEsperada": 0})
    cola.agregar("m1", {"eventId": "e2", "versionEsperada": 1})
    agotar(cliente)
    assert cola.bloqueo["eventId"] == "e1" and len(cola.pendientes()) == 2
    antes = len(s.posts)
    s.caido = False                      # aunque la API vuelva, la cola sigue bloqueada
    assert cliente.reenviar_pendientes()["bloqueada"] == 1
    assert len(s.posts) == antes         # e2 NO se envía con una versión obsoleta


def test_tiro_nuevo_con_la_cola_bloqueada_no_se_encola_ni_consulta_la_partida():
    s = Sesion()
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1"})
    agotar(cliente)
    with pytest.raises(ErrorColaBloqueada):
        cliente.registrar_tiro("m1", tiro("e3"))
    assert not cola.contiene("e3") and traza_estado(cliente, tiro("e4")) == traza.BLOQUEADO


def traza_estado(cliente, t):
    return vision_agent.manejar_tiro(t, cliente, "m1")


def test_reanudar_reintenta_el_mismo_evento_desde_cero():
    s = Sesion()
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1"})
    agotar(cliente)
    s.caido = False
    assert cola.reanudar() == "e1" and cola.bloqueo is None
    assert cola.pendientes()[0]["intentos"] == 0
    assert cliente.reenviar_pendientes()["enviados"] == 1 and cola.pendientes() == []


def test_descartar_el_evento_bloqueante_libera_la_cola():
    s = Sesion()
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1"})
    agotar(cliente)
    cola.rechazar("e1", "descartado manualmente")
    assert cola.bloqueo is None and cola.rechazados()[0]["motivo"] == "descartado manualmente"


def test_el_bloqueo_sobrevive_a_un_reinicio(tmp_path):
    ruta = str(tmp_path / "c.json")
    cola = ColaEventos(ruta)
    cola.agregar("m1", {"eventId": "e1"})
    cola.bloquear("e1", "motivo")
    assert ColaEventos(ruta).bloqueo == {"eventId": "e1", "motivo": "motivo"}


def test_el_reintentador_no_insiste_con_la_cola_bloqueada():
    s = Sesion()
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1"})
    agotar(cliente)
    n = len(s.posts)
    assert ReintentadorPendientes(cliente, base=1, maximo=2).intentar() is None
    assert len(s.posts) == n


# ------------------------------------------------------------------ cierre controlado (H-04)
def test_detener_espera_a_la_peticion_en_curso_y_contabiliza_pendientes():
    s = Sesion(caido=False, latencia=0.4)
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1"})
    r = ReintentadorPendientes(cliente, base=0.01, maximo=0.02, sondeo=0.01)
    r.iniciar()
    for _ in range(100):                 # esperar a que la petición esté en vuelo
        if s.posts:
            break
        time.sleep(0.01)
    inicio = time.monotonic()
    cierre = r.detener(drenar=False)
    assert cierre["terminado"] is True and time.monotonic() - inicio >= 0.2  # esperó, no abandonó
    assert cierre == {"terminado": True, "pendientes": 0}


def test_detener_con_timeout_corto_informa_que_no_termino():
    s = Sesion(caido=False, latencia=0.6)
    cola, cliente = montar(s)
    cola.agregar("m1", {"eventId": "e1"})
    r = ReintentadorPendientes(cliente, base=0.01, maximo=0.02, sondeo=0.01)
    r.iniciar()
    for _ in range(100):
        if s.posts:
            break
        time.sleep(0.01)
    cierre = r.detener(timeout=0.05)
    assert cierre["terminado"] is False
    r.detener(timeout=2)


def test_detener_hace_una_pasada_final_y_cuenta_lo_que_queda():
    s = Sesion(caido=True)
    cola, cliente = montar(s, max_intentos=50)
    cola.agregar("m1", {"eventId": "e1"})
    r = ReintentadorPendientes(cliente, base=100, maximo=100)
    cierre = r.detener()
    assert cierre == {"terminado": True, "pendientes": 1} and len(s.posts) == 1


def test_main_devuelve_4_si_cierra_con_pendientes(tmp_path, monkeypatch):
    class FalsoReintentador:
        def __init__(self, *a, **k):
            pass

        def iniciar(self):
            pass

        def detener(self):
            return {"terminado": True, "pendientes": 2}

    monkeypatch.setattr(vision_agent, "ReintentadorPendientes", FalsoReintentador)
    import cv2
    import numpy as np
    video = str(tmp_path / "v.mp4")
    w = cv2.VideoWriter(video, cv2.VideoWriter_fourcc(*"mp4v"), 10, (160, 120))
    for _ in range(3):
        w.write(np.zeros((120, 160, 3), np.uint8))
    w.release()
    codigo = vision_agent.main(["--source", video, "--no-gui", "--match-id", "m1",
                                "--api-url", "http://127.0.0.1:9", "--cola", str(tmp_path / "c.json"),
                                "--traza", str(tmp_path / "t.jsonl")])
    assert codigo == 4


# ------------------------------------------------------------------ estados y traza (H-02, H-07)
def test_get_fallido_se_traza_como_estado_no_disponible_y_no_como_pendiente(tmp_path):
    s = Sesion()
    s.request = lambda m, u, **k: (_ for _ in ()).throw(requests.ConnectionError())
    cola, cliente = montar(s)
    ruta = str(tmp_path / "t.jsonl")
    estado = vision_agent.manejar_tiro(tiro("e9"), cliente, "m1", TrazaTiros(ruta))
    assert estado == traza.ESTADO_NO_DISPONIBLE and cola.pendientes() == []
    assert traza.estado_actual(ruta) == {"e9": traza.ESTADO_NO_DISPONIBLE}


def test_evento_confirmado_por_el_reintentador_deja_la_traza_en_enviado(tmp_path):
    s = Sesion(caido=True)
    cola, cliente = montar(s, max_intentos=50)
    ruta = str(tmp_path / "t.jsonl")
    registro = TrazaTiros(ruta)
    assert vision_agent.manejar_tiro(tiro("e5"), cliente, "m1", registro) == traza.PENDIENTE
    assert traza.estado_actual(ruta)["e5"] == traza.PENDIENTE

    def anotar(event_id, estado, detalle):
        registro.transicion(event_id, estado, "m1", detalle)

    cliente.reenviar_pendientes(reintentos=1, observador=anotar)          # sigue caído -> REINTENTO
    assert traza.estado_actual(ruta)["e5"] == traza.REINTENTO
    s.caido = False
    cliente.reenviar_pendientes(reintentos=1, observador=anotar)          # confirma -> ENVIADO
    assert traza.estado_actual(ruta)["e5"] == traza.ENVIADO
    assert [l for l in open(ruta, encoding="utf-8")][0].count("OBSERVADO") == 1


def test_gestionar_cola_reanuda_y_descarta(tmp_path):
    ruta = str(tmp_path / "c.json")
    cola = ColaEventos(ruta)
    cola.agregar("m1", {"eventId": "e1"})
    cola.bloquear("e1", "x")

    class A:  # argumentos mínimos
        reanudar_cola, descartar_evento, cola, traza = True, None, ruta, str(tmp_path / "t.jsonl")

    assert vision_agent.gestionar_cola(A) == 0 and ColaEventos(ruta).bloqueo is None
    A.reanudar_cola, A.descartar_evento = False, "e1"
    assert vision_agent.gestionar_cola(A) == 0 and ColaEventos(ruta).pendientes() == []
    A.descartar_evento = None
    assert vision_agent.gestionar_cola(A) is None
