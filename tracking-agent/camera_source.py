"""
Origen de vídeo del agente (Fase 3 de la auditoría, H-08).

Permite usar una webcam (índice), DroidCam/IP (URL) o un vídeo grabado (ruta) sin tocar
el código, lo que además hace reproducibles las pruebas con grabaciones.
"""
from __future__ import annotations

from typing import Iterator, Union

import cv2
import numpy as np


def abrir_fuente(fuente: Union[int, str]) -> cv2.VideoCapture:
    """Abre una fuente de vídeo.

    Args:
        fuente: índice de cámara (``0``, ``"1"``), ruta de archivo o URL.

    Raises:
        RuntimeError: si no se pudo abrir la fuente.
    """
    if isinstance(fuente, str) and fuente.strip().isdigit():
        fuente = int(fuente.strip())
    cap = cv2.VideoCapture(fuente)
    if not cap.isOpened():
        cap.release()
        raise RuntimeError(f"No se pudo abrir la fuente de vídeo: {fuente!r}")
    return cap


def leer_frames(cap: cv2.VideoCapture, max_frames: int = 0) -> Iterator[np.ndarray]:
    """Genera fotogramas hasta que se acabe la fuente o se alcance ``max_frames``.

    Args:
        cap: captura abierta con :func:`abrir_fuente`.
        max_frames: límite de fotogramas (0 = sin límite).
    """
    leidos = 0
    while max_frames == 0 or leidos < max_frames:
        ok, frame = cap.read()
        if not ok or frame is None:
            return
        leidos += 1
        yield frame
