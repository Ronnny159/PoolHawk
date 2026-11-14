"""
Cola local de eventos pendientes (H-04 de la auditoría del cliente REST).

Guarda en disco cada tiro confirmado ANTES de enviarlo, con su ``eventId`` y su payload ya
completo (jugador y versión incluidos). Si el agente se reinicia o se pierde la respuesta,
el mismo evento se reenvía con la misma identidad y el backend (idempotente) no lo duplica.

El archivo es JSON y se reescribe de forma atómica (archivo temporal + ``os.replace``): una
escritura interrumpida deja intacta la versión anterior. Es seguro entre hilos (H-11), porque
el agente envía tiros desde el hilo principal mientras otro hilo reintenta los pendientes.
No contiene tokens ni credenciales.
"""
from __future__ import annotations

import json
import os
import tempfile
import threading
from typing import Any, Dict, List, Optional


class ColaEventos:
    """Persistencia mínima de eventos pendientes y rechazados.

    Args:
        ruta: archivo JSON donde se guarda el estado. ``None`` mantiene la cola solo en
            memoria (útil en pruebas o si no se necesita recuperación tras reinicio).
    """

    def __init__(self, ruta: Optional[str] = None):
        self.ruta = ruta
        self._lock = threading.RLock()
        self._pendientes: Dict[str, Dict[str, Any]] = {}
        self._rechazados: List[Dict[str, Any]] = []
        self._cargar()

    # ------------------------------------------------------------------ consulta
    def pendientes(self) -> List[Dict[str, Any]]:
        """Copia de los eventos aún no confirmados por el backend, en orden de llegada."""
        with self._lock:
            return list(self._pendientes.values())

    def contiene(self, event_id: str) -> bool:
        """Indica si el evento sigue pendiente."""
        with self._lock:
            return event_id in self._pendientes

    def rechazados(self) -> List[Dict[str, Any]]:
        """Eventos descartados definitivamente (rechazo 4xx o reintentos agotados), con su motivo."""
        with self._lock:
            return list(self._rechazados)

    # ------------------------------------------------------------------ mutación
    def agregar(self, match_id: str, payload: Dict[str, Any]) -> None:
        """Registra un evento pendiente. Si el ``eventId`` ya existe, no lo duplica."""
        event_id = payload["eventId"]
        with self._lock:
            if event_id not in self._pendientes:
                self._pendientes[event_id] = {"matchId": match_id, "payload": payload, "intentos": 0}
                self._guardar()

    def resolver(self, event_id: str) -> None:
        """El backend confirmó el evento: se elimina de la cola."""
        with self._lock:
            if self._pendientes.pop(event_id, None) is not None:
                self._guardar()

    def registrar_intento(self, event_id: str) -> int:
        """Cuenta un intento de envío fallido y devuelve el total acumulado (0 si no existe)."""
        with self._lock:
            item = self._pendientes.get(event_id)
            if item is None:
                return 0
            item["intentos"] = int(item.get("intentos", 0)) + 1
            self._guardar()
            return item["intentos"]

    def rechazar(self, event_id: str, motivo: str) -> None:
        """El backend rechazó el evento: sale de pendientes para no reintentarse en bucle."""
        with self._lock:
            item = self._pendientes.pop(event_id, None)
            if item is not None:
                item["motivo"] = motivo
                self._rechazados.append(item)
                self._guardar()

    # ------------------------------------------------------------------ disco
    def _cargar(self) -> None:
        if not self.ruta or not os.path.exists(self.ruta):
            return
        try:
            with open(self.ruta, encoding="utf-8") as f:
                datos = json.load(f)
            self._pendientes = {i["payload"]["eventId"]: i for i in datos.get("pendientes", [])}
            self._rechazados = list(datos.get("rechazados", []))
        except (OSError, ValueError, KeyError, TypeError) as e:
            # Un archivo corrupto no debe impedir arrancar; se conserva una copia para revisar.
            os.replace(self.ruta, self.ruta + ".corrupto")
            raise ValueError(f"Cola de eventos ilegible; se movió a {self.ruta}.corrupto") from e

    def _guardar(self) -> None:
        if not self.ruta:
            return
        datos = {"pendientes": self.pendientes(), "rechazados": self._rechazados}
        carpeta = os.path.dirname(os.path.abspath(self.ruta))
        fd, tmp = tempfile.mkstemp(dir=carpeta, suffix=".tmp")
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as f:
                json.dump(datos, f, ensure_ascii=False, indent=2)
            os.replace(tmp, self.ruta)
        except OSError:
            if os.path.exists(tmp):
                os.remove(tmp)
            raise
