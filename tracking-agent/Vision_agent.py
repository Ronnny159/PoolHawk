"""
PoolHawk - Agente de Visión Computarizada.
Detecta bolas de billar, clasifica su color y franja (lisa/rayada)
y deduce el número de bola basándose en el estándar.
"""
import cv2
import numpy as np
import requests
import argparse
import sys

# Mapeo estándar de colores a números (Bola Lisa, Bola Rayada)
COLORES_BILLAR = {
    'AMARILLO': (1, 9),
    'AZUL': (2, 10),
    'ROJO': (3, 11),
    'MORADO': (4, 12),
    'NARANJA': (5, 13),
    'VERDE': (6, 14),
    'VINO': (7, 15),
    'NEGRO': (8, 8), # La 8 no tiene rayada
    'BLANCO': (0, 0) # La blanca
}

# Rangos HSV aproximados para cada color
# (Ajustar según la iluminación de tu cuarto)
RANGOS_HSV = {
    'ROJO': [(np.array([0, 100, 100]), np.array([10, 255, 255])), 
             (np.array([160, 100, 100]), np.array([180, 255, 255]))],
    'AZUL': [(np.array([100, 100, 50]), np.array([130, 255, 255]))],
    'AMARILLO': [(np.array([20, 100, 100]), np.array([35, 255, 255]))],
    'VERDE': [(np.array([40, 50, 50]), np.array([80, 255, 255]))],
    'NARANJA': [(np.array([10, 100, 100]), np.array([20, 255, 255]))],
    'MORADO': [(np.array([130, 50, 50]), np.array([160, 255, 255]))],
    'VINO': [(np.array([0, 50, 20]), np.array([10, 150, 100]))],
}

def clasificar_bola(roi_bola):
    """Analiza el recorte de una bola y devuelve su número tentativo."""
    if roi_bola.size == 0:
        return -1, "DESCONOCIDA"

    # Convertir a HSV
    hsv = cv2.cvtColor(roi_bola, cv2.COLOR_BGR2HSV)
    
    # 1. Detectar si es RAYADA (mucho blanco en los bordes)
    lower_white = np.array([0, 0, 180])
    upper_white = np.array([180, 50, 255])
    mask_white = cv2.inRange(hsv, lower_white, upper_white)
    ratio_blanco = cv2.countNonZero(mask_white) / roi_bola.size
    
    es_rayada = ratio_blanco > 0.15

    # 2. Detectar el color dominante
    color_detectado = None
    max_pixeles = 0

    for color, rangos in RANGOS_HSV.items():
        mask = np.zeros(hsv.shape[:2], dtype=np.uint8)
        for (lower, upper) in rangos:
            mask = cv2.bitwise_or(mask, cv2.inRange(hsv, lower, upper))
        
        pixeles = cv2.countNonZero(mask)
        if pixeles > max_pixeles:
            max_pixeles = pixeles
            color_detectado = color

    # 3. Determinar el número final
    if ratio_blanco > 0.6:
        return 0, "BLANCO"
    
    if color_detectado is None:
        # Si es muy negro, probablemente es la 8
        if np.mean(hsv[:, :, 2]) < 50:
            return 8, "NEGRO"
        return -1, "DESCONOCIDA"

    # Asignar número según color y si es rayada
    lisa, rayada = COLORES_BILLAR[color_detectado]
    if color_detectado == 'NEGRO':
        return 8, "NEGRO"
    
    return (rayada if es_rayada else lisa), f"{color_detectado} {'RAYADA' if es_rayada else 'LISA'}"

def main():
    parser = argparse.ArgumentParser(description="Agente de Visión PoolHawk")
    parser.add_argument("--match-id", required=True, help="ID de la partida")
    args = parser.parse_args()
    
    # Iniciar cámara (0 para DroidCam/Webcam)
    cap = cv2.VideoCapture(0)
    if not cap.isOpened():
        print("❌ No se pudo abrir la cámara.")
        sys.exit(1)

    print("📷 Cámara iniciada. Presiona 'q' para salir.")
    print("Apunta la cámara a las bolas para ver qué número detecta el agente.")

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # Detección de círculos
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        gray_blur = cv2.medianBlur(gray, 5)
        
        circulos = cv2.HoughCircles(
            gray_blur, cv2.HOUGH_GRADIENT, dp=1.2, minDist=30,
            param1=50, param2=30, minRadius=10, maxRadius=50
        )

        if circulos is not None:
            circulos = np.round(circulos[0, :]).astype("int")
            for (x, y, r) in circulos:
                # Recortar el centro de la bola para evitar el borde
                r_int = int(r * 0.7)
                roi = frame[y-r_int:y+r_int, x-r_int:x+r_int]
                
                # Clasificar
                numero, descripcion = clasificar_bola(roi)

                # Dibujar en pantalla
                if numero != -1:
                    cv2.circle(frame, (x, y), r, (0, 255, 0), 2)
                    texto = f"#{numero} {descripcion}"
                    cv2.putText(frame, texto, (x - 30, y - r - 10), 
                                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
                else:
                    cv2.circle(frame, (x, y), r, (0, 0, 255), 2)

        cv2.imshow("PoolHawk Vision Agent", frame)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    main()