"""
Inferencia de tiros a partir del seguimiento (Fase 5 de la auditoría).

El analizador NO es dueño de las reglas del billar: solo convierte evidencia visual en una
:class:`TiroObservado` que el backend validará. Distingue lo observado de lo dudoso: si
una bola desaparece lejos de cualquier tronera, o sin número identificado, el tiro queda
marcado con ``requiere_revision`` y los motivos, en vez de inventar un embolsado.

Un tiro empieza cuando alguna bola se mueve y termina cuando todas las visibles llevan
``frames_quietud`` fotogramas quietas y no quedan bolas ocultas pendientes de resolver.

La primera bola tocada es una HEURÍSTICA: la primera bola de objeto que empieza a moverse
después de que la blanca (número 0) se mueve. Con una cámara cenital y pocos fps puede
fallar, por eso nunca se marca como confirmada.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

from ball_tracker import BallTracker, Track

Punto = Tuple[float, float]


@dataclass(frozen=True)
class TiroObservado:
    """Tiro inferido visualmente. Campos alineados con ``TiroEventRequest`` del backend.

    ``event_id`` nace aquí, cuando el analizador confirma el tiro, y acompaña a ese tiro
    durante todo su ciclo de vida (reintentos y reinicios del agente incluidos). El cliente
    REST solo añade ``jugadorId`` y ``versionEsperada``."""

    primera_bola_tocada: Optional[int]
    bolas_embolsadas: List[int]
    troneras: List[str]
    duracion_segundos: int
    requiere_revision: bool
    motivos: List[str] = field(default_factory=list)
    event_id: str = field(default_factory=lambda: str(uuid.uuid4()))


class ShotAnalyzer:
    """Detecta el inicio y el fin de un tiro y resume lo ocurrido.

    Args:
        troneras: nombre -> posición (px) de cada tronera. Debe calibrarse con la mesa
            real; sin troneras no se puede inferir ningún embolsado.
        radio_tronera: distancia máxima (px) para asignar una bola perdida a una tronera.
        frames_quietud: fotogramas de quietud para dar el tiro por terminado.
        fps: fotogramas por segundo de la fuente (para la duración).
    """

    def __init__(
        self,
        troneras: Optional[Dict[str, Punto]] = None,
        radio_tronera: float = 60.0,
        frames_quietud: int = 10,
        fps: float = 15.0,
    ):
        self.troneras = troneras or {}
        self.radio_tronera = radio_tronera
        self.frames_quietud = frames_quietud
        self.fps = fps
        self._frame = 0
        self._reiniciar()

    @property
    def en_tiro(self) -> bool:
        """Verdadero mientras hay un tiro en curso."""
        return self._inicio is not None

    def actualizar(self, tracker: BallTracker) -> Optional[TiroObservado]:
        """Procesa el estado del tracker tras un fotograma.

        Returns:
            Un :class:`TiroObservado` cuando el tiro termina; ``None`` en cualquier otro caso.
        """
        self._frame += 1
        visibles = [t for t in tracker.tracks if t.visible]

        if self._inicio is None:
            if any(t.en_movimiento for t in visibles):
                self._inicio = self._frame
            else:
                return None

        self._observar_movimiento(visibles)
        self._registrar_perdidas(tracker.ultimas_perdidas)

        if tracker.todas_quietas(self.frames_quietud) and not tracker.hay_pendientes:
            tiro = self._cerrar()
            self._reiniciar()
            return tiro
        return None

    # ------------------------------------------------------------------ internos
    def _reiniciar(self) -> None:
        self._inicio: Optional[int] = None
        self._blanca_se_movio = False
        self._primera: Optional[int] = None
        self._primera_sin_numero = False
        self._embolsadas: List[Tuple[int, str]] = []
        self._motivos: List[str] = []

    def _observar_movimiento(self, visibles: List[Track]) -> None:
        if not self._blanca_se_movio:
            self._blanca_se_movio = any(t.numero == 0 and t.en_movimiento for t in visibles)
            return
        if self._primera is None and not self._primera_sin_numero:
            for t in visibles:
                if t.en_movimiento and t.numero != 0:
                    if t.numero is None:
                        self._primera_sin_numero = True
                    else:
                        self._primera = t.numero
                    break

    def _tronera_cercana(self, t: Track) -> Optional[str]:
        mejor, dist_mejor = None, self.radio_tronera
        for nombre, (px, py) in self.troneras.items():
            d = ((t.x - px) ** 2 + (t.y - py) ** 2) ** 0.5
            if d <= dist_mejor:
                mejor, dist_mejor = nombre, d
        return mejor

    def _registrar_perdidas(self, perdidas: List[Track]) -> None:
        for t in perdidas:
            tronera = self._tronera_cercana(t)
            if t.numero is None:
                self._motivos.append(f"Bola id{t.id} desapareció sin número identificado")
            elif tronera is None:
                self._motivos.append(
                    f"Bola #{t.numero} desapareció lejos de toda tronera (posible oclusión)")
            else:
                self._embolsadas.append((t.numero, tronera))

    def _cerrar(self) -> TiroObservado:
        motivos = list(self._motivos)
        if not self.troneras:
            motivos.append("Sin troneras calibradas: no se pueden inferir embolsados")
        if self._primera_sin_numero:
            motivos.append("La primera bola tocada no tiene número identificado")
        if not self._blanca_se_movio:
            motivos.append("No se observó movimiento de la bola blanca")
        duracion = max(1, round((self._frame - (self._inicio or self._frame)) / self.fps))
        return TiroObservado(
            primera_bola_tocada=self._primera,
            bolas_embolsadas=[n for n, _ in self._embolsadas],
            troneras=[t for _, t in self._embolsadas],
            duracion_segundos=duracion,
            requiere_revision=bool(motivos),
            motivos=motivos,
        )
