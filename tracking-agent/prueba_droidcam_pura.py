import cv2

url = "http://192.168.40.89:4747/video"
print(f"Probando conexión directa a: {url}")

cap = cv2.VideoCapture(url)  # Sin forzar NINGÚN backend

if cap.isOpened():
    print("🎉 ¡ÉXITO TOTAL! OpenCV puede leer tu DroidCam por URL.")
    cap.release()
else:
    print("💀 FALLÓ. La IP es incorrecta, DroidCam está cerrado en el celular, o el Firewall de Windows bloquea el puerto 4747.")
