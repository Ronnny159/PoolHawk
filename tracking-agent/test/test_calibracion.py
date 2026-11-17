"""Herramientas de calibración/evaluación (H-06, H-07, H-08) y vídeos reales opcionales (H-10)."""
import glob
import json
import os

import numpy as np
import pytest

import calibracion
from ball_classifier import LISA, Observacion
from ball_detector import ConfigDetector, Deteccion

TRONERAS_OK = {"TOP_LEFT": (10, 10), "TOP_RIGHT": (600, 10), "MIDDLE_LEFT": (10, 240),
               "MIDDLE_RIGHT": (600, 240), "BOTTOM_LEFT": (10, 470), "BOTTOM_RIGHT": (600, 470)}


def test_troneras_validas_no_tienen_problemas_y_se_guardan_en_formato_del_agente(tmp_path):
    assert calibracion.validar_troneras(TRONERAS_OK, 640, 480) == []
    ruta = str(tmp_path / "t.json")
    calibracion.guardar_troneras(TRONERAS_OK, ruta)
    import vision_agent
    assert vision_agent._cargar_troneras(ruta)["TOP_RIGHT"] == (600.0, 10.0)


def test_troneras_detecta_faltantes_fuera_de_imagen_y_clics_repetidos():
    malas = dict(TRONERAS_OK)
    del malas["MIDDLE_LEFT"]
    malas["TOP_RIGHT"] = (900, 10)
    malas["BOTTOM_LEFT"] = (12, 12)  # casi encima de TOP_LEFT
    texto = " | ".join(calibracion.validar_troneras(malas, 640, 480))
    assert "Falta la tronera MIDDLE_LEFT" in texto
    assert "fuera del fotograma" in texto
    assert "clic repetido" in texto


class DetectorFalso:
    def __init__(self, por_frame):
        self.por_frame = list(por_frame)

    def detectar(self, frame):
        return self.por_frame.pop(0)


def test_diagnostico_sugiere_falsos_positivos_y_desconocidas():
    frames = [np.full((100, 100, 3), 128, np.uint8)] * 3  # gris: clasificación DESCONOCIDA
    dets = [[Deteccion(50, 50, 20)] * 20] * 3
    informe = calibracion.diagnosticar(frames, DetectorFalso(dets), bolas_esperadas=16)
    texto = " ".join(informe["sugerencias"])
    assert "FALSOS POSITIVOS" in texto and "DESCONOCIDAS" in texto
    assert informe["detecciones_por_frame"]["media"] == 20


def test_diagnostico_sin_detecciones_lo_avisa():
    informe = calibracion.diagnosticar([np.zeros((50, 50, 3), np.uint8)], DetectorFalso([[]]))
    assert any("ninguna bola" in s for s in informe["sugerencias"])


def _obs(n):
    return Observacion(n, "X", LISA, 0.9)


def test_evaluar_calcula_precision_recall_y_exactitud_de_numero():
    detectado = [[(Deteccion(100, 100, 20), _obs(5)),      # acierto
                  (Deteccion(200, 200, 20), _obs(3)),      # número equivocado (era 7)
                  (Deteccion(400, 400, 20), _obs(1))]]     # falso positivo
    esperado = [[{"x": 102, "y": 99, "numero": 5}, {"x": 201, "y": 200, "numero": 7},
                 {"x": 300, "y": 50, "numero": 2}]]        # falso negativo
    m = calibracion.evaluar_detecciones(detectado, esperado)
    assert (m["tp"], m["fp"], m["fn"]) == (2, 1, 1)
    assert m["precision"] == pytest.approx(0.667, abs=1e-3) and m["recall"] == pytest.approx(0.667, abs=1e-3)
    assert m["exactitud_numero"] == 0.5


def test_evaluar_sin_numero_esperado_no_penaliza_el_numero():
    m = calibracion.evaluar_detecciones([[(Deteccion(10, 10, 20), None)]], [[{"x": 10, "y": 10}]])
    assert m["precision"] == 1.0 and m["exactitud_numero"] == 0.0


def test_cli_con_fuente_inexistente_devuelve_error():
    assert calibracion.main(["diagnostico", "--source", "no_existe.mp4"]) == 1


# ---- H-10: vídeos reales. Opcional: define POOLHAWK_VIDEOS con una carpeta que contenga
# ---- partida.mp4 + partida.esperado.json (ver calibracion.py). Sin ella, se omite.
VIDEOS = sorted(glob.glob(os.path.join(os.environ.get("POOLHAWK_VIDEOS", "__sin_videos__"), "*.esperado.json")))


@pytest.mark.skipif(not VIDEOS, reason="Define POOLHAWK_VIDEOS con grabaciones reales + *.esperado.json")
@pytest.mark.parametrize("esperado", VIDEOS)
def test_video_real_cumple_precision_y_recall_minimos(esperado):
    video = esperado.replace(".esperado.json", ".mp4")
    m = calibracion.evaluar_fuente(video, esperado, ConfigDetector())
    assert m["precision"] >= 0.8 and m["recall"] >= 0.8, json.dumps(m)
