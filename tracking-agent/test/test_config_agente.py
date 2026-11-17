"""Configuración única versionada y su validación contra la resolución real (H-06)."""
import json

import pytest

import ball_classifier
import vision_agent
from config_agente import ConfigAgente, ErrorConfig, guardar_calibracion

TRONERAS = {"TOP_LEFT": (60.0, 50.0), "TOP_RIGHT": (1220.0, 50.0)}


def cfg_base():
    return ConfigAgente(resolucion_referencia=(1280, 720), roi=(80, 40, 1100, 640), troneras=TRONERAS,
                        detector={"param2": 28, "min_radius": 20, "max_radius": 40},
                        clasificador={"UMBRAL_RAYADA": 0.12})


def test_guardar_y_cargar_conserva_todo(tmp_path):
    ruta = str(tmp_path / "c.json")
    cfg_base().guardar(ruta)
    c = ConfigAgente.cargar(ruta)
    assert c.resolucion_referencia == (1280, 720) and c.roi == (80, 40, 1100, 640)
    assert c.troneras["TOP_LEFT"] == (60.0, 50.0) and c.detector["param2"] == 28
    assert json.load(open(ruta, encoding="utf-8"))["version"] == 1


def test_misma_proporcion_reescala_roi_troneras_y_radios():
    c = cfg_base().para_resolucion(640, 360)
    assert c.roi == (40, 20, 550, 320) and c.troneras["TOP_LEFT"] == (30.0, 25.0)
    assert c.detector["min_radius"] == 10 and c.detector["max_radius"] == 20
    assert c.detector["param2"] == 28            # no es un valor en píxeles
    assert c.config_detector().roi_mesa == (40, 20, 550, 320)


def test_otra_proporcion_se_rechaza_hay_que_recalibrar():
    with pytest.raises(ErrorConfig, match="proporción"):
        cfg_base().para_resolucion(640, 480)


def test_misma_resolucion_no_cambia_nada():
    assert cfg_base().para_resolucion(1280, 720).roi == (80, 40, 1100, 640)


@pytest.mark.parametrize("datos,mensaje", [
    ({"version": 2}, "Versión"),
    ({"version": 1, "roi": [1, 2, 3, 4]}, "resolucion_referencia"),
    ({"version": 1, "resolucion_referencia": [10, 10], "detector": {"foo": 1}}, "detector desconocido"),
    ({"version": 1, "resolucion_referencia": [10, 10], "clasificador": {"FOO": 1}}, "clasificador desconocido"),
    ({"version": 1, "resolucion_referencia": [10, 10], "roi": ["a"]}, "inválida"),
])
def test_configuraciones_invalidas(tmp_path, datos, mensaje):
    ruta = tmp_path / "c.json"
    ruta.write_text(json.dumps(datos), encoding="utf-8")
    with pytest.raises(ErrorConfig, match=mensaje):
        ConfigAgente.cargar(str(ruta))


def test_archivo_inexistente_o_no_json(tmp_path):
    with pytest.raises(ErrorConfig):
        ConfigAgente.cargar(str(tmp_path / "no.json"))
    (tmp_path / "x.json").write_text("{no", encoding="utf-8")
    with pytest.raises(ErrorConfig, match="JSON"):
        ConfigAgente.cargar(str(tmp_path / "x.json"))


def test_guardar_calibracion_acumula_y_rechaza_otra_resolucion(tmp_path):
    ruta = str(tmp_path / "c.json")
    guardar_calibracion(ruta, (1280, 720), roi=(1, 2, 3, 4))
    c = guardar_calibracion(ruta, (1280, 720), troneras=TRONERAS)
    assert c.roi == (1, 2, 3, 4) and c.troneras
    with pytest.raises(ErrorConfig, match="inconsistente"):
        guardar_calibracion(ruta, (640, 480), troneras=TRONERAS)


def test_umbrales_del_clasificador_se_validan_y_se_aplican():
    original = ball_classifier.umbrales_actuales()
    try:
        ball_classifier.configurar_umbrales(UMBRAL_RAYADA=0.2, COLOR_S_MIN=90)
        assert ball_classifier.UMBRAL_RAYADA == 0.2 and ball_classifier.COLOR_S_MIN == 90
        for malo in ({"NOEXISTE": 1}, {"UMBRAL_RAYADA": 2}, {"COLOR_S_MIN": 300}, {"UMBRAL_RAYADA": True}):
            with pytest.raises(ValueError):
                ball_classifier.configurar_umbrales(**malo)
    finally:
        ball_classifier.configurar_umbrales(**original)


def test_el_agente_rechaza_una_config_incompatible_con_la_fuente(tmp_path):
    import cv2
    import numpy as np
    video = str(tmp_path / "v.mp4")
    w = cv2.VideoWriter(video, cv2.VideoWriter_fourcc(*"mp4v"), 10, (160, 120))   # 4:3
    for _ in range(2):
        w.write(np.zeros((120, 160, 3), np.uint8))
    w.release()
    ruta = str(tmp_path / "c.json")
    cfg_base().guardar(ruta)                                                       # calibrada a 16:9
    assert vision_agent.main(["--source", video, "--no-gui", "--config", ruta]) == 2


def test_el_agente_usa_la_config_compatible(tmp_path, capsys):
    import cv2
    import numpy as np
    video = str(tmp_path / "v.mp4")
    w = cv2.VideoWriter(video, cv2.VideoWriter_fourcc(*"mp4v"), 10, (160, 90))     # 16:9
    for _ in range(3):
        w.write(np.zeros((90, 160, 3), np.uint8))
    w.release()
    ruta = str(tmp_path / "c.json")
    ConfigAgente(resolucion_referencia=(320, 180), roi=(10, 10, 280, 150), troneras=TRONERAS).guardar(ruta)
    assert vision_agent.main(["--source", video, "--no-gui", "--config", ruta]) == 0
