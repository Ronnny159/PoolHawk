"""
PoolHawk - Agente de Visión Computarizada.

Pipeline: fuente de vídeo -> detección de círculos -> clasificación de cada bola ->
seguimiento temporal con identidad persistente.

Con ``--match-id`` el agente infiere tiros (ShotAnalyzer) y los envía al backend mediante
el cliente REST. Los tiros dudosos (sin troneras calibradas, bola sin número, desaparición
lejos de una tronera) NO se envían: se imprimen para revisión manual.
"""
from __future__ import annotations

import argparse
import json
import sys
from typing import List, Optional, Tuple

import cv2
import numpy as np

from ball_classifier import Observacion, clasificar_bola, recortar_roi
from ball_detector import BallDetector, ConfigDetector, Deteccion
from ball_tracker import BallTracker, Track
from camera_source import abrir_fuente, leer_frames
from api_client import (ErrorApi, ErrorConflicto, ErrorRed, PoolHawkApiClient,
                        ReintentadorPendientes)
from cola_eventos import ColaEventos
from shot_analyzer import ShotAnalyzer, TiroObservado
import traza
from traza import TrazaTiros


def procesar_frame(
    frame: np.ndarray, detector: BallDetector, tracker: BallTracker
) -> List[Track]:
    """Detecta, clasifica y sigue las bolas de un fotograma (sin dibujar nada).

    Returns:
        Pistas visibles en este fotograma.
    """
    entradas: List[Tuple[Deteccion, Optional[Observacion]]] = []
    for det in detector.detectar(frame):
        roi = recortar_roi(frame, det.x, det.y, det.r)
        entradas.append((det, clasificar_bola(roi)))
    return tracker.actualizar(entradas)


def dibujar(frame: np.ndarray, tracks: List[Track]) -> None:
    """Dibuja las pistas sobre el fotograma (solo para visualización local)."""
    for t in tracks:
        numero = t.numero
        color = (0, 255, 0) if numero is not None else (0, 0, 255)
        centro = (int(t.x), int(t.y))
        cv2.circle(frame, centro, int(t.r), color, 2)
        etiqueta = f"id{t.id} #{numero}" if numero is not None else f"id{t.id} ?"
        if t.en_movimiento:
            etiqueta += " >>"
        cv2.putText(frame, etiqueta, (centro[0] - 30, centro[1] - int(t.r) - 8),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)


def _parsear_roi(texto: Optional[str]) -> Optional[Tuple[int, int, int, int]]:
    if not texto:
        return None
    try:
        x, y, w, h = (int(v) for v in texto.split(","))
    except ValueError:
        raise argparse.ArgumentTypeError("--roi debe ser x,y,ancho,alto (enteros)")
    return x, y, w, h


def _cargar_troneras(ruta: Optional[str]) -> dict:
    """Lee un JSON ``{"TOP_LEFT": [x, y], ...}`` con la posición (px) de cada tronera."""
    if not ruta:
        return {}
    with open(ruta, encoding="utf-8") as f:
        return {k: (float(v[0]), float(v[1])) for k, v in json.load(f).items()}


