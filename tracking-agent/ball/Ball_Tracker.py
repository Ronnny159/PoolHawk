"""
Seguimiento temporal de bolas (Fase 4 de la auditoría, H-05).

Mantiene una identidad persistente por bola entre fotogramas. Asocia detecciones a pistas
existentes por proximidad (con predicción de movimiento), estima el número de cada bola
por VOTACIÓN de observaciones confiables (una clasificación errónea aislada no cambia la
identidad) y distingue entre bolas quietas, en movimiento y desaparecidas.

Una bola desaparecida más de ``max_perdidas`` fotogramas se reporta en
``ultimas_perdidas`` con su última posición conocida. Eso es solo EVIDENCIA para el
``ShotAnalyzer`` (Fase 5): el tracker no decide que una bola fue embolsada.
"""
from __future__ import annotations

from collections import Counter, deque
from dataclasses import dataclass, field
from typing import Deque, Dict, List, Optional, Tuple

from ball_classifier import Observacion
from ball_detector import Deteccion

MIN_VOTOS = 3          # observaciones confiables mínimas para fijar un número
MIN_PROPORCION = 0.5   # el número ganador debe tener al menos esta proporción de votos


@dataclass
class Track:
    """Bola seguida a lo largo del tiempo."""

    id: int
    x: float
    y: float
    r: float
    vx: float = 0.0
    vy: float = 0.0
    edad: int = 1
    frames_perdida: int = 0
    frames_quieta: int = 0
    en_movimiento: bool = False
    votos: Counter = field(default_factory=Counter)
    trayectoria: Deque[Tuple[float, float]] = field(default_factory=lambda: deque(maxlen=60))

    @property
    def numero(self) -> Optional[int]:
        """Número estable de la bola, o ``None`` si aún no hay evidencia suficiente."""
        total = sum(self.votos.values())
        if total < MIN_VOTOS:
            return None
        numero, cuantos = self.votos.most_common(1)[0]
        return numero if cuantos / total >= MIN_PROPORCION else None

    @property
    def visible(self) -> bool:
        """Verdadero si fue detectada en el último fotograma."""
        return self.frames_perdida == 0


class BallTracker:
    """Asocia detecciones entre fotogramas y mantiene identidades persistentes.

    Args:
        max_distancia: distancia máxima (px) entre la posición predicha y la detección.
        max_perdidas: fotogramas consecutivos sin ver una bola antes de darla por perdida.
        umbral_movimiento: velocidad (px/fotograma) a partir de la cual está en movimiento.
        suavizado: peso de la velocidad nueva en el promedio móvil (0-1).
    """

    def __init__(
        self,
        max_distancia: float = 40.0,
        max_perdidas: int = 15,
        umbral_movimiento: float = 1.5,
        suavizado: float = 0.5,
    ):
        self.max_distancia = max_distancia
        self.max_perdidas = max_perdidas
        self.umbral_movimiento = umbral_movimiento
        self.suavizado = suavizado
        self._tracks: Dict[int, Track] = {}
        self._siguiente_id = 1
        self.ultimas_perdidas: List[Track] = []

    @property
    def tracks(self) -> List[Track]:
        """Pistas activas (visibles u ocultas temporalmente)."""
        return list(self._tracks.values())

    @property
    def hay_pendientes(self) -> bool:
        """Verdadero si hay bolas ocultas que aún pueden reaparecer o darse por perdidas."""
        return any(t.frames_perdida > 0 for t in self._tracks.values())

    def todas_quietas(self, frames: int = 10) -> bool:
        """Verdadero si todas las bolas visibles llevan ``frames`` fotogramas sin moverse."""
        visibles = [t for t in self._tracks.values() if t.visible]
        return bool(visibles) and all(t.frames_quieta >= frames for t in visibles)

    def actualizar(
        self, entradas: List[Tuple[Deteccion, Optional[Observacion]]]
    ) -> List[Track]:
        """Procesa un fotograma.

        Args:
            entradas: pares ``(deteccion, observacion)``; la observación puede ser ``None``.

        Returns:
            Las pistas visibles en este fotograma.
        """
        self.ultimas_perdidas = []

        candidatos = []
        for ti, t in self._tracks.items():
            px, py = t.x + t.vx, t.y + t.vy
            for di, (det, _) in enumerate(entradas):
                d = ((det.x - px) ** 2 + (det.y - py) ** 2) ** 0.5
                if d <= self.max_distancia:
                    candidatos.append((d, ti, di))
        candidatos.sort()

        pistas_usadas, dets_usadas = set(), set()
        for _, ti, di in candidatos:
            if ti in pistas_usadas or di in dets_usadas:
                continue
            pistas_usadas.add(ti)
            dets_usadas.add(di)
            self._actualizar_pista(self._tracks[ti], *entradas[di])

        nuevas = set()
        for di, (det, obs) in enumerate(entradas):
            if di not in dets_usadas:
                nuevas.add(self._crear_pista(det, obs))

        for ti in list(self._tracks):
            if ti in pistas_usadas or ti in nuevas:
                continue
            t = self._tracks[ti]
            t.frames_perdida += 1
            t.en_movimiento = False
            if t.frames_perdida > self.max_perdidas:
                self.ultimas_perdidas.append(t)
                del self._tracks[ti]

        return [t for t in self._tracks.values() if t.visible]

    # ------------------------------------------------------------------ internos
    def _crear_pista(self, det: Deteccion, obs: Optional[Observacion]) -> int:
        t = Track(id=self._siguiente_id, x=det.x, y=det.y, r=det.r)
        t.trayectoria.append((det.x, det.y))
        self._registrar_voto(t, obs)
        self._tracks[t.id] = t
        self._siguiente_id += 1
        return t.id

    def _actualizar_pista(
        self, t: Track, det: Deteccion, obs: Optional[Observacion]
    ) -> None:
        nvx, nvy = det.x - t.x, det.y - t.y
        a = self.suavizado
        t.vx = a * nvx + (1 - a) * t.vx
        t.vy = a * nvy + (1 - a) * t.vy
        t.x, t.y, t.r = float(det.x), float(det.y), float(det.r)
        t.edad += 1
        t.frames_perdida = 0
        velocidad = (t.vx ** 2 + t.vy ** 2) ** 0.5
        t.en_movimiento = velocidad > self.umbral_movimiento
        t.frames_quieta = 0 if t.en_movimiento else t.frames_quieta + 1
        t.trayectoria.append((t.x, t.y))
        self._registrar_voto(t, obs)

    @staticmethod
    def _registrar_voto(t: Track, obs: Optional[Observacion]) -> None:
        if obs is not None and obs.es_confiable and obs.numero is not None:
            t.votos[obs.numero] += 1