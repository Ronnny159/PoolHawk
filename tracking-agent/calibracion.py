"""
Herramientas de calibración y evaluación del agente de visión.
Los rangos HSV, los parámetros de Hough, el ROI y las troneras dependen de la cámara, la
distancia y la iluminación REALES. Este módulo permite fijarlos y MEDIR su efecto con
grabaciones de tu mesa, en lugar de confiar en valores por defecto.
"""
from __future__ import annotations
import argparse
import json
import sys
from collections import Counter
from typing import Dict, Iterable, List, Optional, Sequence, Tuple
import cv2
import numpy as np
from ball_classifier import Observacion, clasificar_bola, recortar_roi, configurar_umbrales
from ball_detector import BallDetector, ConfigDetector, Deteccion
from ball_tracker import asignacion_optima
from camera_source import CameraSource, leer_frames
from config_agente import ErrorConfig, guardar_calibracion, ConfigAgente

ORDEN_TRONERAS = ["TOP_LEFT", "TOP_RIGHT", "MIDDLE_LEFT", "MIDDLE_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"]
Punto = Tuple[float, float]
Entrada = Tuple[Deteccion, Optional[Observacion]]

def validar_troneras(troneras: Dict[str, Punto], ancho: int, alto: int) -> List[str]:
    problemas = []
    for faltante in [n for n in ORDEN_TRONERAS if n not in troneras]:
        problemas.append(f"Falta la tronera {faltante}")
    for nombre, (x, y) in troneras.items():
        if nombre not in ORDEN_TRONERAS:
            problemas.append(f"Nombre de tronera desconocido: {nombre}")
        if not (0 <= x < ancho and 0 <= y < alto):
            problemas.append(f"{nombre} ({x:.0f},{y:.0f}) cae fuera del fotograma {ancho}x{alto}")
    nombres = list(troneras)
    for i, a in enumerate(nombres):
        for b in nombres[i + 1:]:
            if np.hypot(troneras[a][0] - troneras[b][0], troneras[a][1] - troneras[b][1]) < 20:
                problemas.append(f"{a} y {b} están a menos de 20 px: ¿clic repetido?")
    return problemas

def guardar_troneras(troneras: Dict[str, Punto], ruta: str) -> None:
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump({k: [float(v[0]), float(v[1])] for k, v in troneras.items()}, f, indent=2)

