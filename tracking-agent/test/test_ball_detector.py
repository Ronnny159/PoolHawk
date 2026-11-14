"""Pruebas de ``ball_detector`` (H-04)."""
import cv2
import numpy as np
import pytest

from ball_detector import BallDetector, ConfigDetector

FIELTRO = (60, 110, 40)


def _mesa(bolas, ancho=640, alto=480):
    """Fotograma con fieltro verde y bolas dibujadas como discos con sombreado suave."""
    img = np.full((alto, ancho, 3), FIELTRO, dtype=np.uint8)
    for (x, y, r, bgr) in bolas:
        cv2.circle(img, (x, y), r, bgr, -1)
        cv2.circle(img, (x, y), r, (20, 20, 20), 2)
    return img


def _cerca(dets, x, y, tol=6):
    return any(abs(d.x - x) <= tol and abs(d.y - y) <= tol for d in dets)


def test_detecta_bolas_visibles():
    frame = _mesa([(150, 150, 22, (0, 255, 255)), (400, 300, 22, (255, 255, 255))])
    dets = BallDetector().detectar(frame)
    assert _cerca(dets, 150, 150) and _cerca(dets, 400, 300)


def test_cuadro_invalido_devuelve_vacio():
    det = BallDetector()
    assert det.detectar(None) == []
    assert det.detectar(np.zeros((0, 0, 3), np.uint8)) == []


def test_roi_mesa_excluye_circulos_fuera_y_conserva_coordenadas_globales():
    frame = _mesa([(100, 100, 22, (0, 255, 255)), (450, 300, 22, (255, 255, 255))])
    cfg = ConfigDetector(roi_mesa=(300, 200, 300, 250))
    dets = BallDetector(cfg).detectar(frame)
    assert _cerca(dets, 450, 300)          # coordenadas del cuadro completo
    assert not _cerca(dets, 100, 100)      # fuera de la región de la mesa


def test_roi_mesa_demasiado_pequena_o_fuera_devuelve_vacio():
    frame = _mesa([(100, 100, 22, (0, 255, 255))])
    assert BallDetector(ConfigDetector(roi_mesa=(1000, 1000, 50, 50))).detectar(frame) == []


def test_escalada_ajusta_radios_a_la_resolucion():
    base = ConfigDetector()
    doble = base.escalada(1280)
    assert doble.min_radius == 2 * base.min_radius
    assert doble.max_radius == 2 * base.max_radius
    with pytest.raises(ValueError):
        base.escalada(0)
