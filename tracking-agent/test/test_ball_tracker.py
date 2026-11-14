"""Pruebas de ``ball_tracker`` (H-05): identidad persistente entre fotogramas."""
from ball_classifier import LISA, Observacion
from ball_detector import Deteccion
from ball_tracker import BallTracker


def _obs(numero, confianza=0.9):
    return Observacion(numero, "X", LISA, confianza)


def _frame(*bolas):
    """bolas: (x, y, numero|None)"""
    return [(Deteccion(x, y, 20), _obs(n) if n is not None else None) for x, y, n in bolas]


def test_bola_en_movimiento_conserva_su_id():
    t = BallTracker()
    ids = []
    for i in range(10):
        tracks = t.actualizar(_frame((100 + 6 * i, 200, 5)))
        ids.append(tracks[0].id)
    assert len(set(ids)) == 1
    assert tracks[0].en_movimiento
    assert tracks[0].numero == 5


def test_dos_bolas_cercanas_no_intercambian_identidad():
    t = BallTracker()
    id_por_numero = {}
    for i in range(12):
        tracks = t.actualizar(_frame((100 + 5 * i, 100, 1), (300 - 5 * i, 130, 2)))
        for tr in tracks:
            if tr.numero is not None:
                id_por_numero.setdefault(tr.numero, set()).add(tr.id)
    assert len(t.tracks) == 2
    assert all(len(ids) == 1 for ids in id_por_numero.values())   # cada número = un solo id
    assert set(id_por_numero) == {1, 2}


def test_una_clasificacion_erronea_aislada_no_cambia_el_numero():
    t = BallTracker()
    secuencia = [5, 5, 5, 11, 5, 5]
    for n in secuencia:
        tracks = t.actualizar(_frame((100, 100, n)))
    assert tracks[0].numero == 5


def test_numero_desconocido_hasta_tener_votos_suficientes():
    t = BallTracker()
    tracks = t.actualizar(_frame((100, 100, 5)))
    assert tracks[0].numero is None


def test_observaciones_poco_confiables_no_votan():
    t = BallTracker()
    for _ in range(5):
        tracks = t.actualizar([(Deteccion(100, 100, 20), _obs(5, confianza=0.2))])
    assert tracks[0].numero is None


def test_bola_oculta_temporalmente_se_recupera_con_el_mismo_id():
    t = BallTracker(max_perdidas=5)
    id0 = t.actualizar(_frame((100, 100, 3)))[0].id
    for _ in range(3):                       # oculta 3 fotogramas
        assert t.actualizar([]) == []
    tracks = t.actualizar(_frame((101, 100, 3)))
    assert tracks[0].id == id0
    assert t.ultimas_perdidas == []


def test_bola_desaparecida_se_reporta_como_perdida_con_ultima_posicion():
    t = BallTracker(max_perdidas=3)
    for _ in range(4):
        t.actualizar(_frame((100, 100, 7)))
    reportadas = []
    for _ in range(5):
        t.actualizar([])
        reportadas += t.ultimas_perdidas
    assert len(reportadas) == 1
    assert (reportadas[0].x, reportadas[0].y) == (100, 100)
    assert reportadas[0].numero == 7
    assert t.tracks == []


def test_deteccion_lejana_crea_pista_nueva():
    t = BallTracker(max_distancia=30)
    t.actualizar(_frame((100, 100, 1)))
    tracks = t.actualizar(_frame((100, 100, 1), (400, 400, 2)))
    assert len({tr.id for tr in tracks}) == 2


def test_todas_quietas_requiere_frames_sin_movimiento():
    t = BallTracker()
    for _ in range(12):
        t.actualizar(_frame((100, 100, 1), (200, 200, 2)))
    assert t.todas_quietas(10)
    t.actualizar(_frame((140, 100, 1), (200, 200, 2)))   # una bola se mueve
    assert not t.todas_quietas(10)


def test_sin_bolas_no_se_considera_mesa_quieta():
    assert not BallTracker().todas_quietas(5)
