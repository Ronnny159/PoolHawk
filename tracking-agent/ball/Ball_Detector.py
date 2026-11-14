"""
Detección de bolas de billar en un fotograma (Fase 3 de la auditoría, H-04 y H-08).

Separa la localización de círculos de la visualización y de la clasificación. Los
parámetros de ``HoughCircles`` ya no están fijos: viven en :class:`ConfigDetector`, se
pueden escalar según la resolución de la cámara y se puede limitar la búsqueda a la
región de la mesa para reducir falsos positivos.
"""
from __future__ import annotations

from dataclasses import dataclass, replace
from typing import List, Optional, Tuple

import cv2
import numpy as np

RESOLUCION_REFERENCIA = 640  # ancho para el que están pensados los radios por defecto


@dataclass(frozen=True)
class Deteccion:
    """Círculo detectado, en coordenadas del fotograma completo."""

    x: int
    y: int
    r: int


@dataclass(frozen=True)
class ConfigDetector:
    """Parámetros del detector. Todos deben calibrarse con la mesa real.

    Attributes:
        dp, min_dist, param1, param2: parámetros de ``cv2.HoughCircles``.
        min_radius, max_radius: radio en píxeles (dependen de la distancia a la mesa).
        blur: tamaño (impar) del filtro de mediana previo.
        roi_mesa: región ``(x, y, ancho, alto)`` donde buscar; ``None`` = todo el cuadro.
    """

    dp: float = 1.2
    min_dist: int = 30
    param1: int = 50
    param2: int = 30
    min_radius: int = 10
    max_radius: int = 50
    blur: int = 5
    roi_mesa: Optional[Tuple[int, int, int, int]] = None

    def escalada(self, ancho_frame: int) -> "ConfigDetector":
        """Devuelve una copia con radios y distancias escalados al ancho del cuadro."""
        if ancho_frame <= 0:
            raise ValueError("ancho_frame debe ser positivo")
        k = ancho_frame / RESOLUCION_REFERENCIA
        return replace(
            self,
            min_dist=max(1, round(self.min_dist * k)),
            min_radius=max(1, round(self.min_radius * k)),
            max_radius=max(2, round(self.max_radius * k)),
        )


class BallDetector:
    """Localiza círculos candidatos a bola mediante la transformada de Hough."""

    def __init__(self, config: Optional[ConfigDetector] = None):
        self.config = config or ConfigDetector()

    def detectar(self, frame: Optional[np.ndarray]) -> List[Deteccion]:
        """Detecta círculos en un fotograma BGR.

        Returns:
            Lista de :class:`Deteccion` en coordenadas del fotograma original (vacía si
            el cuadro es inválido o no hay círculos).
        """
        if frame is None or frame.ndim != 3 or frame.size == 0:
            return []
        cfg = self.config
        ox = oy = 0
        zona = frame
        if cfg.roi_mesa is not None:
            x, y, w, h = cfg.roi_mesa
            alto, ancho = frame.shape[:2]
            x0, y0 = max(0, x), max(0, y)
            x1, y1 = min(ancho, x + w), min(alto, y + h)
            if x1 - x0 < 2 * cfg.min_radius or y1 - y0 < 2 * cfg.min_radius:
                return []
            zona = frame[y0:y1, x0:x1]
            ox, oy = x0, y0

        gris = cv2.cvtColor(zona, cv2.COLOR_BGR2GRAY)
        gris = cv2.medianBlur(gris, cfg.blur if cfg.blur % 2 == 1 else cfg.blur + 1)
        circulos = cv2.HoughCircles(
            gris, cv2.HOUGH_GRADIENT, dp=cfg.dp, minDist=cfg.min_dist,
            param1=cfg.param1, param2=cfg.param2,
            minRadius=cfg.min_radius, maxRadius=cfg.max_radius,
        )
        if circulos is None:
            return []
        return [
            Deteccion(int(round(c[0])) + ox, int(round(c[1])) + oy, int(round(c[2])))
            for c in circulos[0]
        ]