"""
Cliente REST de PoolHawk.

Convierte un :class:`~shot_analyzer.TiroObservado` en un ``TiroEventRequest`` y lo envía a
``POST /api/v1/matches/{id}/events``. El backend sigue siendo la autoridad sobre las reglas
y el turno: este cliente solo PROPONE eventos.

Garantías (auditoría del commit ``ec639cb``):

* H-02: el jugador se identifica por ``jugadorActualId`` (nunca por nombre visible). Si la
  API no lo devuelve, se lanza un error controlado.
* H-03: el evento lleva ``versionEsperada`` (= ``cantidadTiros`` observado al confirmar el
  tiro). Si el estado cambió, el backend responde 409 y el evento NO se reatribuye.
* H-04: el ``eventId`` nace en :class:`~shot_analyzer.ShotAnalyzer` y el evento completo se
  guarda en una :class:`~cola_eventos.ColaEventos` antes de enviarse, de modo que un
  reinicio o una respuesta perdida reenvían el MISMO evento.
* H-05: se valida la configuración del constructor.
* H-06/H-07: las respuestas se decodifican y validan; los fallos son ``ErrorApi``
  controlados y trazables (sin exponer el token).

Clasificación de errores:

* :class:`ErrorRed`: sin respuesta, timeout o 5xx tras los reintentos. Recuperable: el
  evento queda pendiente.
* :class:`ErrorConflicto` (409) y :class:`ErrorApi` con 4xx: el backend rechazó el evento;
  no se reintenta y pasa a la lista de rechazados.
* :class:`ErrorRespuesta`: la API respondió algo que el cliente no puede interpretar.
"""
from __future__ import annotations

import logging
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional
from urllib.parse import urlparse

import requests

from cola_eventos import ColaEventos
from shot_analyzer import TiroObservado

log = logging.getLogger("poolhawk.api")


class ErrorApi(Exception):
    """Error base del cliente. ``status`` es el código HTTP si lo hubo."""

    def __init__(self, mensaje: str, status: Optional[int] = None):
        super().__init__(mensaje)
        self.status = status


class ErrorRed(ErrorApi):
    """La API no respondió (conexión, timeout o 5xx) tras todos los reintentos."""


class ErrorConflicto(ErrorApi):
    """HTTP 409: el estado de la partida cambió respecto a ``versionEsperada``."""


class ErrorRespuesta(ErrorApi):
    """Respuesta exitosa pero con cuerpo o estructura que el cliente no puede interpretar."""


@dataclass(frozen=True)
class EstadoPartida:
    """Subconjunto validado de ``PartidaResponse`` que necesita el agente.

    Attributes:
        id: identificador de la partida.
        jugador_actual_id: id estable del jugador con el turno.
        version: ``cantidadTiros``; sirve como versión del estado.
        fase: fase de la partida (informativo).
    """

    id: str
    jugador_actual_id: str
    version: int
    fase: Optional[str]


def parsear_partida(datos: Any) -> EstadoPartida:
    """Valida el JSON de ``GET /matches/{id}`` y extrae lo que el agente necesita.

    Raises:
        ErrorRespuesta: si falta algún campo obligatorio o tiene un tipo inesperado.
    """
    if not isinstance(datos, dict):
        raise ErrorRespuesta("La partida recibida no es un objeto JSON")
    jugador_id = datos.get("jugadorActualId")
    if not isinstance(jugador_id, str) or not jugador_id.strip():
        raise ErrorRespuesta(
            "La API no devolvió 'jugadorActualId'; actualiza el backend (no se resuelve por nombre)")
    version = datos.get("cantidadTiros")
    if isinstance(version, bool) or not isinstance(version, int) or version < 0:
        raise ErrorRespuesta("La API no devolvió un 'cantidadTiros' válido")
    partida_id = datos.get("id")
    if not isinstance(partida_id, str) or not partida_id:
        raise ErrorRespuesta("La API no devolvió el 'id' de la partida")
    fase = datos.get("fase")
    return EstadoPartida(partida_id, jugador_id, version, fase if isinstance(fase, str) else None)


