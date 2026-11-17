"""Primer contacto, confianza y trazabilidad del analizador de tiros (H-05, H-12)."""
from ball_classifier import LISA, Observacion
from ball_detector import Deteccion
from ball_tracker import BallTracker
from shot_analyzer import ShotAnalyzer

TRONERAS = {"TOP_RIGHT": (400.0, 100.0)}


class Escena:
    def __init__(self, fps=10):
        self.tracker = BallTracker(max_perdidas=3)
        self.analizador = ShotAnalyzer(troneras=TRONERAS, frames_quietud=10, fps=fps)
        self.tiros = []

    def frame(self, *bolas):
        self.tracker.actualizar(
            [(Deteccion(int(x), int(y), 20), Observacion(n, "X", LISA, 0.9)) for x, y, n in bolas])
        tiro = self.analizador.actualizar(self.tracker)
        if tiro:
            self.tiros.append(tiro)


def _escena(inicio_a, inicio_b, pos_a=(200, 100), pos_b=(200, 140), fps=10):
    """La blanca avanza 5 fotogramas; la bola 5 empieza a moverse en ``inicio_a`` y la 6 en ``inicio_b``."""
    e = Escena(fps)
    fija = (300, 300, 3)
    for _ in range(5):
        e.frame((100, 100, 0), (*pos_a, 5), (*pos_b, 6), fija)

    def pos(base, inicio, f):
        return (base[0] + 10 * max(0, f - inicio + 1), base[1])

    for f in range(1, 13):
        e.frame((min(100 + 10 * f, 150), 100, 0), (*pos(pos_a, inicio_a, f), 5),
                (*pos(pos_b, inicio_b, f), 6), fija)
    ultimo = (150, 100, 0), (*pos(pos_a, inicio_a, 12), 5), (*pos(pos_b, inicio_b, 12), 6), fija
    for _ in range(25):
        e.frame(*ultimo)
    assert len(e.tiros) == 1
    return e.tiros[0]


def test_contacto_claro_se_acepta_con_confianza_y_fotogramas():
    t = _escena(inicio_a=3, inicio_b=99)  # la 6 nunca se mueve
    assert t.primera_bola_tocada == 5
    assert not [m for m in t.motivos if "contacto" in m.lower()]
    assert 0.9 <= t.confianza <= 1.0
    assert t.frame_fin > t.frame_inicio > 0


def test_dos_bolas_que_empiezan_a_moverse_a_la_vez_es_ambiguo():
    t = _escena(inicio_a=3, inicio_b=3)
    assert t.requiere_revision
    assert any("ambiguo" in m for m in t.motivos)


def test_una_bola_un_fotograma_despues_sigue_siendo_ambiguo():
    t = _escena(inicio_a=3, inicio_b=4)
    assert any("ambiguo" in m for m in t.motivos)


def test_separadas_mas_que_la_ventana_no_es_ambiguo():
    t = _escena(inicio_a=2, inicio_b=8)  # ventana a 10 fps = 2 fotogramas
    assert not any("ambiguo" in m for m in t.motivos)
    assert t.primera_bola_tocada == 5


def test_a_pocos_fps_la_ventana_no_se_reduce_por_debajo_de_dos_fotogramas():
    t = _escena(inicio_a=3, inicio_b=5, fps=3)  # ceil(0.15*3)=1 -> mínimo 2
    assert any("ambiguo" in m for m in t.motivos)


def test_bola_que_se_mueve_lejos_de_la_trayectoria_de_la_blanca_pide_revision():
    t = _escena(inicio_a=3, inicio_b=99, pos_a=(350, 220))
    assert t.requiere_revision
    assert any("no pasa cerca" in m for m in t.motivos)
