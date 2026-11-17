"""Reintento periódico de eventos pendientes con espera incremental (H-03)."""
import os
import sys
import threading
import time

import pytest
import requests

from api_client import PoolHawkApiClient, ReintentadorPendientes
from cola_eventos import ColaEventos


class Resp:
    status_code = 200
    text = '{"ok": 1}'

    def json(self):
        return {"ok": 1}


class SesionControlable:
    """Simula un backend que puede estar caído o disponible."""

    def __init__(self):
        self.headers, self.caido, self.posts = {}, True, []

    def request(self, metodo, url, **kw):
        self.posts.append(kw.get("json"))
        if self.caido:
            raise requests.ConnectionError()
        return Resp()


class Reloj:
    def __init__(self):
        self.t = 100.0

    def __call__(self):
        return self.t


def montar(max_intentos_evento=50):
    sesion, cola, reloj = SesionControlable(), ColaEventos(), Reloj()
    cliente = PoolHawkApiClient("http://x", espera=0, sesion=sesion, cola=cola,
                                max_intentos_evento=max_intentos_evento)
    cola.agregar("m1", {"eventId": "e1", "jugadorId": "j", "versionEsperada": 4})
    return sesion, cola, reloj, ReintentadorPendientes(cliente, base=2, maximo=10, reloj=reloj)


def test_espera_incremental_hasta_el_maximo_y_se_reinicia_al_recuperarse():
    sesion, cola, reloj, r = montar()
    esperas = []
    for _ in range(5):
        r.intentar()
        esperas.append(r.espera_actual)
        reloj.t += 100
    assert esperas == [4, 8, 10, 10, 10]
    sesion.caido = False
    r.intentar()
    assert cola.pendientes() == [] and r.espera_actual == 2


def test_no_reintenta_antes_de_tiempo():
    sesion, _, reloj, r = montar()
    assert r.intentar() is not None
    n = len(sesion.posts)
    reloj.t += 1  # menos que la espera
    assert r.intentar() is None and len(sesion.posts) == n


def test_reenvia_exactamente_el_mismo_evento_cuando_vuelve_la_api():
    sesion, cola, reloj, r = montar()
    r.intentar()
    sesion.caido = False
    reloj.t += 100
    assert r.intentar()["enviados"] == 1
    assert {p["eventId"] for p in sesion.posts} == {"e1"}
    assert sesion.posts[0] == sesion.posts[-1]


def test_un_solo_intento_http_por_pasada():
    sesion, _, _, r = montar()
    r.intentar()
    assert len(sesion.posts) == 1


def test_limite_de_intentos_bloquea_la_cola_sin_descartar_el_evento():
    sesion, cola, reloj, r = montar(max_intentos_evento=3)
    resultados = []
    for _ in range(3):
        resultados.append(r.intentar())
        reloj.t += 100
    assert resultados[-1]["bloqueada"] == 1
    assert [i["payload"]["eventId"] for i in cola.pendientes()] == ["e1"]  # NO se descarta
    assert cola.rechazados() == [] and "reintentos agotados" in cola.bloqueo["motivo"]


def test_sin_pendientes_no_hace_nada():
    cola = ColaEventos()
    cliente = PoolHawkApiClient("http://x", sesion=SesionControlable(), cola=cola)
    assert ReintentadorPendientes(cliente).intentar() is None


def test_hilo_reenvia_sin_reiniciar_el_agente():
    sesion = SesionControlable()
    cola = ColaEventos()
    cola.agregar("m1", {"eventId": "e1"})
    cliente = PoolHawkApiClient("http://x", espera=0, sesion=sesion, cola=cola)
    r = ReintentadorPendientes(cliente, base=0.05, maximo=0.1, sondeo=0.02)
    r.iniciar()
    try:
        time.sleep(0.2)
        assert cola.pendientes()          # API caída: sigue pendiente
        sesion.caido = False
        for _ in range(100):
            if not cola.pendientes():
                break
            time.sleep(0.05)
        assert cola.pendientes() == []
    finally:
        r.detener()


@pytest.mark.parametrize("kw", [{"base": 0}, {"base": 5, "maximo": 1}, {"sondeo": 0}])
def test_configuracion_invalida(kw):
    cliente = PoolHawkApiClient("http://x", sesion=SesionControlable())
    with pytest.raises(ValueError):
        ReintentadorPendientes(cliente, **kw)


# ----------------------------------------------------------- cola: concurrencia y escritura (H-11)
def test_cola_concurrente_no_pierde_ni_corrompe_eventos(tmp_path):
    ruta = str(tmp_path / "c.json")
    cola = ColaEventos(ruta)

    def productor(k):
        for i in range(25):
            cola.agregar("m1", {"eventId": f"{k}-{i}"})
            if i % 2:
                cola.resolver(f"{k}-{i - 1}")

    hilos = [threading.Thread(target=productor, args=(k,)) for k in range(6)]
    [h.start() for h in hilos]
    [h.join() for h in hilos]
    esperados = {f"{k}-{i}" for k in range(6) for i in range(25) if i % 2 == 1 or i == 24} - \
        {f"{k}-{i}" for k in range(6) for i in range(0, 24, 2)}
    assert {i["payload"]["eventId"] for i in cola.pendientes()} == esperados
    assert {i["payload"]["eventId"] for i in ColaEventos(ruta).pendientes()} == esperados  # disco coherente


def test_escritura_interrumpida_deja_intacto_el_archivo_anterior(tmp_path, monkeypatch):
    ruta = str(tmp_path / "c.json")
    cola = ColaEventos(ruta)
    cola.agregar("m1", {"eventId": "a"})
    antes = open(ruta, encoding="utf-8").read()

    def falla(*a, **k):
        raise OSError("disco lleno")

    monkeypatch.setattr(os, "replace", falla)
    with pytest.raises(OSError):
        cola.agregar("m1", {"eventId": "b"})
    monkeypatch.undo()
    assert open(ruta, encoding="utf-8").read() == antes
    assert [f for f in os.listdir(tmp_path) if f.endswith(".tmp")] == []
    assert [i["payload"]["eventId"] for i in ColaEventos(ruta).pendientes()] == ["a"]


def test_registrar_intento_se_persiste(tmp_path):
    ruta = str(tmp_path / "c.json")
    cola = ColaEventos(ruta)
    cola.agregar("m1", {"eventId": "a"})
    assert cola.registrar_intento("a") == 1 and cola.registrar_intento("a") == 2
    assert ColaEventos(ruta).pendientes()[0]["intentos"] == 2
    assert cola.registrar_intento("no-existe") == 0