class PoolHawkApiClient:
    """Cliente mínimo, idempotente y tolerante a fallos para registrar tiros.

    Args:
        base_url: URL base del backend (``http://`` o ``https://``).
        token: JWT opcional; se envía como ``Authorization: Bearer`` y nunca se registra.
        max_reintentos: intentos totales por petición (>= 1).
        espera: segundos entre reintentos (>= 0).
        timeout: segundos máximos por petición (> 0).
        cola: cola de eventos pendientes; por defecto en memoria.
        sesion: sesión de ``requests`` (inyectable para pruebas).

    Raises:
        ValueError: si algún parámetro de configuración es inválido.
    """

    def __init__(self, base_url: str, token: Optional[str] = None, max_reintentos: int = 4,
                 espera: float = 1.0, timeout: float = 10.0, cola: Optional[ColaEventos] = None,
                 sesion: Optional[requests.Session] = None):
        if not isinstance(base_url, str) or not base_url.strip():
            raise ValueError("base_url es obligatoria")
        partes = urlparse(base_url)
        if partes.scheme not in ("http", "https") or not partes.netloc:
            raise ValueError("base_url debe ser una URL http:// o https:// válida")
        if isinstance(max_reintentos, bool) or not isinstance(max_reintentos, int) or max_reintentos < 1:
            raise ValueError("max_reintentos debe ser un entero >= 1")
        if espera < 0:
            raise ValueError("espera no puede ser negativa")
        if timeout <= 0:
            raise ValueError("timeout debe ser mayor que 0")
        self.base_url = base_url.rstrip("/")
        self.max_reintentos = max_reintentos
        self.espera = espera
        self.timeout = timeout
        self.cola = cola if cola is not None else ColaEventos()
        self._sesion = sesion or requests.Session()
        if token:
            self._sesion.headers["Authorization"] = f"Bearer {token}"

    # ------------------------------------------------------------------ HTTP
    def _pedir(self, metodo: str, ruta: str, **kwargs: Any) -> requests.Response:
        """Hace una petición con reintentos (solo red, timeout y 5xx) y valida el estado HTTP."""
        url = f"{self.base_url}{ruta}"
        ultimo: Optional[BaseException] = None
        for intento in range(1, self.max_reintentos + 1):
            try:
                r = self._sesion.request(metodo, url, timeout=self.timeout, **kwargs)
            except (requests.ConnectionError, requests.Timeout) as e:
                ultimo = e
            else:
                if r.status_code < 500:
                    return self._validar_estado(metodo, ruta, r)
                ultimo = requests.HTTPError(f"HTTP {r.status_code}")
            log.warning("%s %s falló (intento %d/%d): %s", metodo, ruta, intento,
                        self.max_reintentos, type(ultimo).__name__)
            if intento < self.max_reintentos:
                time.sleep(self.espera)
        raise ErrorRed(f"La API no responde tras {self.max_reintentos} intentos") from ultimo

    @staticmethod
    def _validar_estado(metodo: str, ruta: str, r: requests.Response) -> requests.Response:
        if 200 <= r.status_code < 300:
            return r
        detalle = (r.text or "")[:200]
        if r.status_code == 409:
            raise ErrorConflicto(f"Conflicto de estado en {metodo} {ruta}: {detalle}", 409)
        raise ErrorApi(f"La API rechazó {metodo} {ruta} ({r.status_code}): {detalle}", r.status_code)

    @staticmethod
    def _json(r: requests.Response) -> Any:
        """Decodifica el cuerpo como JSON, o lanza :class:`ErrorRespuesta` con contexto."""
        if r.status_code == 204 or not (r.text or "").strip():
            raise ErrorRespuesta(f"La API respondió {r.status_code} sin cuerpo JSON", r.status_code)
        try:
            return r.json()
        except ValueError as e:
            raise ErrorRespuesta("La API respondió un cuerpo que no es JSON válido", r.status_code) from e

    # ------------------------------------------------------------------ consultas
    def obtener_estado(self, match_id: str) -> EstadoPartida:
        """Consulta y valida el estado actual de la partida."""
        return parsear_partida(self._json(self._pedir("GET", f"/api/v1/matches/{match_id}")))

    @staticmethod
    def construir_payload(tiro: TiroObservado, jugador_id: str, version: int) -> Dict[str, Any]:
        """Arma el cuerpo de ``TiroEventRequest`` con el ``eventId`` del propio tiro."""
        return {
            "eventId": tiro.event_id,
            "jugadorId": jugador_id,
            "primeraBolaTocada": tiro.primera_bola_tocada,
            "bolasEmbolsadas": list(tiro.bolas_embolsadas),
            "troneras": list(tiro.troneras),
            "duracionSegundos": tiro.duracion_segundos,
            "versionEsperada": version,
        }

    # ------------------------------------------------------------------ registro
    def registrar_tiro(self, match_id: str, tiro: TiroObservado) -> Optional[Dict[str, Any]]:
        """Registra un tiro confirmado.

        El jugador y la versión se fijan UNA vez, al confirmar el tiro, y el evento completo
        se guarda en la cola antes de enviarse. Si ya estaba pendiente (reintento tras un
        reinicio) se reutiliza el mismo payload.

        Returns:
            El veredicto del backend, o ``None`` si el tiro requiere revisión y no se envió.

        Raises:
            ErrorRed: la API no respondió; el evento queda pendiente para reenviarse.
            ErrorConflicto / ErrorApi: el backend lo rechazó; pasa a rechazados.
            ErrorRespuesta: la API no devolvió un estado interpretable.
        """
        if tiro.requiere_revision:
            return None
        existente = next((i for i in self.cola.pendientes()
                          if i["payload"]["eventId"] == tiro.event_id), None)
        if existente is not None:
            return self._enviar(existente["matchId"], existente["payload"])
        estado = self.obtener_estado(match_id)
        payload = self.construir_payload(tiro, estado.jugador_actual_id, estado.version)
        self.cola.agregar(match_id, payload)
        return self._enviar(match_id, payload)

    def _enviar(self, match_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
        """Envía un evento ya persistido y actualiza la cola según el resultado."""
        event_id = payload["eventId"]
        try:
            r = self._pedir("POST", f"/api/v1/matches/{match_id}/events", json=payload)
        except ErrorRed:
            raise  # sigue pendiente: se reenviará con el mismo eventId
        except ErrorApi as e:
            self.cola.rechazar(event_id, str(e))
            raise
        # El 2xx confirma el registro; un cuerpo ilegible no justifica reenviar el tiro.
        self.cola.resolver(event_id)
        try:
            return self._json(r)
        except ErrorRespuesta:
            return {}

    def reenviar_pendientes(self) -> Dict[str, int]:
        """Reintenta los eventos guardados (p. ej. al arrancar el agente).

        Se detiene al primer :class:`ErrorRed` porque la API sigue caída.

        Returns:
            Conteo ``{"enviados": n, "rechazados": n, "pendientes": n}``.
        """
        enviados = rechazados = 0
        for item in self.cola.pendientes():
            try:
                self._enviar(item["matchId"], item["payload"])
                enviados += 1
            except ErrorRed:
                break
            except ErrorApi:
                rechazados += 1
        return {"enviados": enviados, "rechazados": rechazados,
                "pendientes": len(self.cola.pendientes())}
