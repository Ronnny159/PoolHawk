"""Pruebas de ``shot_analyzer`` (Fase 5): de seguimiento a tiro observado."""
from ball_classifier import LISA, Observacion
from ball_detector import Deteccion
from ball_tracker import BallTracker
from shot_analyzer import ShotAnalyzer

TRONERAS = {"TOP_RIGHT": (400.0, 100.0), "BOTTOM_LEFT": (20.0, 400.0)}


def _entrada(x, y, n):
    return (Deteccion(int(x), int(y), 20), Observacion(n, "X", LISA, 0.9))


class Escena:
    """Alimenta tracker + analizador fotograma a fotograma."""

    def __init__(self, troneras=TRONERAS):
        self.tracker = BallTracker(max_perdidas=3)
        self.analizador = ShotAnalyzer(troneras=troneras, frames_quietud=10, fps=10)
        self.tiros = []

    def frame(self, *bolas):
        self.tracker.actualizar([_entrada(x, y, n) for x, y, n in bolas])
        tiro = self.analizador.actualizar(self.tracker)
        if tiro:
            self.tiros.append(tiro)

    def quietos(self, n, *bolas):
        for _ in range(n):
            self.frame(*bolas)


def _tiro_embolsando_la_5(escena, destino_5=(395, 100)):
    """Blanca (0) golpea a la 5, que sale rumbo a ``destino_5`` y desaparece."""
    fijas = [(300, 300, 3)]
    escena.quietos(5, (100, 100, 0), (200, 100, 5), *fijas)           # fija identidades
    for i in range(1, 6):                                              # blanca se mueve
        escena.frame((100 + 10 * i, 100, 0), (200, 100, 5), *fijas)
    for i in range(1, 20):                                             # la 5 se mueve
        x = min(200 + 10 * i, destino_5[0])
        escena.frame((150, 100, 0), (x, destino_5[1], 5), *fijas)
        if x >= destino_5[0]:
            break
    escena.quietos(25, (150, 100, 0), *fijas)                          # la 5 ya no aparece


def test_tiro_con_embolsado_en_tronera():
    e = Escena()
    _tiro_embolsando_la_5(e)
    assert len(e.tiros) == 1
    t = e.tiros[0]
    assert t.primera_bola_tocada == 5
    assert t.bolas_embolsadas == [5]
    assert t.troneras == ["TOP_RIGHT"]
    assert not t.requiere_revision
    assert t.duracion_segundos >= 1


def test_bola_perdida_lejos_de_tronera_no_se_da_por_embolsada():
    e = Escena()
    _tiro_embolsando_la_5(e, destino_5=(250, 300))
    t = e.tiros[0]
    assert t.bolas_embolsadas == []
    assert t.requiere_revision
    assert any("lejos de toda tronera" in m for m in t.motivos)


def test_sin_troneras_calibradas_requiere_revision():
    e = Escena(troneras={})
    _tiro_embolsando_la_5(e)
    t = e.tiros[0]
    assert t.bolas_embolsadas == []
    assert t.requiere_revision
    assert any("Sin troneras" in m for m in t.motivos)


def test_mesa_quieta_nunca_genera_tiro():
    e = Escena()
    e.quietos(60, (100, 100, 0), (200, 100, 5))
    assert e.tiros == [] and not e.analizador.en_tiro


def test_tiro_sin_embolsar_termina_cuando_todo_queda_quieto():
    e = Escena()
    e.quietos(5, (100, 100, 0), (200, 100, 5))
    for i in range(1, 6):
        e.frame((100 + 10 * i, 100, 0), (200, 100, 5))
    for i in range(1, 4):
        e.frame((150, 100, 0), (200 + 8 * i, 100, 5))
    e.quietos(15, (150, 100, 0), (224, 100, 5))
    assert len(e.tiros) == 1
    assert e.tiros[0].bolas_embolsadas == []
    assert e.tiros[0].primera_bola_tocada == 5


def test_puede_detectar_un_segundo_tiro_despues_del_primero():
    e = Escena()
    _tiro_embolsando_la_5(e)
    assert len(e.tiros) == 1
    for i in range(1, 6):
        e.frame((150 + 10 * i, 100, 0), (300, 300, 3))
    e.quietos(15, (200, 100, 0), (300, 300, 3))
    assert len(e.tiros) == 2


def test_cada_tiro_observado_nace_con_un_event_id_unico():
    from shot_analyzer import TiroObservado
    a = TiroObservado(None, [], [], 1, True)
    b = TiroObservado(None, [], [], 1, True)
    assert a.event_id and a.event_id != b.event_id
