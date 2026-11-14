"""Pruebas de ``ball_classifier`` con imágenes sintéticas controladas (H-09).

Las imágenes sintéticas validan la LÓGICA (normalización, bordes, umbrales). La
calibración con fotos reales de la mesa debe añadirse como fixtures de regresión.
"""
import os
import sys

import cv2
import numpy as np
import pytest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from ball_classifier import (  # noqa: E402
    BLANCA, DESCONOCIDA, LISA, NEGRA, RAYADA,
    clasificar_bola, recortar_roi,
)

TAM = 100
BLANCO = (255, 255, 255)

# Colores BGR representativos de cada bola lisa
COLORES = {
    1: (0, 255, 255),    # amarillo
    2: (255, 0, 0),      # azul
    3: (0, 0, 255),      # rojo
    4: (200, 0, 150),    # morado
    5: (0, 128, 255),    # naranja
    6: (0, 200, 0),      # verde
    7: (30, 30, 120),    # granate
}


def _bola_lisa(bgr):
    """Bola de un solo color con el círculo blanco del número en el centro."""
    img = np.full((TAM, TAM, 3), bgr, dtype=np.uint8)
    cv2.circle(img, (TAM // 2, TAM // 2), 18, BLANCO, -1)
    return img


def _bola_rayada(bgr):
    """Bola blanca con una banda de color horizontal en el centro."""
    img = np.full((TAM, TAM, 3), BLANCO, dtype=np.uint8)
    img[20:80, :] = bgr
    return img


@pytest.mark.parametrize("numero,bgr", COLORES.items())
def test_lisas(numero, bgr):
    obs = clasificar_bola(_bola_lisa(bgr))
    assert obs.tipo == LISA
    assert obs.numero == numero
    assert obs.es_confiable


@pytest.mark.parametrize("numero,bgr", COLORES.items())
def test_rayadas(numero, bgr):
    obs = clasificar_bola(_bola_rayada(bgr))
    assert obs.tipo == RAYADA
    assert obs.numero == numero + 8
    assert obs.es_confiable


def test_blanca_completamente_blanca_regresion_h01():
    """Un ROI 100% blanco debe dar BLANCA (antes el ratio máximo era ~0.33)."""
    obs = clasificar_bola(np.full((TAM, TAM, 3), 255, dtype=np.uint8))
    assert obs.tipo == BLANCA and obs.numero == 0
    assert obs.confianza > 0.95


def test_negra_con_numero_blanco():
    img = np.full((TAM, TAM, 3), (20, 20, 20), dtype=np.uint8)
    cv2.circle(img, (TAM // 2, TAM // 2), 15, BLANCO, -1)
    obs = clasificar_bola(img)
    assert obs.tipo == NEGRA and obs.numero == 8


def test_oscuro_con_color_no_es_ocho_regresion_h02():
    """Algo oscuro pero con mucho color (azul oscuro) no debe marcarse como 8."""
    img = np.full((TAM, TAM, 3), (120, 20, 20), dtype=np.uint8)  # azul oscuro, V=120
    obs = clasificar_bola(img)
    assert obs.numero != 8


def test_imagen_gris_ambigua_es_desconocida():
    obs = clasificar_bola(np.full((TAM, TAM, 3), 128, dtype=np.uint8))
    assert obs.tipo == DESCONOCIDA
    assert obs.numero is None
    assert not obs.es_confiable


@pytest.mark.parametrize("entrada", [None, np.zeros((0, 0, 3), np.uint8), np.zeros((2, 2, 3), np.uint8)])
def test_entradas_invalidas_no_lanzan_excepcion(entrada):
    assert clasificar_bola(entrada).tipo == DESCONOCIDA


# ---------------------------------------------------------------------------
# recortar_roi (H-03)
# ---------------------------------------------------------------------------
@pytest.mark.parametrize("x,y", [(0, 0), (5, 240), (630, 5), (639, 479), (320, 0)])
def test_roi_junto_a_bordes_no_lanza_ni_usa_indices_negativos(x, y):
    frame = np.full((480, 640, 3), 255, dtype=np.uint8)
    roi = recortar_roi(frame, x, y, 30)
    if roi is not None:
        assert roi.ndim == 3 and roi.shape[0] >= 4 and roi.shape[1] >= 4
        assert roi.shape[0] <= 42 and roi.shape[1] <= 42
        clasificar_bola(roi)  # no debe lanzar


def test_roi_totalmente_fuera_devuelve_none():
    frame = np.zeros((100, 100, 3), np.uint8)
    assert recortar_roi(frame, 500, 500, 20) is None


def test_roi_centrado_tiene_el_tamano_esperado():
    frame = np.zeros((200, 200, 3), np.uint8)
    roi = recortar_roi(frame, 100, 100, 30)  # r_int = 21 -> 42x42
    assert roi.shape[:2] == (42, 42)
