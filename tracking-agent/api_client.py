"""
Cliente REST de PoolHawk.
Convierte un :class:`~shot_analyzer.TiroObservado` en un `TiroEventRequest` y lo envía a
`POST /api/v1/matches/{id}/events`. El backend sigue siendo la autoridad sobre las reglas
y el turno: este cliente solo PROPONE eventos.
"""
from __future__ import annotations
import logging
import threading
import time
from dataclasses import dataclass
from typing import Any, Callable, Dict, Optional
from urllib.parse import urlparse
import requests
from cola_eventos import ColaEventos
import traza as _traza
from shot_analyzer import TiroObservado

log = logging.getLogger("poolhawk.api")

class ErrorApi(Exception):
    """Error base del cliente. `status` es el código HTTP si lo hubo."""
    def __init__(self, mensaje: str, status: Optional[int] = None):
        super().__init__(mensaje)
        self.status = status

class ErrorRed(ErrorApi):
    """La API no respondió (conexión, timeout o 5xx) tras todos los reintentos."""

class ErrorConflicto(ErrorApi):
    """HTTP 409: el estado de la partida cambió respecto a `versionEsperada`."""

class ErrorEstadoNoDisponible(ErrorApi):
    """No se pudo consultar la partida: el tiro NO se encoló (no hay jugador ni versión fiables)."""

class ErrorColaBloqueada(ErrorApi):
    """La cola está bloqueada por un evento que agotó sus reintentos; requiere intervención."""

class ErrorRespuesta(ErrorApi):
    """Respuesta exitosa pero con cuerpo o estructura que el cliente no puede interpretar."""

@dataclass(frozen=True)
class EstadoPartida:
    """Subconjunto validado de `PartidaResponse` que necesita el agente."""
    id: str
    jugador_actual_id: str
    version: int
    fase: Optional[str]

def parsear_partida(datos: Any) -> EstadoPartida:
    """Valida el JSON de `GET /matches/{id}` y extrae lo que el agente necesita."""
    if not isinstance(datos, dict):
        raise ErrorRespuesta("La partida recibida no es un objeto JSON")
    
    jugador_actual = datos.get("jugadorActualId") or datos.get("jugadorActual")
    if isinstance(jugador_actual, dict):
        jugador_id = jugador_actual.get("id")
    elif isinstance(jugador_actual, str):
        jugador_id = jugador_actual
    else:
        jugador_id = None
    if not isinstance(jugador_id, str) or not jugador_id.strip():
        raise ErrorRespuesta("La API no devolvió 'jugadorActualId'; actualiza el backend")
    version = datos.get("cantidadTiros")
    if isinstance(version, bool) or not isinstance(version, int) or version < 0:
        raise ErrorRespuesta("La API no devolvió un 'cantidadTiros' válido")
    
    partida_id = datos.get("id")
    if not isinstance(partida_id, str) or not partida_id:
        raise ErrorRespuesta("La API no devolvió el 'id' de la partida")
    
    fase = datos.get("fase")
    return EstadoPartida(partida_id, jugador_id, version, fase if isinstance(fase, str) else None)

