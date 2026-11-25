#!/usr/bin/env python3
"""
Auditoría completa de cámara para Windows y OpenCV.
Diagnostica dispositivos, bloqueos de apps, permisos, red DroidCam y matriz OpenCV.
Compatible con Python 3.9+ y opencv-python.
"""

import cv2
import subprocess
import socket
import urllib.request
import urllib.error
import winreg
import os
import sys
import re

# ------------------------------------------------------------
# Helpers de salida con emojis
# ------------------------------------------------------------
OK = "✅"
FAIL = "❌"
WARN = "⚠️"
INFO = "ℹ️"


def seccion(titulo: str) -> None:
    print(f"\n{'=' * 60}")
    print(f"  {titulo}")
    print(f"{'=' * 60}")


def linea(estado: str, texto: str) -> None:
    print(f"   {estado} {texto}")


# ------------------------------------------------------------
# 1. INVENTARIO DE DISPOSITIVOS (PowerShell / WMI)
# ------------------------------------------------------------
def inventario_dispositivos() -> None:
    seccion("1. INVENTARIO DE DISPOSITIVOS DE CÁMARA")
    try:
        cmd = (
            "powershell -NoProfile -ExecutionPolicy Bypass "
            "-Command \"Get-PnpDevice -Class Camera | Select-Object FriendlyName, Status | Format-Table -AutoSize\""
        )
        resultado = subprocess.run(
            cmd, shell=True, capture_output=True, text=True, timeout=30
        )
        salida = resultado.stdout.strip()
        if salida:
            for l in salida.splitlines():
                l = l.strip()
                if l:
                    linea(OK, l)
        else:
            linea(WARN, "No se encontraron dispositivos de clase 'Camera' vía PowerShell.")
    except Exception as e:
        linea(FAIL, f"Error ejecutando PowerShell: {e}")

    # Dispositivos de sonido/video (Camo, DroidCam, OBS)
    try:
        cmd2 = (
            "powershell -NoProfile -ExecutionPolicy Bypass "
            "-Command \"Get-PnpDevice | Where-Object { $_.FriendlyName -match 'Camo|DroidCam|OBS|Webcam|Camera' } | Select-Object Name, Class, Status | Format-Table -AutoSize\""
        )
        resultado2 = subprocess.run(
            cmd2, shell=True, capture_output=True, text=True, timeout=30
        )
        salida2 = resultado2.stdout.strip()
        if salida2:
            print("   Dispositivos relacionados con video detectados:")
            for l in salida2.splitlines():
                l = l.strip()
                if l:
                    linea(INFO, l)
        else:
            linea(WARN, "No se detectaron dispositivos de video virtuales (Camo/DroidCam/OBS).")
    except Exception as e:
        linea(FAIL, f"Error listando dispositivos video: {e}")


# ------------------------------------------------------------
# 2. DETECCIÓN DE BLOQUEOS Y PERMISOS
# ------------------------------------------------------------
def procesos_que_usar_camara() -> list[str]:
    conocidos = [
        "camo.exe", "droidcam.exe", "msteams.exe", "zoom.exe",
        "obs64.exe", "obs32.exe", "skype.exe", "discord.exe",
        "camera.exe", "webcam.exe"
    ]
    encontrados = []
    try:
        resultado = subprocess.run(
            ["tasklist", "/FO", "CSV", "/NH"],
            capture_output=True, text=True, timeout=30
        )
        for linea_csv in resultado.stdout.splitlines():
            nombre = linea_csv.split(",")[0].strip().replace('"', "").lower()
            if any(c in nombre for c in conocidos):
                encontrados.append(nombre)
    except Exception as e:
        print(f"   {FAIL} Error ejecutando tasklist: {e}")
    return encontrados


def permiso_registro_camara() -> str:
    """Lee HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\CapabilityAccessManager\\ConsentStore\\webcam\\NonPackaged"""
    try:
        clave = winreg.OpenKey(
            winreg.HKEY_CURRENT_USER,
            r"Software\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\webcam\NonPackaged"
        )
        valor, _ = winreg.QueryValueEx(clave, "Value")
        winreg.CloseKey(clave)
        return valor
    except FileNotFoundError:
        return "NO_ENCONTRADO"
    except PermissionError:
        return "SIN_PERMISO"
    except Exception as e:
        return f"ERROR: {e}"


def bloqueos_y_permisos() -> None:
    seccion("2. DETECCIÓN DE BLOQUEOS Y PERMISOS")

    apps = procesos_que_usar_camara()
    if apps:
        for app in apps:
            linea(FAIL, f"Aplicación en ejecución que puede bloquear la cámara: {app}")
        linea(WARN, "Cierra estas aplicaciones antes de usar el agente de visión.")
    else:
        linea(OK, "No se detectaron aplicaciones conocidas usando la cámara.")

    permiso = permiso_registro_camara()
    if permiso == "Allow":
        linea(OK, "Permiso de cámara en el registro: Allow (habilitado).")
    elif permiso == "NO_ENCONTRADO":
        linea(WARN, "Clave de registro de permisos de cámara no encontrada (valor por defecto).")
    elif permiso == "SIN_PERMISO":
        linea(FAIL, "Sin permiso de lectura en el registro de cámara.")
    else:
        linea(FAIL, f"Estado del permiso de cámara: {permiso}")

    linea(INFO, "Para habilitar: Configuración > Privacidad y seguridad > Cámara > "
                "'Permitir que las aplicaciones de escritorio accedan a la cámara'")