def elegir_troneras(frame: np.ndarray) -> Dict[str, Punto]:
    puntos: Dict[str, Punto] = {}
    def al_clic(evento, x, y, *_):
        if evento == cv2.EVENT_LBUTTONDOWN and len(puntos) < len(ORDEN_TRONERAS):
            puntos[ORDEN_TRONERAS[len(puntos)]] = (float(x), float(y))
    cv2.namedWindow("Troneras")
    cv2.setMouseCallback("Troneras", al_clic)
    while len(puntos) < len(ORDEN_TRONERAS):
        vista = frame.copy()
        for nombre, (x, y) in puntos.items():
            cv2.circle(vista, (int(x), int(y)), 8, (0, 255, 0), 2)
            cv2.putText(vista, nombre, (int(x) + 10, int(y)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
        siguiente = ORDEN_TRONERAS[len(puntos)]
        cv2.putText(vista, f"Clic en: {siguiente}  (ESC cancela)", (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
        cv2.imshow("Troneras", vista)
        if cv2.waitKey(30) == 27:
            cv2.destroyAllWindows()
            raise KeyboardInterrupt("Calibración cancelada")
    cv2.destroyAllWindows()
    return puntos

def diagnosticar(frames: Iterable[np.ndarray], detector: BallDetector, bolas_esperadas: Optional[int] = None) -> Dict[str, object]:
    cuentas, tipos, numeros, confianzas = [], Counter(), Counter(), []
    for frame in frames:
        dets = detector.detectar(frame)
        cuentas.append(len(dets))
        for d in dets:
            obs = clasificar_bola(recortar_roi(frame, d.x, d.y, d.r))
            tipos[obs.tipo] += 1
            confianzas.append(obs.confianza)
            if obs.numero is not None:
                numeros[obs.numero] += 1
    
    total = sum(tipos.values())
    media = float(np.mean(cuentas)) if cuentas else 0.0
    informe: Dict[str, object] = {
        "frames": len(cuentas),
        "detecciones_por_frame": {"media": round(media, 2), "min": min(cuentas, default=0), "max": max(cuentas, default=0)},
        "tipos": dict(tipos),
        "numeros": dict(sorted(numeros.items())),
        "confianza_media": round(float(np.mean(confianzas)), 3) if confianzas else 0.0,
        "pct_desconocidas": round(100 * tipos["DESCONOCIDA"] / total, 1) if total else 0.0,
    }
    sugerencias: List[str] = []
    if bolas_esperadas is not None and cuentas:
        if media > bolas_esperadas * 1.15:
            sugerencias.append("Más círculos que bolas: posibles FALSOS POSITIVOS. Sube param2, acota min/max_radius o define --roi sobre el paño.")
        elif media < bolas_esperadas * 0.85:
            sugerencias.append("Menos círculos que bolas: posibles FALSOS NEGATIVOS. Baja param2, revisa el rango de radios o la iluminación (reflejos).")
    if total and informe["pct_desconocidas"] > 30:
        sugerencias.append("Más del 30 % de bolas DESCONOCIDAS: recalibra los rangos HSV con fotos reales de cada bola bajo tu iluminación.")
    if not total:
        sugerencias.append("No se detectó ninguna bola: revisa la fuente, el --roi y los radios.")
    informe["sugerencias"] = sugerencias
    return informe

def evaluar_detecciones(detectado: Sequence[Sequence[Entrada]], esperado: Sequence[Sequence[dict]], tolerancia: float = 0.0) -> Dict[str, float]:
    tp = fp = fn = num_total = num_ok = 0
    for dets, reales in zip(detectado, esperado):
        costos = [[(np.hypot(d.x - r["x"], d.y - r["y"]) if np.hypot(d.x - r["x"], d.y - r["y"]) <= (tolerancia or d.r) else 1e9) for r in reales] for d, _ in dets]
        emparejados = [(i, j) for i, j in asignacion_optima(costos) if costos[i][j] < 1e9]
        tp += len(emparejados)
        fp += len(dets) - len(emparejados)
        fn += len(reales) - len(emparejados)
        for i, j in emparejados:
            if reales[j].get("numero") is not None:
                num_total += 1
                obs = dets[i][1]
                num_ok += int(obs is not None and obs.numero == reales[j]["numero"])
    return {
        "tp": tp, "fp": fp, "fn": fn,
        "precision": round(tp / (tp + fp), 3) if tp + fp else 0.0,
        "recall": round(tp / (tp + fn), 3) if tp + fn else 0.0,
        "exactitud_numero": round(num_ok / num_total, 3) if num_total else 0.0,
    }

def evaluar_fuente(fuente: str, ruta_esperado: str, config: ConfigDetector, tolerancia: float = 0.0) -> Dict[str, float]:
    with open(ruta_esperado, encoding="utf-8") as f:
        anotado = {int(k): v for k, v in json.load(f)["frames"].items()}
    camera = CameraSource(fuente)
    camera.abrir()
    cap = camera.cap
    ancho = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)) or 640
    detector = BallDetector(config.escalada(ancho))
    detectado, esperado = [], []
    try:
        for indice, frame in enumerate(leer_frames(cap, 0)):
            if indice in anotado:
                entradas = [(d, clasificar_bola(recortar_roi(frame, d.x, d.y, d.r))) for d in detector.detectar(frame)]
                detectado.append(entradas)
                esperado.append(anotado[indice])
            if indice >= max(anotado, default=0):
                break
    finally:
        camera.cerrar()
    return evaluar_detecciones(detectado, esperado, tolerancia)

def _frame_de(fuente: str, indice: int = 0) -> np.ndarray:
    camera = CameraSource(fuente)
    camera.abrir()
    cap = camera.cap
    try:
        for i, frame in enumerate(leer_frames(cap, 0)):
            if i >= indice:
                return frame
    finally:
        camera.cerrar()
    raise RuntimeError(f"La fuente tiene menos de {indice + 1} fotogramas")

def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description="Calibración y evaluación del agente de visión")
    sub = parser.add_subparsers(dest="cmd", required=True)
    for nombre in ("troneras", "roi", "diagnostico", "evaluar"):
        p = sub.add_parser(nombre)
        p.add_argument("--source", default="0", help="Cámara, vídeo o URL")
    sub.choices["troneras"].add_argument("--salida", default="troneras.json", help="JSON suelto (se ignora con --config)")
    sub.choices["troneras"].add_argument("--frame", type=int, default=0, help="Fotograma a usar")
    sub.choices["roi"].add_argument("--frame", type=int, default=0)
    for nombre in ("troneras", "roi"):
        sub.choices[nombre].add_argument("--config", default=None, help="configuracion.json donde guardar la calibración")
    
    d = sub.choices["diagnostico"]
    d.add_argument("--max-frames", type=int, default=300)
    d.add_argument("--bolas-esperadas", type=int, default=None)
    d.add_argument("--config", default=None, help="configuracion.json para usar sus parámetros de detector") # H-06
    
    e = sub.choices["evaluar"]
    e.add_argument("--esperado", required=True, help="JSON con las bolas reales por fotograma")
    e.add_argument("--min-precision", type=float, default=0.0)
    e.add_argument("--min-recall", type=float, default=0.0)
    
    args = parser.parse_args(argv)
    try:
        if args.cmd == "troneras":
            frame = _frame_de(args.source, args.frame)
            troneras = elegir_troneras(frame)
            problemas = validar_troneras(troneras, frame.shape[1], frame.shape[0])
            if problemas:
                print("❌ Calibración inválida:\n  - " + "\n  - ".join(problemas), file=sys.stderr)
                return 1
            if args.config:
                guardar_calibracion(args.config, (frame.shape[1], frame.shape[0]), troneras=troneras)
                print(f"✅ Troneras guardadas en {args.config} ({frame.shape[1]}x{frame.shape[0]})")
            else:
                guardar_troneras(troneras, args.salida)
                print(f"✅ Troneras guardadas en {args.salida}")
        elif args.cmd == "roi":
            frame = _frame_de(args.source, args.frame)
            x, y, w, h = cv2.selectROI("Selecciona el paño y pulsa ENTER", frame)
            cv2.destroyAllWindows()
            if w <= 0 or h <= 0:
                print("❌ No se seleccionó ninguna región", file=sys.stderr)
                return 1
            if args.config:
                guardar_calibracion(args.config, (frame.shape[1], frame.shape[0]), roi=(int(x), int(y), int(w), int(h)))
                print(f"✅ ROI guardado en {args.config} ({frame.shape[1]}x{frame.shape[0]})")
            else:
                print(f"--roi {x},{y},{w},{h}")
        elif args.cmd == "diagnostico":
            cap = CameraSource(args.source).abrir()
            ancho = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)) or 640
            alto = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT)) or 480
            try:
                # H-06: Usar configuración guardada si se proporciona
                if args.config:
                    cfg = ConfigAgente.cargar(args.config).para_resolucion(ancho, alto)
                    configurar_umbrales(**cfg.clasificador)
                    detector_cfg = cfg.config_detector()
                else:
                    detector_cfg = ConfigDetector().escalada(ancho)
                
                informe = diagnosticar(leer_frames(cap, args.max_frames), BallDetector(detector_cfg), args.bolas_esperadas)
            finally:
                cap.release()
            print(json.dumps(informe, indent=2, ensure_ascii=False))
        else:
            metricas = evaluar_fuente(args.source, args.esperado, ConfigDetector())
            print(json.dumps(metricas, indent=2))
            if metricas["precision"] < args.min_precision or metricas["recall"] < args.min_recall:
                print("❌ Por debajo de los mínimos exigidos", file=sys.stderr)
                return 1
    except (RuntimeError, KeyboardInterrupt, OSError, ValueError, KeyError, ErrorConfig) as exc:
        print(f"❌ {exc}", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    sys.exit(main())