class PoolHawkApiClient:
    """Cliente mínimo, idempotente y tolerante a fallos para registrar tiros."""
    def __init__(self, base_url: str, token: Optional[str] = None, max_reintentos: int = 4,
                 espera: float = 1.0, timeout: float = 10.0, max_intentos_evento: int = 10,
                 cola: Optional[ColaEventos] = None, sesion: Optional[requests.Session] = None):
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
        if isinstance(max_intentos_evento, bool) or not isinstance(max_intentos_evento, int) or max_intentos_evento < 1:
            raise ValueError("max_intentos_evento debe ser un entero >= 1")
        
        self.max_intentos_evento = max_intentos_evento
        self._lock = threading.RLock()
        self.base_url = base_url.rstrip("/")
        self.max_reintentos = max_reintentos
        self.espera = espera
        self.timeout = timeout
        self.cola = cola if cola is not None else ColaEventos()
        self._sesion = sesion or requests.Session()
        if token:
            self._sesion.headers["Authorization"] = f"Bearer {token}"

    def _pedir(self, metodo: str, ruta: str, reintentos: Optional[int] = None, **kwargs: Any) -> requests.Response:
        total = reintentos or self.max_reintentos
        url = f"{self.base_url}{ruta}"
        ultimo: Optional[BaseException] = None
        for intento in range(1, total + 1):
            try:
                r = self._sesion.request(metodo, url, timeout=self.timeout, **kwargs)
            except (requests.ConnectionError, requests.Timeout) as e:
                ultimo = e
            else:
                if r.status_code < 500:
                    return self._validar_estado(metodo, ruta, r)
                ultimo = requests.HTTPError(f"HTTP {r.status_code}")
            log.warning("%s %s falló (intento %d/%d): %s", metodo, ruta, intento, total, type(ultimo).__name__)
            if intento < total:
                time.sleep(self.espera)
        raise ErrorRed(f"La API no responde tras {total} intentos") from ultimo

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
        if r.status_code == 204 or not (r.text or "").strip():
            raise ErrorRespuesta(f"La API respondió {r.status_code} sin cuerpo JSON", r.status_code)
        try:
            return r.json()
        except ValueError as e:
            raise ErrorRespuesta("La API respondió un cuerpo que no es JSON válido", r.status_code) from e

    def obtener_estado(self, match_id: str) -> EstadoPartida:
        return parsear_partida(self._json(self._pedir("GET", f"/api/v1/matches/{match_id}")))

    @staticmethod
    def construir_payload(tiro: TiroObservado, jugador_id: str, version: int) -> Dict[str, Any]:
        return {
            "eventId": tiro.event_id,
            "jugadorId": jugador_id,
            "primeraBolaTocada": tiro.primera_bola_tocada,
            "bolasEmbolsadas": list(tiro.bolas_embolsadas),
            "troneras": list(tiro.troneras),
            "duracionSegundos": tiro.duracion_segundos,
            "versionEsperada": version,
        }

    def registrar_tiro(self, match_id: str, tiro: TiroObservado) -> Optional[Dict[str, Any]]:
        if tiro.requiere_revision:
            return None
        
        bloqueo = self.cola.bloqueo
        if bloqueo is not None:
            raise ErrorColaBloqueada(
                f"Cola bloqueada por el evento {bloqueo['eventId']} ({bloqueo['motivo']}). Reconcilia "
                "con el backend y reanuda (--reanudar-cola) o descarta ese evento (--descartar-evento)")
        
        # H-03 CORREGIDO: Bloquear solo si hay un evento pendiente con un eventId DIFERENTE.
        # Si el eventId es el mismo, es un reintento legítimo del mismo evento y debe permitirse.
        pendientes_mismo_match = [i for i in self.cola.pendientes() if i["matchId"] == match_id]
        if pendientes_mismo_match:
            for pendiente in pendientes_mismo_match:
                if pendiente["payload"]["eventId"] != tiro.event_id:
                    raise ErrorColaBloqueada(
                        f"Hay un tiro pendiente de confirmación para la partida {match_id} con otro eventId. "
                        "Espere a que se resuelva antes de inferir nuevos eventos."
                    )

        existente = next((i for i in self.cola.pendientes()
                          if i["payload"]["eventId"] == tiro.event_id), None)
        if existente is not None:
            return self._enviar(existente["matchId"], existente["payload"])
        
        try:
            estado = self.obtener_estado(match_id)
        except ErrorApi as e:
            raise ErrorEstadoNoDisponible(
                f"No se pudo consultar la partida ({e}); el tiro no se encoló y requiere revisión manual", e.status) from e
        
        payload = self.construir_payload(tiro, estado.jugador_actual_id, estado.version)
        self.cola.agregar(match_id, payload)
        return self._enviar(match_id, payload)

    def _enviar(self, match_id: str, payload: Dict[str, Any], reintentos: Optional[int] = None) -> Dict[str, Any]:
        event_id = payload["eventId"]
        with self._lock:
            if not self.cola.contiene(event_id):
                return {}
            try:
                r = self._pedir("POST", f"/api/v1/matches/{match_id}/events", reintentos=reintentos, json=payload)
            except ErrorRed:
                raise
            except ErrorApi as e:
                self.cola.rechazar(event_id, str(e))
                raise
            self.cola.resolver(event_id)
        try:
            return self._json(r)
        except ErrorRespuesta:
            return {}

    def reenviar_pendientes(self, reintentos: Optional[int] = None,
                            observador: Optional[Callable[[str, str, Optional[str]], None]] = None) -> Dict[str, int]:
        notificar = observador or (lambda *_: None)
        enviados = rechazados = bloqueada = 0
        if self.cola.bloqueo is not None:
            return {"enviados": 0, "rechazados": 0, "bloqueada": 1, "pendientes": len(self.cola.pendientes())}
        
        for item in self.cola.pendientes():
            event_id = item["payload"]["eventId"]
            try:
                self._enviar(item["matchId"], item["payload"], reintentos)
                enviados += 1
                notificar(event_id, _traza.ENVIADO, "confirmado en un reintento")
            except ErrorRed as e:
                intentos = self.cola.registrar_intento(event_id)
                notificar(event_id, _traza.REINTENTO, f"intento {intentos}/{self.max_intentos_evento}: {e}")
                if intentos >= self.max_intentos_evento:
                    motivo = f"reintentos agotados tras {intentos} fallos de red"
                    self.cola.bloquear(event_id, motivo)
                    log.error("Cola bloqueada por el evento %s: %s", event_id, motivo)
                    notificar(event_id, _traza.BLOQUEADO, motivo)
                    bloqueada = 1
                break
            except ErrorConflicto as e:
                rechazados += 1
                notificar(event_id, _traza.CONFLICTO, str(e))
            except ErrorApi as e:
                rechazados += 1
                notificar(event_id, _traza.RECHAZADO, str(e))
        return {"enviados": enviados, "rechazados": rechazados, "bloqueada": bloqueada, "pendientes": len(self.cola.pendientes())}

