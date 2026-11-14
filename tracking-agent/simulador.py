"""
PoolHawk - Agente de simulacion de tracking.
Simula las lecturas de la camara enviando tiros a la API.
"""
import argparse
import random
import sys
import time
import requests

NUM_TIROS = 10
PAUSA_SEGUNDOS = 2
ESPERA_REINTENTO = 5
MAX_REINTENTOS = 12
TRONERAS = [
    "TOP_LEFT", "TOP_RIGHT",
    "MIDDLE_LEFT", "MIDDLE_RIGHT",
    "BOTTOM_LEFT", "BOTTOM_RIGHT",
]

class ApiNoDisponible(Exception):
    pass

def pedir(metodo, url, espera, max_reintentos, **kwargs):
    for intento in range(1, max_reintentos + 1):
        try:
            r = requests.request(metodo, url, timeout=10, **kwargs)
            if r.status_code >= 500:
                raise requests.HTTPError(f"error {r.status_code}", response=r)
            return r
        except (requests.ConnectionError, requests.Timeout, requests.HTTPError) as e:
            if intento == max_reintentos:
                raise ApiNoDisponible(f"La API no responde tras {max_reintentos} intentos") from e
            print(f"  API no disponible. Reintento {intento}/{max_reintentos - 1} en {espera:g} s...")
            time.sleep(espera)

def crear_partida(base_url, espera, max_reintentos):
    # JSON CORREGIDO: Envía 'jugadores' y 'formato'
    payload = {
        "modalidad": "BOLA_8",
        "formato": "INDIVIDUAL",
        "jugadores": ["Ana", "Luis"],
        "anchoMesa": 254.0,
        "altoMesa": 127.0
    }
    r = pedir("POST", f"{base_url}/api/v1/matches", espera, max_reintentos, json=payload)
    r.raise_for_status()
    return r.json()

def consultar_partida(base_url, match_id, espera, max_reintentos):
    r = pedir("GET", f"{base_url}/api/v1/matches/{match_id}", espera, max_reintentos)
    r.raise_for_status()
    return r.json()

def ids_por_nombre(partida):
    jugadores = partida["equipoA"] + partida["equipoB"]
    return {j["nombre"]: j["id"] for j in jugadores}

def generar_tiro(jugador_id, bolas_disponibles):
    embolsa = bolas_disponibles and random.random() < 0.5
    bolas = []
    troneras = []
    if embolsa:
        bola = random.choice(bolas_disponibles)
        bolas = [bola]
        troneras = [random.choice(TRONERAS)]
    primera = random.choice(bolas_disponibles or [1])
    return {
        "jugadorId": jugador_id,
        "primeraBolaTocada": primera,
        "bolasEmbolsadas": bolas,
        "troneras": troneras,
        "duracionSegundos": random.randint(5, 30),
    }

def main():
    parser = argparse.ArgumentParser(description="Simulador de camara PoolHawk")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--pausa", type=float, default=PAUSA_SEGUNDOS)
    parser.add_argument("--match-id", help="Usar partida existente")
    args = parser.parse_args()
    
    base_url = args.base_url.rstrip("/")
    espera, intentos = ESPERA_REINTENTO, MAX_REINTENTOS

    try:
        if args.match_id:
            partida = consultar_partida(base_url, args.match_id, espera, intentos)
            print(f"Usando partida existente: {args.match_id}")
        else:
            partida = crear_partida(base_url, espera, intentos)
            print(f"Partida creada: {partida['id']}")
            
        match_id = partida["id"]
        ids = ids_por_nombre(partida)
        print(f"Jugadores: {', '.join(ids)}")

        bolas_disponibles = [b for b in range(1, 16) if b != 8]

        for i in range(1, NUM_TIROS + 1):
            partida = consultar_partida(base_url, match_id, espera, intentos)
            if partida.get("fase") != "EN_CURSO":
                print(f"La partida termino (fase: {partida.get('fase')}).")
                break

            nombre = partida["jugadorActual"]
            tiro = generar_tiro(ids[nombre], bolas_disponibles)

            r = pedir("POST", f"{base_url}/api/v1/matches/{match_id}/events", espera, intentos, json=tiro)
            
            if r.status_code >= 400:
                print(f"[{i}/{NUM_TIROS}] Error {r.status_code}: {r.text}")
            else:
                for b in tiro["bolasEmbolsadas"]:
                    if b in bolas_disponibles:
                        bolas_disponibles.remove(b)
                res = r.json()
                print(f"[{i}/{NUM_TIROS}] {nombre}: embolsadas={tiro['bolasEmbolsadas']} -> {res.get('descripcion')}")
            time.sleep(args.pausa)

        final = consultar_partida(base_url, match_id, espera, intentos)
        print(f"Simulacion terminada. Puntajes: {final['puntajes']}")
        return 0
    except ApiNoDisponible as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1
    except requests.RequestException as e:
        print(f"Error en la peticion a la API: {e}", file=sys.stderr)
        return 1

if __name__ == "__main__":
    sys.exit(main())