"""Pruebas de la asignación global del tracker: cruces, oclusión y óptimo (H-04, H-09)."""
import itertools
import random

from ball_classifier import LISA, Observacion
from ball_detector import Deteccion
from ball_tracker import BallTracker, asignacion_optima


def _obs(numero):
    return Observacion(numero, "X", LISA, 0.9)


def _frame(*bolas):
    """bolas: (x, y, numero|None)"""
    return [(Deteccion(x, y, 20), _obs(n) if n is not None else None) for x, y, n in bolas]


def _coste_total(costos, pares):
    return sum(costos[i][j] for i, j in pares)


def _optimo_fuerza_bruta(costos):
    filas, columnas = len(costos), len(costos[0])
    if filas <= columnas:
        return min(sum(costos[i][c[i]] for i in range(filas))
                   for c in itertools.permutations(range(columnas), filas))
    return min(sum(costos[c[j]][j] for j in range(columnas))
               for c in itertools.permutations(range(filas), columnas))


def test_asignacion_optima_coincide_con_fuerza_bruta():
    azar = random.Random(7)
    for _ in range(200):
        n, m = azar.randint(1, 5), azar.randint(1, 5)
        costos = [[azar.uniform(0, 10) for _ in range(m)] for _ in range(n)]
        pares = asignacion_optima(costos)
        assert len(pares) == min(n, m)
        assert len({i for i, _ in pares}) == len(pares) == len({j for _, j in pares})
        assert abs(_coste_total(costos, pares) - _optimo_fuerza_bruta(costos)) < 1e-9


def test_asignacion_vacia():
    assert asignacion_optima([]) == [] and asignacion_optima([[]]) == []


def test_caso_donde_la_asociacion_voraz_intercambia_identidades():
    """A en x=0 y B en x=10; llegan detecciones en x=6 y x=20.
    Voraz: B->6 (4) y A->20 (20) = 24. Óptimo: A->6 (6) y B->20 (10) = 16."""
    t = BallTracker(max_distancia=40)
    t.actualizar(_frame((100, 100, None), (110, 100, None)))
    id_a, id_b = sorted(tr.id for tr in t.tracks)
    t.actualizar(_frame((106, 100, None), (120, 100, None)))
    pos = {tr.id: tr.x for tr in t.tracks}
    assert pos[id_a] == 106 and pos[id_b] == 120


def test_cruce_de_dos_bolas_conserva_cada_identidad_por_clasificacion():
    t = BallTracker(max_distancia=40)
    ids = {}
    for i in range(14):
        # la bola 1 va de izquierda a derecha y la 2 al revés, cruzándose a 6 px de distancia
        tracks = t.actualizar(_frame((100 + 8 * i, 100, 1), (212 - 8 * i, 106, 2)))
        for tr in tracks:
            if tr.numero is not None:
                ids.setdefault(tr.numero, set()).add(tr.id)
    assert all(len(v) == 1 for v in ids.values()) and set(ids) == {1, 2}
    assert len(t.tracks) == 2


def test_oclusion_breve_la_bola_reaparece_con_la_misma_identidad():
    t = BallTracker(max_distancia=40, max_perdidas=15)
    for _ in range(5):
        t.actualizar(_frame((200, 200, 4)))
    id_original = t.tracks[0].id
    for _ in range(6):  # oculta 6 fotogramas
        t.actualizar([])
    tracks = t.actualizar(_frame((203, 201, 4)))
    assert len(tracks) == 1 and tracks[0].id == id_original and tracks[0].numero == 4


def test_deteccion_lejana_no_se_asocia_y_crea_pista_nueva():
    t = BallTracker(max_distancia=40)
    t.actualizar(_frame((100, 100, 1)))
    t.actualizar(_frame((400, 300, 2)))
    assert len(t.tracks) == 2


def test_mas_detecciones_que_pistas_y_viceversa():
    t = BallTracker(max_distancia=40)
    t.actualizar(_frame((100, 100, 1)))
    t.actualizar(_frame((101, 100, 1), (300, 300, 3), (500, 100, 5)))
    assert len(t.tracks) == 3
    t.actualizar(_frame((102, 100, 1)))
    assert len([x for x in t.tracks if x.visible]) == 1


# ---- casos difíciles adicionales (H-05 de la auditoría 2088938)
def test_dos_bolas_del_mismo_color_aparente_no_se_fusionan_ni_intercambian():
    t = BallTracker(max_distancia=40)
    for i in range(10):  # ambas se clasifican igual (mismo número) y se acercan sin tocarse
        tracks = t.actualizar(_frame((100 + 6 * i, 100, 1), (220 - 6 * i, 112, 1)))
    assert len(t.tracks) == 2
    xs = sorted(tr.x for tr in t.tracks)
    assert xs[0] < xs[1]                          # no se cruzaron las identidades de posición


def test_deteccion_falsa_entre_dos_pistas_no_roba_ninguna_identidad():
    t = BallTracker(max_distancia=40)
    for _ in range(4):
        t.actualizar(_frame((100, 100, 1), (160, 100, 2)))
    ids = {tr.numero: tr.id for tr in t.tracks}
    t.actualizar(_frame((100, 100, 1), (160, 100, 2), (130, 100, None)))   # reflejo entre ambas
    por_numero = {tr.numero: tr.id for tr in t.tracks if tr.numero is not None}
    assert por_numero == ids and len(t.tracks) == 3


def test_pista_sin_deteccion_fiable_queda_sin_correspondencia_en_vez_de_aceptar_un_par_dudoso():
    t = BallTracker(max_distancia=40)
    for _ in range(4):
        t.actualizar(_frame((100, 100, 1)))
    vieja = t.tracks[0].id
    # a 34 px (casi todo el alcance) aparece una bola CLARAMENTE distinta (otro número): no es la misma
    t.actualizar(_frame((134, 100, 5)))
    nuevas = [tr for tr in t.tracks if tr.id != vieja]
    assert len(nuevas) == 1 and nuevas[0].x == 134
    assert next(tr for tr in t.tracks if tr.id == vieja).x == 100        # la vieja no se movió


def test_clasificacion_erronea_aislada_en_la_misma_posicion_conserva_la_identidad():
    t = BallTracker(max_distancia=40)
    for _ in range(5):
        t.actualizar(_frame((100, 100, 1)))
    original = t.tracks[0].id
    t.actualizar(_frame((100, 100, 7)))          # un fotograma mal clasificado
    assert len(t.tracks) == 1 and t.tracks[0].id == original and t.tracks[0].numero == 1
