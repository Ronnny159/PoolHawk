"""
PoolHawk - Agente de simulacion de tracking.

Simula las lecturas de la camara: crea una partida de Bola 8 y envia
10 tiros a la API con una pausa de 2 segundos entre cada uno.

Uso:
    pip install -r requirements.txt
    python simulador.py
    python simulador.py --base-url http://localhost:8080
"""
import argparse
import random
import sys
import time

import requests

NUM_TIROS = 10
PAUSA_SEGUNDOS = 2
TRONERAS = [
    "TOP_LEFT", "TOP_RIGHT",
    "MIDDLE_LEFT", "MIDDLE_RIGHT",
    "BOTTOM_LEFT", "BOTTOM_RIGHT",
]


def crear_partida(base_url: str) -> dict:
    """POST /api/v1/matches -> devuelve la partida creada."""
    payload = {
        "modalidad": "BOLA_8",
        "equipoA": ["Ana"],
        "equipoB": ["Luis"],
    }
    r = requests.post(f"{base_url}/api/v1/matches", json=payload, timeout=10)
    r.raise_for_status()
    return r.json()


def consultar_partida(base_url: str, match_id: str) -> dict:
    """GET /api/v1/matches/{id} -> estado actual de la partida."""
    r = requests.get(f"{base_url}/api/v1/matches/{match_id}", timeout=10)
    r.raise_for_status()
    return r.json()


def ids_por_nombre(partida: dict) -> dict:
    """Mapa nombre -> id de jugador (la API da el turno por nombre)."""
    jugadores = partida["equipoA"] + partida["equipoB"]
    return {j["nombre"]: j["id"] for j in jugadores}


def generar_tiro(jugador_id: str, bolas_disponibles: list) -> dict:
    """Arma el cuerpo de un tiro simulado (como si lo leyera la camara).

    No se embolsa nunca la bola 8 para que la partida no termine antes
    de completar los 10 tiros.
    """
    embolsa = bolas_disponibles and random.random() < 0.5
    bolas = []
    troneras = []
    if embolsa:
        bola = random.choice(bolas_disponibles)
        bolas = [bola]
        troneras = [random.choice(TRONERAS)]
    return {
        "jugadorId": jugador_id,
        "primeraBolaTocada": random.choice(bolas_disponibles or [1]),
        "bolasEmbolsadas": bolas,
        "troneras": troneras,
        "duracionSegundos": random.randint(5, 30),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Simulador de camara PoolHawk")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--pausa", type=float, default=PAUSA_SEGUNDOS)
    args = parser.parse_args()
    base_url = args.base_url.rstrip("/")

    try:
        partida = crear_partida(base_url)
        match_id = partida["id"]
        ids = ids_por_nombre(partida)
        print(f"Partida creada: {match_id}")
        print(f"Jugadores: {', '.join(ids)}")

        # Bolas 1-15 sin la 8
        bolas_disponibles = [b for b in range(1, 16) if b != 8]

        for i in range(1, NUM_TIROS + 1):
            partida = consultar_partida(base_url, match_id)
            if partida.get("fase") != "EN_CURSO":
                print(f"La partida termino (fase: {partida.get('fase')}).")
                break

            nombre = partida["jugadorActual"]
            tiro = generar_tiro(ids[nombre], bolas_disponibles)

            r = requests.post(
                f"{base_url}/api/v1/matches/{match_id}/events",
                json=tiro,
                timeout=10,
            )
            if r.status_code >= 400:
                print(f"[{i}/{NUM_TIROS}] Error {r.status_code}: {r.text}")
            else:
                for b in tiro["bolasEmbolsadas"]:
                    bolas_disponibles.remove(b)
                res = r.json()
                print(
                    f"[{i}/{NUM_TIROS}] {nombre}: "
                    f"embolsadas={tiro['bolasEmbolsadas']} "
                    f"troneras={tiro['troneras']} -> {res.get('descripcion')}"
                )
            time.sleep(args.pausa)

        final = consultar_partida(base_url, match_id)
        print(f"Simulacion terminada. Puntajes: {final['puntajes']}")
        return 0
    except requests.RequestException as e:
        print(f"Error de conexion con la API: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
