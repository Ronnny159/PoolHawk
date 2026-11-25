from __future__ import annotations

import os
from typing import Iterator, Union

import cv2
import numpy as np


def abrir_fuente(fuente: Union[int, str]) -> cv2.VideoCapture:
    if isinstance(fuente, str):
        if os.path.exists(fuente):
            pass
        elif fuente.strip().isdigit():
            fuente = int(fuente.strip())
    cap = cv2.VideoCapture(fuente)
    if not cap.isOpened():
        cap.release()
        raise RuntimeError(f"No se pudo abrir la fuente de video: {fuente!r}")
    return cap


def leer_frames(cap: cv2.VideoCapture, max_frames: int = 0) -> Iterator[np.ndarray]:
    leidos = 0
    while max_frames == 0 or leidos < max_frames:
        ok, frame = cap.read()
        if not ok or frame is None:
            return
        leidos += 1
        yield frame