"""Trazabilidad de tiros y decisión de envío del agente (H-12)."""
import json

import pytest

import traza
import vision_agent
from api_client import ErrorApi, ErrorConflicto, ErrorRed
from shot_analyzer import TiroObservado
from traza import TrazaTiros


class ClienteFalso:
    def __init__(self, error=None):
        self.error, self.enviados = error, []

    def registrar_tiro(self, match_id, tiro):
        if self.error:
            raise self.error
        self.enviados.append(tiro.event_id)


def tiro(revision=False):
    return TiroObservado(2, [5], ["TOP_LEFT"], 3, revision, ["dudoso"] if revision else [],
                         "evt-1", 0.87, 10, 42)


def leer(ruta):
    return [json.loads(l) for l in open(ruta, encoding="utf-8")]


@pytest.mark.parametrize("cliente,revision,esperado", [
    (ClienteFalso(), False, traza.ENVIADO),
    (ClienteFalso(ErrorRed("x")), False, traza.PENDIENTE),
    (ClienteFalso(ErrorConflicto("x", 409)), False, traza.CONFLICTO),
    (ClienteFalso(ErrorApi("x", 400)), False, traza.RECHAZADO),
    (ClienteFalso(), True, traza.REVISION),
    (None, False, traza.SIN_API),
])
def test_cada_decision_queda_trazada(tmp_path, cliente, revision, esperado):
    ruta = str(tmp_path / "t.jsonl")
    estado = vision_agent.manejar_tiro(tiro(revision), cliente, "m1", TrazaTiros(ruta))
    assert estado == esperado
    lineas = leer(ruta)
    assert [l["estado"] for l in lineas] == [traza.OBSERVADO, esperado]
    assert {l["eventId"] for l in lineas} == {"evt-1"} and lineas[-1]["matchId"] == "m1"
    assert lineas[0]["confianza"] == 0.87 and (lineas[0]["frameInicio"], lineas[0]["frameFin"]) == (10, 42)
    assert traza.estado_actual(ruta) == {"evt-1": esperado}


def test_un_tiro_en_revision_nunca_se_envia(tmp_path):
    cliente = ClienteFalso()
    vision_agent.manejar_tiro(tiro(True), cliente, "m1", TrazaTiros(str(tmp_path / "t.jsonl")))
    assert cliente.enviados == []


def test_traza_desactivada_no_escribe_nada():
    assert TrazaTiros(None).registrar(tiro(), traza.ENVIADO)["estado"] == traza.ENVIADO
