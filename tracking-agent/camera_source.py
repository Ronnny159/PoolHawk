from __future__ import annotations

import logging
import os
import sys
from typing import Iterator, Union

import cv2
import numpy as np

logger = logging.getLogger("poolhawk.camera")

_BACKEND_NOMBRES = {
    cv2.CAP_ANY: "CAP_ANY",
    cv2.CAP_DSHOW: "CAP_DSHOW",
    cv2.CAP_MSMF: "CAP_MSMF",
    cv2.CAP_V4L2: "CAP_V4L2",
}


def _get_backend_name(backend: int) -> str:
    return _BACKEND_NOMBRES.get(backend, str(backend))


def _backend_predeterminado() -> int:
    if sys.platform == "win32":
        return cv2.CAP_DSHOW
    if sys.platform.startswith("linux"):
        return cv2.CAP_V4L2
    return cv2.CAP_ANY


class CameraSource:
    """Fuente de video (cámara, archivo o stream)."""

    def __init__(self, fuente: Union[int, str], backend=None):
        self.fuente = fuente

        # Si es una URL (DroidCam, RTSP, archivo remoto), no usar backend numérico
        if isinstance(fuente, str) and (fuente.startswith("http://") or fuente.startswith("https://") or fuente.startswith("rtsp://") or fuente.startswith("rtsps://")):
            self.cap = cv2.VideoCapture(fuente)
            logger.info("Abriendo stream remoto: %s", fuente)
        elif isinstance(fuente, str) and os.path.exists(fuente):
            # Archivo de video local
            self.cap = cv2.VideoCapture(fuente)
            logger.info("Abriendo archivo de video: %s", fuente)
        elif isinstance(fuente, str) and (fuente.endswith(".avi") or fuente.endswith(".mp4") or fuente.endswith(".webm") or fuente.endswith(".mkv") or fuente.endswith(".mov")):
            # Archivo de video que no existe - error específico
            raise RuntimeError(
                f"No se encontró el archivo de video: {fuente}\n"
                f"💡 Verifica que la ruta sea correcta y el archivo exista."
            )
        else:
            # Índice numérico de cámara
            if isinstance(fuente, str) and fuente.strip().isdigit():
                fuente = int(fuente.strip())
            self.backend = backend if backend is not None else _backend_predeterminado()
            self.cap = cv2.VideoCapture(fuente, self.backend)
            logger.info("Abriendo cámara %s con backend %s", fuente, _get_backend_name(self.backend))

        # Validación robusta
        if not self.cap.isOpened():
            self.cap.release()
            camaras = self.detectar_camaras_disponibles()
            if camaras:
                sugerida = camaras[0]
                raise RuntimeError(
                    f"No se pudo abrir la cámara en el índice {fuente}.\n"
                    f"💡 Cámaras disponibles detectadas:\n"
                    f"   - Índice {sugerida['indice']} con backend {sugerida['backend']}\n"
                    f"💡 Usa: python vision_agent.py --source {sugerida['indice']} --backend {sugerida['backend'].split()[0]}"
                )
            else:
                raise RuntimeError(
                    f"No se pudo abrir la cámara en el índice {fuente}.\n"
                    f"❌ No se detectaron cámaras disponibles en el sistema.\n"
                    f"💡 Verifica:\n"
                    f"   1. Que la cámara esté conectada\n"
                    f"   2. Que otra aplicación no la esté usando (Zoom, Teams, Discord)\n"
                    f"   3. Permisos de cámara en el sistema operativo\n"
                    f"   4. Si usas DroidCam, usa la URL HTTP: --source 'http://IP:4747/video'"
                )

        # Verificar que se pueda leer al menos un frame válido
        ok, frame = self.cap.read()
        if not ok or frame is None:
            self.cap.release()
            raise RuntimeError(
                f"La cámara {fuente} se abrió pero no entrega frames válidos.\n"
                f"💡 Posibles causas:\n"
                f"   - La cámara está en uso por otra aplicación\n"
                f"   - Permisos de cámara denegados\n"
                f"   - Driver de cámara incompatible\n"
                f"   - Para streams HTTP/RTSP: verifica la URL y conectividad"
            )

    @classmethod
    def _get_backend_name(cls, backend: int) -> str:
        return _BACKEND_NOMBRES.get(backend, str(backend))

    @classmethod
    def detectar_camaras_disponibles(cls, max_indice=5, backend=None):
        camaras = []
        backends_a_probar = [backend] if backend else [cv2.CAP_DSHOW, cv2.CAP_MSMF, cv2.CAP_ANY]

        for i in range(max_indice):
            for b in backends_a_probar:
                try:
                    cap = cv2.VideoCapture(i, b)
                    if cap.isOpened():
                        width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                        height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                        camaras.append({
                            'indice': i,
                            'backend': cls._get_backend_name(b),
                            'backend_code': b,
                            'resolucion': (width, height)
                        })
                        cap.release()
                        break
                except Exception:
                    pass

        return camaras

    def read(self):
        return self.cap.read()

    def release(self):
        self.cap.release()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.release()


def abrir_fuente(fuente: Union[int, str], backend: int = cv2.CAP_ANY) -> cv2.VideoCapture:
    """
    Función de compatibilidad hacia atrás.
    Usa CameraSource internamente y devuelve el cv2.VideoCapture.
    """
    camera = CameraSource(fuente=fuente, backend=backend)
    return camera.cap


def leer_frames(cap: cv2.VideoCapture, max_frames: int = 0) -> Iterator[np.ndarray]:
    leidos = 0
    while max_frames == 0 or leidos < max_frames:
        ok, frame = cap.read()
        if not ok or frame is None:
            return
        leidos += 1
        yield frame