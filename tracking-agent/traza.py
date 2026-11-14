"""
Trazabilidad de tiros (H-12 de la auditoría).

Escribe una línea JSON por cada tiro que el agente procesa, con su destino (enviado,
pendiente, rechazado, requiere revisión...), sus motivos, su confianza y los fotogramas que
lo delimitan. Permite auditar a posteriori por qué un tiro se envió o no, y localizarlo en el
vídeo grabado. No contiene tokens ni credenciales.
"""
from __future__ import annotations

import json
import threading
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from shot_analyzer import TiroObservado

ENVIADO = "ENVIADO"
PENDIENTE = "PENDIENTE"
REVISION = "REVISION"
CONFLICTO = "CONFLICTO"
RECHAZADO = "RECHAZADO"
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