def manejar_tiro(tiro: TiroObservado, cliente: Optional[PoolHawkApiClient],
                 match_id: Optional[str], registro: Optional[TrazaTiros] = None) -> str:
    """Envía el tiro al backend si es confiable; si no, lo reporta para revisión.

    Cada decisión queda anotada en la traza (H-12).

    Returns:
        El estado final del tiro (constantes de :mod:`traza`).
    """
    registro = registro or TrazaTiros(None)
    if tiro.requiere_revision:
        motivos = "; ".join(tiro.motivos)
        print(f"🔎 Tiro NO enviado (requiere revisión): {motivos}")
        registro.registrar(tiro, traza.REVISION, match_id)
        return traza.REVISION
    if cliente is None or not match_id:
        print(f"ℹ️  Tiro observado (sin API configurada): {tiro}")
        registro.registrar(tiro, traza.SIN_API, match_id)
        return traza.SIN_API
    try:
        cliente.registrar_tiro(match_id, tiro)
    except ErrorConflicto as e:
        print(f"⚠️  Tiro descartado: el estado de la partida cambió ({e}). Revisar manualmente.",
              file=sys.stderr)
        registro.registrar(tiro, traza.CONFLICTO, match_id, str(e))
        return traza.CONFLICTO
    except ErrorRed as e:
        print(f"⏳ API no disponible; el tiro queda pendiente y se reenviará ({e})", file=sys.stderr)
        registro.registrar(tiro, traza.PENDIENTE, match_id, str(e))
        return traza.PENDIENTE
    except ErrorApi as e:
        print(f"❌ No se pudo enviar el tiro: {e}", file=sys.stderr)
        registro.registrar(tiro, traza.RECHAZADO, match_id, str(e))
        return traza.RECHAZADO
    print(f"✅ Tiro enviado: primera={tiro.primera_bola_tocada} embolsadas={tiro.bolas_embolsadas}")
    registro.registrar(tiro, traza.ENVIADO, match_id)
    return traza.ENVIADO


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Agente de Visión PoolHawk")
    parser.add_argument("--match-id", help="ID de la partida; si se indica, los tiros confiables se envían a la API")
    parser.add_argument("--api-url", default="http://localhost:8080", help="URL base del backend")
    parser.add_argument("--token", default=None, help="JWT opcional para la API")
    parser.add_argument("--cola", default=".poolhawk_cola.json",
                        help="Archivo donde se guardan los tiros pendientes de envío (sobrevive a reinicios)")
    parser.add_argument("--traza", default=".poolhawk_traza.jsonl",
                        help="Registro JSON Lines de cada tiro (enviado, pendiente, en revisión...)")
    parser.add_argument("--troneras", default=None,
                        help="JSON con la posición en px de cada tronera (necesario para inferir embolsados)")
    parser.add_argument("--source", default="0", help="Índice de cámara, ruta de vídeo o URL (por defecto 0)")
    parser.add_argument("--roi", type=_parsear_roi, default=None, help="Región de la mesa: x,y,ancho,alto")
    parser.add_argument("--no-gui", action="store_true", help="No abrir ventana (modo headless)")
    parser.add_argument("--max-frames", type=int, default=0, help="Procesar solo N fotogramas (0 = todos)")
    args = parser.parse_args(argv)

    try:
        cap = abrir_fuente(args.source)
    except RuntimeError as e:
        print(f"❌ {e}", file=sys.stderr)
        return 1

    ancho = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)) or 640
    config = ConfigDetector(roi_mesa=args.roi).escalada(ancho)
    detector, tracker = BallDetector(config), BallTracker()
    fps = cap.get(cv2.CAP_PROP_FPS) or 15.0
    analizador = ShotAnalyzer(troneras=_cargar_troneras(args.troneras), fps=fps)
    cliente, reintentador = None, None
    registro = TrazaTiros(args.traza)
    if args.match_id:
        try:
            cliente = PoolHawkApiClient(args.api_url, token=args.token, cola=ColaEventos(args.cola))
        except ValueError as e:
            print(f"❌ Configuración inválida: {e}", file=sys.stderr)
            return 2
        resumen = cliente.reenviar_pendientes()
        if resumen["enviados"] or resumen["rechazados"] or resumen["pendientes"]:
            print(f"♻️  Reenvío de pendientes: {resumen}")
        reintentador = ReintentadorPendientes(cliente)
        reintentador.iniciar()  # sigue reenviando en segundo plano mientras corre la cámara

    print("📷 Fuente abierta." + ("" if args.no_gui else " Presiona 'q' para salir."))
    procesados = 0
    try:
        for frame in leer_frames(cap, args.max_frames):
            tracks = procesar_frame(frame, detector, tracker)
            procesados += 1
            tiro = analizador.actualizar(tracker)
            if tiro is not None:
                manejar_tiro(tiro, cliente, args.match_id, registro)
            for t in tracker.ultimas_perdidas:
                print(f"⚠️  Bola id{t.id} (#{t.numero}) perdida en ({t.x:.0f},{t.y:.0f})")
            if not args.no_gui:
                dibujar(frame, tracks)
                cv2.imshow("PoolHawk Vision Agent", frame)
                if cv2.waitKey(1) & 0xFF == ord("q"):
                    break
    finally:
        if reintentador is not None:
            reintentador.detener()
        cap.release()
        if not args.no_gui:
            cv2.destroyAllWindows()
    print(f"Fotogramas procesados: {procesados}. Bolas seguidas: {len(tracker.tracks)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())