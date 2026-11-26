import cv2
import sys

print("1. Intentando abrir cámara 0 con CAP_DSHOW...")
cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)

if not cap.isOpened():
    print("❌ FALLO: cv2.VideoCapture(0, CAP_DSHOW) no pudo abrirse.")
    sys.exit(1)

print("2. Cámara abierta. Intentando leer el primer frame...")
ret, frame = cap.read()

if not ret or frame is None:
    print("❌ FALLO: La cámara se abrió, pero no entrega imágenes (está bloqueada por otra app).")
    cap.release()
    sys.exit(1)

print(f"✅ ÉXITO TOTAL: Cámara funcionando. Resolución: {frame.shape[1]}x{frame.shape[0]}")
cap.release()