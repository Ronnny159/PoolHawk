"""
Clasificación de bolas de billar por color HSV.

Corrige los hallazgos de la auditoría de la rama ``feature/vision-tracking-agent``:

* H-01: el porcentaje de blanco se normaliza por número de PÍXELES dentro de una
  máscara circular (antes se dividía por ``roi.size``, que cuenta los 3 canales BGR).
* H-02: la bola negra (8) se detecta por separado y exige evidencia, no solo "oscuridad".
* H-03: el recorte del ROI se limita a los bordes de la imagen y valida que no quede vacío.
* H-07: la clasificación devuelve una :class:`Observacion` con confianza; si la confianza
  es baja, la bola queda como ``DESCONOCIDA`` y no debe generar eventos.

IMPORTANTE: los rangos HSV y los umbrales son un punto de partida razonable, NO están
calibrados con la mesa y la iluminación reales. Hay que ajustarlos con imágenes de
referencia (ver ``tests/test_ball_classifier.py``).
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Optional

import cv2
import numpy as np

# ---------------------------------------------------------------------------
# Umbrales (todos son fracciones de PÍXELES dentro del círculo, entre 0 y 1)
# ---------------------------------------------------------------------------
UMBRAL_BLANCA = 0.60          # fracción de blanco para decir "bola blanca"
UMBRAL_NEGRA = 0.55           # fracción de píxeles oscuros para "bola negra"
MAX_COLOR_EN_NEGRA = 0.15     # si hay más color que esto, no es una 8 limpia
UMBRAL_RAYADA = 0.30          # fracción de blanco para considerar rayada
MIN_COLOR_TOTAL = 0.25        # fracción mínima de píxeles de color para clasificar
UMBRAL_CONFIANZA = 0.50       # por debajo de esto la bola es DESCONOCIDA

# Máscaras de píxeles (OpenCV: H 0-179, S 0-255, V 0-255)
BLANCO_S_MAX = 60
BLANCO_V_MIN = 170
NEGRO_V_MAX = 60
COLOR_S_MIN = 80
COLOR_V_MIN = 60

# Rangos de matiz (H) por color. Rojo y granate comparten matiz y se separan por V.
_RANGOS_H = {
    "AMARILLO": [(20, 35)],
    "NARANJA": [(9, 19)],
    "VERDE": [(40, 85)],
    "AZUL": [(100, 125)],
    "MORADO": [(128, 155)],
    "ROJO": [(0, 8), (172, 179)],
    "GRANATE": [(0, 8), (172, 179)],
}
_V_ROJO_MIN = 130  # ROJO: V >= 130; GRANATE: COLOR_V_MIN <= V < 130

# Número de la bola lisa por color (la rayada suma 8).
COLOR_A_NUMERO = {
    "AMARILLO": 1,
    "AZUL": 2,
    "ROJO": 3,
    "MORADO": 4,
    "NARANJA": 5,
    "VERDE": 6,
    "GRANATE": 7,
}

BLANCA = "BLANCA"
NEGRA = "NEGRA"
LISA = "LISA"
RAYADA = "RAYADA"
DESCONOCIDA = "DESCONOCIDA"


@dataclass(frozen=True)
class Observacion:
    """Resultado de clasificar una bola en un fotograma.

    Attributes:
        numero: número estimado (0 = blanca, 8 = negra) o ``None`` si es desconocida.
        color: nombre del color dominante (``"BLANCO"``, ``"NEGRO"`` o el color base).
        tipo: ``BLANCA``, ``NEGRA``, ``LISA``, ``RAYADA`` o ``DESCONOCIDA``.
        confianza: valor entre 0 y 1. Debe ignorarse el resultado si es baja.
    """

    numero: Optional[int]
    color: str
    tipo: str
    confianza: float

    @property
    def es_confiable(self) -> bool:
        """Indica si la observación puede usarse para inferir eventos."""
        return self.tipo != DESCONOCIDA and self.confianza >= UMBRAL_CONFIANZA


def recortar_roi(
    frame: np.ndarray, x: int, y: int, r: int, factor: float = 0.7
) -> Optional[np.ndarray]:
    """Recorta el ROI cuadrado de una bola limitándolo a los bordes del fotograma (H-03).

    Args:
        frame: imagen BGR completa.
        x, y: centro del círculo detectado.
        r: radio del círculo detectado.
        factor: fracción del radio que abarca el recorte (evita el borde de la bola).

    Returns:
        El recorte, o ``None`` si queda vacío o demasiado pequeño para analizarse.
    """
    if frame is None or frame.ndim != 3 or frame.size == 0:
        return None
    alto, ancho = frame.shape[:2]
    r_int = int(r * factor)
    if r_int < 2:
        return None
    x0, x1 = max(0, int(x) - r_int), min(ancho, int(x) + r_int)
    y0, y1 = max(0, int(y) - r_int), min(alto, int(y) + r_int)
    if x1 - x0 < 4 or y1 - y0 < 4:
        return None
    return frame[y0:y1, x0:x1]


def _mascara_circular(alto: int, ancho: int) -> np.ndarray:
    """Máscara uint8 (255 dentro del círculo inscrito, 0 fuera)."""
    mascara = np.zeros((alto, ancho), dtype=np.uint8)
    centro = (ancho // 2, alto // 2)
    cv2.circle(mascara, centro, min(alto, ancho) // 2, 255, -1)
    return mascara


def _rango_h(h: np.ndarray, rangos: list) -> np.ndarray:
    """Máscara booleana de los píxeles cuyo matiz cae en alguno de los rangos."""
    resultado = np.zeros(h.shape, dtype=bool)
    for bajo, alto in rangos:
        resultado |= (h >= bajo) & (h <= alto)
    return resultado


def clasificar_bola(roi: Optional[np.ndarray]) -> Observacion:
    """Clasifica el recorte de una bola y devuelve color, número y confianza.

    Args:
        roi: recorte BGR de la bola (idealmente de :func:`recortar_roi`).

    Returns:
        Una :class:`Observacion`. Con ROI inválido o evidencia insuficiente devuelve
        ``DESCONOCIDA`` con confianza 0 y no debe usarse para generar eventos.
    """
    desconocida = Observacion(None, "DESCONOCIDO", DESCONOCIDA, 0.0)
    if roi is None or roi.ndim != 3 or roi.shape[0] < 4 or roi.shape[1] < 4:
        return desconocida

    alto, ancho = roi.shape[:2]
    dentro = _mascara_circular(alto, ancho) > 0
    total = int(dentro.sum())  # píxeles, no valores BGR (H-01)
    if total == 0:
        return desconocida

    hsv = cv2.cvtColor(roi, cv2.COLOR_BGR2HSV)
    h, s, v = hsv[:, :, 0], hsv[:, :, 1], hsv[:, :, 2]

    blanco = dentro & (s <= BLANCO_S_MAX) & (v >= BLANCO_V_MIN)
    negro = dentro & (v <= NEGRO_V_MAX)
    coloreado = dentro & (s >= COLOR_S_MIN) & (v >= COLOR_V_MIN)

    ratio_blanco = int(blanco.sum()) / total
    ratio_negro = int(negro.sum()) / total
    ratio_color = int(coloreado.sum()) / total

    # Bola negra (8): oscura y casi sin color (H-02)
    if ratio_negro >= UMBRAL_NEGRA and ratio_color <= MAX_COLOR_EN_NEGRA:
        return Observacion(8, "NEGRO", NEGRA, round(ratio_negro, 3))

    # Bola blanca (0)
    if ratio_blanco >= UMBRAL_BLANCA:
        return Observacion(0, "BLANCO", BLANCA, round(ratio_blanco, 3))

    # Bolas de color
    if ratio_color < MIN_COLOR_TOTAL:
        return desconocida

    puntajes = {}
    for nombre, rangos in _RANGOS_H.items():
        pix = coloreado & _rango_h(h, rangos)
        if nombre == "ROJO":
            pix &= v >= _V_ROJO_MIN
        elif nombre == "GRANATE":
            pix &= v < _V_ROJO_MIN
        puntajes[nombre] = int(pix.sum())

    mejor = max(puntajes, key=puntajes.get)
    pix_color = int(coloreado.sum())
    confianza = puntajes[mejor] / pix_color if pix_color else 0.0
    if confianza < UMBRAL_CONFIANZA:
        return Observacion(None, mejor, DESCONOCIDA, round(confianza, 3))

    es_rayada = ratio_blanco >= UMBRAL_RAYADA
    numero = COLOR_A_NUMERO[mejor] + (8 if es_rayada else 0)
    return Observacion(numero, mejor, RAYADA if es_rayada else LISA, round(confianza, 3))