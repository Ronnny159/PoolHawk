"""Pruebas de la cola persistente de eventos."""
import json
import os
import sys

import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from cola_eventos import ColaEventos  # noqa: E402


def test_agregar_no_duplica_el_mismo_event_id():
    cola = ColaEventos()
    cola.agregar("m1", {"eventId": "a"})
    cola.agregar("m1", {"eventId": "a"})
    assert len(cola.pendientes()) == 1


def test_persiste_y_se_recupera_desde_disco(tmp_path):
    ruta = str(tmp_path / "c.json")
    ColaEventos(ruta).agregar("m1", {"eventId": "a", "jugadorId": "j"})
    recuperada = ColaEventos(ruta)
    assert recuperada.pendientes()[0]["payload"]["jugadorId"] == "j"


def test_resolver_y_rechazar_actualizan_el_archivo(tmp_path):
    ruta = str(tmp_path / "c.json")
    cola = ColaEventos(ruta)
    cola.agregar("m1", {"eventId": "a"})
    cola.agregar("m1", {"eventId": "b"})
    cola.resolver("a")
    cola.rechazar("b", "400")
    datos = json.load(open(ruta, encoding="utf-8"))
    assert datos["pendientes"] == [] and datos["rechazados"][0]["motivo"] == "400"


def test_archivo_corrupto_se_aparta_y_avisa(tmp_path):
    ruta = tmp_path / "c.json"
    ruta.write_text("{ no es json", encoding="utf-8")
    with pytest.raises(ValueError):
        ColaEventos(str(ruta))
    assert (tmp_path / "c.json.corrupto").exists()