class ReintentadorPendientes:
    def __init__(self, cliente: PoolHawkApiClient, base: float = 2.0, maximo: float = 60.0,
                 reloj=time.monotonic, sondeo: float = 0.5, observador=None):
        if base <= 0 or maximo < base or sondeo <= 0:
            raise ValueError("Se requiere base > 0, maximo >= base y sondeo > 0")
        self._cliente, self._base, self._maximo = cliente, base, maximo
        self._reloj, self._sondeo, self._observador = reloj, sondeo, observador
        self._espera, self._proximo = base, 0.0
        self._detener = threading.Event()
        self._hilo: Optional[threading.Thread] = None

    @property
    def espera_actual(self) -> float:
        return self._espera

    def intentar(self) -> Optional[Dict[str, int]]:
        ahora = self._reloj()
        if ahora < self._proximo or self._cliente.cola.bloqueo is not None:
            return None
        if not self._cliente.cola.pendientes():
            self._espera = self._base
            return None
        resumen = self._cliente.reenviar_pendientes(reintentos=1, observador=self._observador)
        self._espera = min(self._espera * 2, self._maximo) if resumen["pendientes"] else self._base
        self._proximo = ahora + self._espera
        return resumen

    def iniciar(self) -> None:
        if self._hilo is not None and self._hilo.is_alive():
            return
        self._detener.clear()
        self._hilo = threading.Thread(target=self._bucle, name="poolhawk-reintentos", daemon=True)
        self._hilo.start()

    def detener(self, timeout: Optional[float] = None, drenar: bool = True) -> Dict[str, Any]:
        limite = timeout if timeout is not None else self._cliente.timeout + 5.0
        self._detener.set()
        terminado = True
        if self._hilo is not None:
            self._hilo.join(limite)
            terminado = not self._hilo.is_alive()
        if terminado and drenar and self._cliente.cola.bloqueo is None:
            self._cliente.reenviar_pendientes(reintentos=1, observador=self._observador)
        return {"terminado": terminado, "pendientes": len(self._cliente.cola.pendientes())}

    def _bucle(self) -> None:
        while not self._detener.wait(self._sondeo):
            try:
                self.intentar()
            except Exception:
                log.exception("Error inesperado reintentando eventos pendientes")
