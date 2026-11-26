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
from dataclasses import replace
import json
import os
import sys
from typing import List, Optional, Tuple

import cv2
import numpy as np

from ball_classifier import Observacion, clasificar_bola, recortar_roi
from ball_detector import BallDetector, ConfigDetector, Deteccion
from ball_tracker import BallTracker, Track
from camera_source import CameraSource, abrir_fuente, leer_frames
from api_client import (ErrorApi, ErrorColaBloqueada, ErrorConflicto, ErrorEstadoNoDisponible,
                        ErrorRed, PoolHawkApiClient, ReintentadorPendientes)
from ball_classifier import configurar_umbrales
from config_agente import ConfigAgente, ErrorConfig
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

    La traza recibe cada transición (``OBSERVADO`` y luego el resultado) con el mismo
    ``eventId``. Solo un fallo del POST deja el tiro pendiente: si falla la consulta inicial
    de la partida el tiro NO está en la cola y se marca para revisión manual.

    Returns:
        El estado final del tiro (constantes de :mod:`traza`).
    """
    registro = registro or TrazaTiros(None)
    registro.registrar(tiro, traza.OBSERVADO, match_id)
    if tiro.requiere_revision:
        motivos = "; ".join(tiro.motivos)
        print(f"🔎 Tiro NO enviado (requiere revisión): {motivos}")
        registro.transicion(tiro.event_id, traza.REVISION, match_id, motivos)
        return traza.REVISION
    if cliente is None or not match_id:
        print(f"ℹ️  Tiro observado (sin API configurada): {tiro}")
        registro.transicion(tiro.event_id, traza.SIN_API, match_id)
        return traza.SIN_API
    try:
        cliente.registrar_tiro(match_id, tiro)
    except ErrorColaBloqueada as e:
        print(f"⛔ {e}", file=sys.stderr)
        registro.transicion(tiro.event_id, traza.BLOQUEADO, match_id, str(e))
        return traza.BLOQUEADO
    except ErrorEstadoNoDisponible as e:
        print(f"🔎 {e}", file=sys.stderr)
        registro.transicion(tiro.event_id, traza.ESTADO_NO_DISPONIBLE, match_id, str(e))
        return traza.ESTADO_NO_DISPONIBLE
    except ErrorConflicto as e:
        print(f"⚠️  Tiro descartado: el estado de la partida cambió ({e}). Revisar manualmente.",
              file=sys.stderr)
        registro.transicion(tiro.event_id, traza.CONFLICTO, match_id, str(e))
        return traza.CONFLICTO
    except ErrorRed as e:
        print(f"⏳ El envío falló pero el tiro ya está en la cola y se reenviará ({e})", file=sys.stderr)
        registro.transicion(tiro.event_id, traza.PENDIENTE, match_id, str(e))
        return traza.PENDIENTE
    except ErrorApi as e:
        print(f"❌ No se pudo enviar el tiro: {e}", file=sys.stderr)
        registro.transicion(tiro.event_id, traza.RECHAZADO, match_id, str(e))
        return traza.RECHAZADO
    print(f"✅ Tiro enviado: primera={tiro.primera_bola_tocada} embolsadas={tiro.bolas_embolsadas}")
    registro.transicion(tiro.event_id, traza.ENVIADO, match_id)
    return traza.ENVIADO


def gestionar_cola(args: argparse.Namespace) -> Optional[int]:
    """Atiende ``--reanudar-cola`` y ``--descartar-evento``; devuelve el código de salida o ``None``."""
    if not (args.reanudar_cola or args.descartar_evento):
        return None
    cola = ColaEventos(args.cola)
    registro = TrazaTiros(args.traza)
    if args.descartar_evento:
        cola.rechazar(args.descartar_evento, "descartado manualmente")
        registro.transicion(args.descartar_evento, traza.RECHAZADO, None, "descartado manualmente")
        print(f"🗑️  Evento {args.descartar_evento} descartado. Pendientes: {len(cola.pendientes())}")
    if args.reanudar_cola:
        desbloqueado = cola.reanudar()
        print(f"▶️  Cola reanudada (evento {desbloqueado})." if desbloqueado
              else "La cola no estaba bloqueada.")
    return 0


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Agente de Visión PoolHawk")
    parser.add_argument("--match-id", help="ID de la partida; si se indica, los tiros confiables se envían a la API")
    parser.add_argument("--api-url", default="http://localhost:8080", help="URL base del backend")
    parser.add_argument("--token", default=None, help="JWT opcional para la API")
    parser.add_argument("--cola", default=".poolhawk_cola.json",
                        help="Archivo donde se guardan los tiros pendientes de envío (sobrevive a reinicios)")
    parser.add_argument("--traza", default=".poolhawk_traza.jsonl",
                        help="Registro JSON Lines de las transiciones de cada tiro")
    parser.add_argument("--config", default=None,
                        help="configuracion.json (ROI, troneras, detector y clasificador a una misma "
                             "resolución; se valida contra la fuente). --roi/--troneras lo anulan")
    parser.add_argument("--troneras", default=None,
                        help="JSON con la posición en px de cada tronera (necesario para inferir embolsados)")
    parser.add_argument("--reanudar-cola", action="store_true",
                        help="Desbloquea la cola tras reconciliar con el backend y sale")
    parser.add_argument("--descartar-evento", default=None, metavar="EVENT_ID",
                        help="Descarta un evento pendiente (p. ej. el que bloquea la cola) y sale")
    parser.add_argument("--profile", type=str, default=None, help="Perfil de cámara predefinido")
    parser.add_argument("--source", type=str, default=None, help="Índice de cámara, ruta de vídeo o URL")
    parser.add_argument("--backend", type=str, default=None, help="Backend de cámara")
    parser.add_argument("--roi", type=_parsear_roi, default=None, help="Región de la mesa: x,y,ancho,alto")
    parser.add_argument("--no-gui", action="store_true", help="No abrir ventana (modo headless)")
    parser.add_argument("--max-frames", type=int, default=0, help="Procesar solo N fotogramas (0 = todos)")
    args = parser.parse_args(argv)

    atendido = gestionar_cola(args)
    if atendido is not None:
        return atendido

    # ==========================================
    # LÓGICA DE RESOLUCIÓN DE FUENTE DE VIDEO
    # ==========================================
    from config_agente import CAMERA_PROFILES, ACTIVE_CAMERA_PROFILE

    if args.profile and args.profile in CAMERA_PROFILES:
        # 1. Si el usuario pidió un perfil específico
        perfil = CAMERA_PROFILES[args.profile]
        fuente_resuelta = perfil["source"]
        backend_resuelto = perfil["backend"]
        print(f"✅ Usando perfil: {args.profile} ({perfil['descripcion']})")

    elif args.source is not None:
        # 2. Si el usuario pasó --source manualmente
        fuente_resuelta = args.source
        if args.backend:
            backend_resuelto = getattr(cv2, args.backend, cv2.CAP_DSHOW)
        else:
            # Si es URL, no usar backend. Si es número, usar DSHOW por defecto
            backend_resuelto = None if str(fuente_resuelta).startswith('http') else cv2.CAP_DSHOW

    else:
        # 3. Fallback al perfil activo por defecto en config_agente.py
        perfil = CAMERA_PROFILES.get(ACTIVE_CAMERA_PROFILE, CAMERA_PROFILES["webcam_default"])
        fuente_resuelta = perfil["source"]
        backend_resuelto = perfil["backend"]
        print(f"✅ Usando perfil por defecto: {ACTIVE_CAMERA_PROFILE}")

    # VALIDACIÓN DE SEGURIDAD: Nunca pasar None a OpenCV
    if fuente_resuelta is None:
        raise ValueError("❌ ERROR CRÍTICO: No se pudo determinar una fuente de video. Revisa config_agente.py o usa --profile.")

    # ==========================================
    # APERTURA DE LA CÁMARA
    # ==========================================
    print(f"🔌 Intentando abrir: {fuente_resuelta} (Backend: {backend_resuelto})")

    try:
        camera = CameraSource(fuente=fuente_resuelta, backend=backend_resuelto)
        cap = camera.cap
    except Exception as e:
        print(f"❌ Fallo al inicializar CameraSource: {e}", file=sys.stderr)
        sys.exit(1)

    ancho = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)) or 640
    alto = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)) or 480
    roi, troneras, detector_cfg = args.roi, _cargar_troneras(args.troneras), None
    if args.config:
        try:
            cfg = ConfigAgente.cargar(args.config).para_resolucion(ancho, alto)
            configurar_umbrales(**cfg.clasificador)
            detector_cfg = cfg.config_detector()
        except ValueError as e:  # ErrorConfig hereda de ValueError
            cap.release()
            print(f"❌ Configuración no utilizable: {e}", file=sys.stderr)
            return 2
        roi = roi or cfg.roi
        troneras = troneras or {n: tuple(p) for n, p in cfg.troneras.items()}
    if detector_cfg is None:
        detector_cfg = ConfigDetector(roi_mesa=roi).escalada(ancho)
    elif args.roi:
        detector_cfg = replace(detector_cfg, roi_mesa=args.roi)
    detector, tracker = BallDetector(detector_cfg), BallTracker()
    fps = cap.get(cv2.CAP_PROP_FPS) or 15.0
    analizador = ShotAnalyzer(troneras=troneras, fps=fps)
    cliente, reintentador = None, None
    registro = TrazaTiros(args.traza)
    if args.match_id:
        try:
            cliente = PoolHawkApiClient(args.api_url, token=args.token, cola=ColaEventos(args.cola))
        except ValueError as e:
            cap.release()
            print(f"❌ Configuración inválida: {e}", file=sys.stderr)
            return 2

        def anotar(event_id, estado, detalle):
            registro.transicion(event_id, estado, args.match_id, detalle)

        bloqueo = cliente.cola.bloqueo
        if bloqueo:
            print(f"⛔ Cola BLOQUEADA por el evento {bloqueo['eventId']}: {bloqueo['motivo']}. "
                  "No se enviarán tiros hasta --reanudar-cola o --descartar-evento.", file=sys.stderr)
        resumen = cliente.reenviar_pendientes(observador=anotar)
        if resumen["enviados"] or resumen["rechazados"] or resumen["pendientes"]:
            print(f"♻️  Reenvío de pendientes: {resumen}")
        reintentador = ReintentadorPendientes(cliente, observador=anotar)
        reintentador.iniciar()  # sigue reenviando en segundo plano mientras corre la cámara

    print("📷 Fuente abierta." + ("" if args.no_gui else " Presiona 'q' para salir."))
    procesados = 0
    cierre = None

    # Detectar si la fuente es un archivo de video (no una cámara en vivo ni stream)
    es_video_archivo = isinstance(args.source, str) and os.path.exists(args.source)

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

        # Si llegamos aquí porque el video terminó (no por 'q'), flush de eventos pendientes
        if es_video_archivo and cliente is not None:
            print("🎬 Fin del video alcanzado. Enviando eventos pendientes...")
            try:
                resumen = cliente.reenviar_pendientes(reintentos=1, observador=anotar if 'anotar' in locals() else None)
                if resumen["enviados"] or resumen["rechazados"] or resumen["pendientes"]:
                    print(f"📤 Eventos al finalizar video: {resumen}")
            except Exception as e:
                print(f"⚠️  No se pudieron enviar todos los eventos pendientes: {e}", file=sys.stderr)

    finally:
        if reintentador is not None:
            cierre = reintentador.detener()
        cap.release()
        if not args.no_gui:
            cv2.destroyAllWindows()

    print(f"Fotogramas procesados: {procesados}. Bolas seguidas: {len(tracker.tracks)}")

    if es_video_archivo:
        print("✅ Análisis de video completado.")
        return 0

    if cierre is not None and (not cierre["terminado"] or cierre["pendientes"]):
        if not cierre["terminado"]:
            print("⚠️  El hilo de reintentos seguía ocupado al cerrar: el último intento NO está "
                  "contabilizado.", file=sys.stderr)
        print(f"⚠️  Quedan {cierre['pendientes']} evento(s) pendientes en {args.cola}; se reenviarán "
              "al próximo arranque (o usa --descartar-evento / --reanudar-cola si la cola está bloqueada).",
              file=sys.stderr)
        return 4
    return 0


if __name__ == "__main__":
    sys.exit(main())
