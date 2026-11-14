"""Pruebas de ``camera_source`` y del pipeline completo con un vídeo sintético."""
import cv2
import numpy as np
import pytest

import Vision_agent
from ball_detector import BallDetector
from ball_tracker import BallTracker
from camera_source import abrir_fuente, leer_frames


def _frame(x):
    img = np.full((480, 640, 3), (60, 110, 40), dtype=np.uint8)
    cv2.circle(img, (x, 240), 22, (0, 255, 255), -1)
    cv2.circle(img, (x, 240), 22, (20, 20, 20), 2)
    cv2.circle(img, (x, 240), 8, (255, 255, 255), -1)
    return img


@pytest.fixture
def video(tmp_path):
    ruta = str(tmp_path / "mesa.avi")
    w = cv2.VideoWriter(ruta, cv2.VideoWriter_fourcc(*"MJPG"), 15, (640, 480))
    for i in range(20):
        w.write(_frame(100 + 8 * i))
    w.release()
    return ruta


def test_fuente_inexistente_lanza_error_claro():
    with pytest.raises(RuntimeError):
        abrir_fuente("no_existe.avi")


def test_leer_frames_respeta_max_frames(video):
    cap = abrir_fuente(video)
    assert len(list(leer_frames(cap, max_frames=5))) == 5
    cap.release()


def test_pipeline_sigue_la_misma_bola_en_todo_el_video(video):
    cap = abrir_fuente(video)
    detector, tracker = BallDetector(), BallTracker()
    ids = set()
    for frame in leer_frames(cap):
        for t in Vision_agent.procesar_frame(frame, detector, tracker):
            ids.add(t.id)
    cap.release()
    assert 1 <= len(ids) <= 2        # tolerancia a un fallo aislado de Hough


def test_main_headless_con_video_termina_bien(video, capsys):
    assert Vision_agent.main(["--source", video, "--no-gui", "--max-frames", "10"]) == 0
    assert "Fotogramas procesados: 10" in capsys.readouterr().out


def test_main_con_fuente_invalida_devuelve_error(capsys):
    assert Vision_agent.main(["--source", "no_existe.avi", "--no-gui"]) == 1
