"""
Cliente REST de PoolHawk (Fase 6 de la auditoría).

Convierte un :class:`~shot_analyzer.TiroObservado` en un ``TiroEventRequest`` y lo envía a
``POST /api/v1/matches/{id}/events``. El backend sigue siendo la autoridad de las reglas:
este cliente solo propone eventos.

Garantías:

* El ``eventId`` se genera UNA vez por tiro y se reutiliza en todos los reintentos, de modo
  que el backend (idempotente) nunca registra el mismo tiro dos veces.
* Se reintenta solo ante fallos de red, timeouts o 5xx. Un 4xx no se reintenta: indica un
  payload inválido o un ``eventId`` repetido con contenido distinto.
* Los tiros con ``requiere_revision`` NO se envían: una observación dudosa no debe alterar
  el marcador.
"""
from __future__ import annotations

import time
import uuid
from typing import Any, Dict, Optional

import requests

from shot_analyzer import TiroObservado


class ErrorApi(Exception):
    """La API rechazó la petición (4xx) o no respondió tras los reintentos."""


class PoolHawkApiClient:
    """Cliente mínimo para consultar partidas y registrar tiros.

    Args:
        base_url: URL base del backend, p. ej. ``http://localhost:8080``.
        token: JWT opcional; si se da se envía como ``Authorization: Bearer``.
        max_reintentos: intentos totales por petición.
        espera: segundos entre reintentos.
        sesion: sesión de ``requests`` (inyectable para pruebas).
    """

    def __init__(self, base_url: str, token: Optional[str] = None, max_reintentos: int = 4,
                 espera: float = 1.0, sesion: Optional[requests.Session] = None):
        self.base_url = base_url.rstrip("/")
        self.max_reintentos = max_reintentos
        self.espera = espera
        self._sesion = sesion or requests.Session()
        if token:
            self._sesion.headers["Authorization"] = f"Bearer {token}"

    def _pedir(self, metodo: str, ruta: str, **kwargs: Any) -> requests.Response:
        url = f"{self.base_url}{ruta}"
        for intento in range(1, self.max_reintentos + 1):
            try:
                r = self._sesion.request(metodo, url, timeout=10, **kwargs)
                if r.status_code >= 500:
                    raise requests.HTTPError(f"error {r.status_code}", response=r)
            except (requests.ConnectionError, requests.Timeout, requests.HTTPError) as e:
                if intento == self.max_reintentos:
                    raise ErrorApi(f"La API no responde tras {intento} intentos") from e
                time.sleep(self.espera)
                continue
            if 400 <= r.status_code < 500:
                raise ErrorApi(f"La API rechazó la petición ({r.status_code}): {r.text[:200]}")
            return r
        raise ErrorApi("Sin respuesta de la API")  # inalcanzable, por completitud

    def obtener_partida(self, match_id: str) -> Dict[str, Any]:
        """Devuelve el estado actual de la partida (``PartidaResponse``)."""
        return self._pedir("GET", f"/api/v1/matches/{match_id}").json()

    def jugador_actual_id(self, match_id: str) -> str:
        """Resuelve el id del jugador con el turno (la API expone solo su nombre)."""
        partida = self.obtener_partida(match_id)
        nombre = partida.get("jugadorActual")
        for j in partida.get("equipoA", []) + partida.get("equipoB", []):
            if j["nombre"] == nombre:
                return j["id"]
        raise ErrorApi(f"No se encontró al jugador actual '{nombre}' en la partida")

    @staticmethod
    def construir_payload(tiro: TiroObservado, jugador_id: str, event_id: str) -> Dict[str, Any]:
        """Arma el cuerpo de ``TiroEventRequest`` a partir de un tiro observado."""
        return {
            "eventId": event_id,
            "jugadorId": jugador_id,
            "primeraBolaTocada": tiro.primera_bola_tocada,
            "bolasEmbolsadas": list(tiro.bolas_embolsadas),
            "troneras": list(tiro.troneras),
            "duracionSegundos": tiro.duracion_segundos,
        }

    def registrar_tiro(self, match_id: str, tiro: TiroObservado,
                       event_id: Optional[str] = None) -> Optional[Dict[str, Any]]:
        """Envía un tiro confirmado al backend.

        Returns:
            El JSON de respuesta, o ``None`` si el tiro requiere revisión y no se envió.
        """
        if tiro.requiere_revision:
            return None
        event_id = event_id or str(uuid.uuid4())  # fijo para todos los reintentos
        jugador_id = self.jugador_actual_id(match_id)
        payload = self.construir_payload(tiro, jugador_id, event_id)
        return self._pedir("POST", f"/api/v1/matches/{match_id}/events", json=payload).json()