# ------------------------------------------------------------
# 3. PRUEBA DE RED (DroidCam)
# ------------------------------------------------------------
def prueba_red_droidcam(ip: str = "192.168.40.89", puerto: int = 4747) -> None:
    seccion("3. PRUEBA DE RED (DroidCam)")
    url = f"http://{ip}:{puerto}/video"
    try:
        req = urllib.request.Request(url, method="HEAD")
        with urllib.request.urlopen(req, timeout=5) as resp:
            codigo = resp.getcode()
            if 200 <= codigo < 300:
                linea(OK, f"URL accesible: {url} (HTTP {codigo})")
            else:
                linea(WARN, f"URL respondió con código: {codigo}")
    except socket.timeout:
        linea(FAIL, f"Tiempo de espera agotado conectando a {url}. Verifica IP y firewall.")
    except urllib.error.URLError as e:
        linea(FAIL, f"No se puede conectar a {url}: {e}")
    except ConnectionRefusedError:
        linea(FAIL, f"Conexión rechazada en {url}. ¿Está DroidCam abierto en el teléfono?")
    except Exception as e:
        linea(FAIL, f"Error inesperado probando {url}: {e}")


# ------------------------------------------------------------
# 4. MATRIZ DE PRUEBA OPENCV
# ------------------------------------------------------------
def matriz_opencv() -> list[dict]:
    seccion("4. MATRIZ DE PRUEBA OPENCV")
    resultados = []
    backends = [
        (cv2.CAP_ANY, "CAP_ANY"),
        (cv2.CAP_DSHOW, "CAP_DSHOW"),
        (cv2.CAP_MSMF, "CAP_MSMF"),
    ]

    for indice in range(5):
        for backend_code, backend_nombre in backends:
            try:
                cap = cv2.VideoCapture(indice, backend_code)
                if cap.isOpened():
                    ancho = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                    alto = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                    cap.release()
                    entrada = {
                        "indice": indice,
                        "backend": backend_nombre,
                        "resolucion": f"{ancho}x{alto}",
                    }
                    resultados.append(entrada)
                    linea(
                        OK,
                        f"Cámara índice {indice} con backend {backend_nombre} -> {ancho}x{alto}",
                    )
                    break  # Si funciona con un backend, no probar otros para este índice
                else:
                    cap.release()
            except Exception as e:
                linea(FAIL, f"Índice {indice} / {backend_nombre}: error - {e}")

    if not resultados:
        print("   No se pudo abrir ninguna cámara con ninguno de los backends probados.")

    return resultados


# ------------------------------------------------------------
# 5. RESUMEN Y RECOMENDACIONES
# ------------------------------------------------------------
def resumen_recomendaciones(resultados: list[dict]) -> None:
    seccion("5. RESUMEN Y RECOMENDACIONES")

    if resultados:
        print(f"\n  Cámaras detectadas: {len(resultados)}")
        for r in resultados:
            print(
                f"   {OK} Índice {r['indice']} | {r['backend']} | {r['resolucion']}"
            )
        mejor = resultados[0]
        print(f"\n  💡 Comando sugerido:")
        print(
            f"      python vision_agent.py --source {mejor['indice']} "
            f"--backend {mejor['backend']}"
        )
    else:
        print("\n  No se detectó ninguna cámara local.")
        print(f"   {INFO} Prueba con DroidCam URL:")
        print(f"       python vision_agent.py --source 'http://192.168.40.89:4747/video'")

    print(f"\n  {WARN} Si ninguna cámara aparece:")
    print(f"      1. Cierra Zoom, Teams, Discord, OBS, Camo Studio.")
    print(f"      2. Abre la app 'Cámara' de Windows y ciérrala inmediatamente.")
    print(f"      3. Verifica Configuración > Privacidad > Cámara (apps de escritorio).")
    print(f"      4. Reinicia tu PC si el problema persiste.")


# ------------------------------------------------------------
# MAIN
# ------------------------------------------------------------
def main() -> None:
    print(f"\n{INFO}  AUDITORÍA DE CÁMARA - PoolHawk Vision Agent")
    print(f"{INFO}  Python {sys.version.split()[0]} | OpenCV {cv2.__version__}")

    inventario_dispositivos()
    bloqueos_y_permisos()
    prueba_red_droidcam()
    resultados = matriz_opencv()
    resumen_recomendaciones(resultados)

    print(f"\n{'=' * 60}")
    print("  Fin de la auditoría.")
    print(f"{'=' * 60}\n")


if __name__ == "__main__":
    main()
