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

import math
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
    REST solo añade ``jugadorId`` y ``versionEsperada``.

    Para trazabilidad (H-12) incluye ``confianza`` (la menor confianza de identidad entre las
    bolas implicadas, 0-1) y los fotogramas de inicio y fin del tiro."""

    primera_bola_tocada: Optional[int]
    bolas_embolsadas: List[int]
    troneras: List[str]
    duracion_segundos: int
    requiere_revision: bool
    motivos: List[str] = field(default_factory=list)
    event_id: str = field(default_factory=lambda: str(uuid.uuid4()))
    confianza: float = 1.0
    frame_inicio: int = 0
    frame_fin: int = 0


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
        ventana_contacto_s: float = 0.15,
        factor_contacto: float = 2.5,
    ):
        self.ventana_contacto_s = ventana_contacto_s
        self.factor_contacto = factor_contacto
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
        self._trayectoria_blanca: List[Tuple[float, float, float]] = []
        self._pos_previa: Dict[int, Punto] = {}
        self._candidatos: List[_Candidato] = []
        self._ids_candidatos: set = set()
        self._embolsadas: List[Tuple[int, str]] = []
        self._confianzas: List[float] = []
        self._motivos: List[str] = []

    def _observar_movimiento(self, visibles: List[Track]) -> None:
        """Registra cuándo empieza a moverse cada bola respecto a la blanca."""
        blanca = next((t for t in visibles if t.numero == 0), None)
        if not self._blanca_se_movio:
            if blanca is not None and blanca.en_movimiento:
                self._blanca_se_movio = True
                self._trayectoria_blanca.append((blanca.x, blanca.y, blanca.r))
        else:
            if blanca is not None:
                self._trayectoria_blanca.append((blanca.x, blanca.y, blanca.r))
            for t in visibles:
                if t.numero == 0 or not t.en_movimiento or t.id in self._ids_candidatos:
                    continue
                inicial = self._pos_previa.get(t.id, (t.x, t.y))
                self._candidatos.append(_Candidato(t, self._frame, inicial))
                self._ids_candidatos.add(t.id)
        self._pos_previa = {t.id: (t.x, t.y) for t in visibles}

    def _evaluar_primer_contacto(self) -> Tuple[Optional[int], List[str]]:
        """Decide la primera bola tocada o, si la evidencia no basta, pide revisión (H-05).

        La primera bola que empieza a moverse NO es necesariamente la golpeada por la blanca
        (el movimiento se transmite entre bolas y con pocos fps el orden se pierde). Solo se
        acepta el candidato si: (a) la trayectoria de la blanca pasa cerca de su posición
        inicial y (b) ninguna otra bola empezó a moverse dentro de la ventana de contacto.
        """
        if not self._candidatos:
            return None, []
        candidatos = sorted(self._candidatos, key=lambda c: c.frame)
        primero = candidatos[0]
        motivos: List[str] = []

        ventana = max(2, math.ceil(self.ventana_contacto_s * self.fps))
        simultaneos = [c for c in candidatos[1:] if c.frame - primero.frame <= ventana]
        if simultaneos:
            nombres = ", ".join(_nombre(c.track) for c in [primero] + simultaneos)
            motivos.append(
                f"Primer contacto ambiguo: {nombres} empezaron a moverse dentro de "
                f"{ventana} fotogramas ({ventana / self.fps * 1000:.0f} ms a {self.fps:.0f} fps)")

        if not self._blanca_pasa_cerca(primero):
            motivos.append(
                f"La trayectoria de la blanca no pasa cerca de {_nombre(primero.track)}: "
                "contacto no observado")

        if primero.track.numero is None:
            motivos.append("La primera bola tocada no tiene número identificado")
            return None, motivos
        self._confianzas.append(_confianza(primero.track))
        return primero.track.numero, motivos

    def _blanca_pasa_cerca(self, c: "_Candidato") -> bool:
        cx, cy = c.inicial
        for bx, by, br in self._trayectoria_blanca:
            if math.hypot(bx - cx, by - cy) <= self.factor_contacto * (br + c.track.r):
                return True
        return False

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
                self._confianzas.append(_confianza(t))

    def _cerrar(self) -> TiroObservado:
        motivos = list(self._motivos)
        if not self.troneras:
            motivos.append("Sin troneras calibradas: no se pueden inferir embolsados")
        if not self._blanca_se_movio:
            motivos.append("No se observó movimiento de la bola blanca")
        primera, motivos_contacto = self._evaluar_primer_contacto()
        motivos.extend(motivos_contacto)
        inicio = self._inicio or self._frame
        duracion = max(1, round((self._frame - inicio) / self.fps))
        return TiroObservado(
            primera_bola_tocada=primera,
            bolas_embolsadas=[n for n, _ in self._embolsadas],
            troneras=[t for _, t in self._embolsadas],
            duracion_segundos=duracion,
            requiere_revision=bool(motivos),
            motivos=motivos,
            confianza=round(min(self._confianzas, default=1.0), 3),
            frame_inicio=inicio,
            frame_fin=self._frame,
        )


@dataclass
class _Candidato:
    """Bola que empezó a moverse después de la blanca (posible primer contacto)."""

    track: Track
    frame: int
    inicial: Punto


def _nombre(t: Track) -> str:
    return f"#{t.numero}" if t.numero is not None else f"id{t.id}"


def _confianza(t: Track) -> float:
    """Proporción de votos del número ganador de la pista (0 si no tiene evidencia)."""
    total = sum(t.votos.values())
    return t.votos.most_common(1)[0][1] / total if total else 0.0
