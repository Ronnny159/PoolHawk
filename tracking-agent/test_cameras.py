import cv2
import sys

print("🔍 Buscando cámaras disponibles en el sistema...\n")

camaras_encontradas = []

backends = [
    (cv2.CAP_DSHOW, "CAP_DSHOW (DirectShow - Windows)"),
    (cv2.CAP_MSMF, "CAP_MSMF (Media Foundation - Windows)"),
    (cv2.CAP_ANY, "CAP_ANY (Autodetección)")
]

for indice in range(5):
    for backend_code, backend_name in backends:
        cap = cv2.VideoCapture(indice, backend_code)
        if cap.isOpened():
            # CRÍTICO: Validar que realmente entrega frames, no solo isOpened()
            ret, frame = cap.read()
            if ret and frame is not None:
                width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                camaras_encontradas.append({
                    'indice': indice,
                    'backend': backend_name,
                    'backend_code': backend_code,
                    'resolucion': f"{width}x{height}"
                })
                print(f"✅ Cámara encontrada:")
                print(f"   Índice: {indice}")
                print(f"   Backend: {backend_name}")
                print(f"   Resolución: {width}x{height}")
                print()
                cap.release()
                break
            else:
                print(f"⚠️  Cámara índice {indice} con {backend_name}: isOpened()=True pero NO entrega frames")
                cap.release()
        else:
            cap.release()

if not camaras_encontradas:
    print("❌ No se encontraron cámaras disponibles.")
    print("\n💡 Posibles causas:")
    print("   1. La cámara no está conectada")
    print("   2. Otra aplicación está usando la cámara (Zoom, Teams, Discord)")
    print("   3. Falta instalar drivers de la cámara")
    print("   4. Si usas DroidCam, asegúrate de que la app esté corriendo")
    print("\n💡 Si usas DroidCam, prueba con la URL:")
    print("   python vision_agent.py --source 'http://TU_IP:4747/video'")
else:
    print("━" * 50)
    print(f"📷 Total: {len(camaras_encontradas)} cámara(s) encontradas")
    print("━" * 50)
    print("\n💡 Para usar la primera cámara encontrada:")
    primera = camaras_encontradas[0]
    backend_short = primera['backend'].split()[0]
    print(f"   python vision_agent.py --source {primera['indice']} --backend {backend_short}")