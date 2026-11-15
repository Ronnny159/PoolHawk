"""
Trazabilidad de tiros (H-12 de la auditoría).

Escribe una línea JSON por cada TRANSICIÓN de estado de un tiro (H-07): ``OBSERVADO`` →
``PENDIENTE`` → ``REINTENTO`` → ``ENVIADO`` (o ``RECHAZADO``, ``CONFLICTO``, ``BLOQUEADO``...).
Todas las líneas de un tiro comparten el mismo ``eventId``, de modo que el estado vigente es el
de la última línea (ver :func:`estado_actual`); un evento que el reintentador confirma después
queda registrado como ``ENVIADO`` y no se queda en ``PENDIENTE``. Cada línea lleva su motivo, y
las de observación además la confianza y los fotogramas que delimitan el tiro. Permite auditar a posteriori por qué un tiro se envió o no, y localizarlo en el
vídeo grabado. No contiene tokens ni credenciales.
"""
from __future__ import annotations

import json
import threading
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from shot_analyzer import TiroObservado

OBSERVADO = "OBSERVADO"
ENVIADO = "ENVIADO"
PENDIENTE = "PENDIENTE"
REINTENTO = "REINTENTO"
REVISION = "REVISION"
CONFLICTO = "CONFLICTO"
RECHAZADO = "RECHAZADO"
BLOQUEADO = "BLOQUEADO"
ESTADO_NO_DISPONIBLE = "ESTADO_NO_DISPONIBLE"
SIN_API = "SIN_API"


class TrazaTiros:
    """Registro en formato JSON Lines (un objeto por línea, solo se añade al final).

    Args:
        ruta: archivo de destino; ``None`` desactiva el registro.
    """

    def __init__(self, ruta: Optional[str]):
        self.ruta = ruta
        self._lock = threading.Lock()

    def registrar(self, tiro: TiroObservado, estado: str, match_id: Optional[str] = None,
                  detalle: Optional[str] = None) -> Dict[str, Any]:
        """Anota un tiro y devuelve la entrada escrita."""
        entrada = {
            "ts": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "estado": estado,
            "eventId": tiro.event_id,
            "matchId": match_id,
            "primeraBolaTocada": tiro.primera_bola_tocada,
            "bolasEmbolsadas": list(tiro.bolas_embolsadas),
            "troneras": list(tiro.troneras),
            "duracionSegundos": tiro.duracion_segundos,
            "confianza": tiro.confianza,
            "frameInicio": tiro.frame_inicio,
            "frameFin": tiro.frame_fin,
            "motivos": list(tiro.motivos),
            "detalle": detalle,
        }
        if self.ruta:
            with self._lock, open(self.ruta, "a", encoding="utf-8") as f:
                f.write(json.dumps(entrada, ensure_ascii=False) + "\n")
        return entrada

    def transicion(self, event_id: str, estado: str, match_id: Optional[str] = None,
                   detalle: Optional[str] = None) -> Dict[str, Any]:
        """Anota un cambio de estado de un evento ya conocido (p. ej. confirmado por el reintentador)."""
        entrada = {
            "ts": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "estado": estado, "eventId": event_id, "matchId": match_id, "detalle": detalle,
        }
        if self.ruta:
            with self._lock, open(self.ruta, "a", encoding="utf-8") as f:
                f.write(json.dumps(entrada, ensure_ascii=False) + "\n")
        return entrada


def estado_actual(ruta: str) -> Dict[str, str]:
    """Reconstruye el estado vigente de cada ``eventId`` leyendo la traza (la última línea gana)."""
    estados: Dict[str, str] = {}
    with open(ruta, encoding="utf-8") as f:
        for linea in f:
            if linea.strip():
                e = json.loads(linea)
                estados[e["eventId"]] = e["estado"]
    return estados