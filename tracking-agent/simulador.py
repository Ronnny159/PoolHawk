"""
PoolHawk - Agente de simulacion de tracking.

Simula las lecturas de la camara: crea una partida de Bola 8 (o usa una
existente) y envia 10 tiros a la API, con una pausa entre cada uno.

Si la API se cae (conexion rechazada, tiempo agotado o error 5xx), el
script espera unos segundos y reintenta en lugar de terminar con error.

Uso:
    python simulador.py
    python simulador.py --base-url http://localhost:8080
    python simulador.py --match-id <id de una partida existente>
    python simulador.py --blanca      # envia primeraBolaTocada = 0
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
    """La API no respondio despues de todos los reintentos."""


def pedir(metodo, url, espera, max_reintentos, **kwargs):
    """Hace una peticion HTTP con reintentos.

    Reintenta cuando la API no responde: conexion rechazada, tiempo
    agotado o error 5xx del servidor. Los errores 4xx NO se reintentan
    (son un problema de la peticion y repetirla no lo arregla).
    """
    for intento in range(1, max_reintentos + 1):
        try:
            r = requests.request(metodo, url, timeout=10, **kwargs)
            if r.status_code >= 500:
                raise requests.HTTPError(
                    f"error {r.status_code} del servidor", response=r
                )
            return r
        except (requests.ConnectionError, requests.Timeout,
                requests.HTTPError) as e:
            motivo = type(e).__name__
            if intento == max_reintentos:
                raise ApiNoDisponible(
                    f"La API no responde tras {max_reintentos} intentos ({motivo})"
                ) from e
            print(
                f"  API no disponible ({motivo}). "
                f"Reintento {intento}/{max_reintentos - 1} en {espera:g} s..."
            )
            time.sleep(espera)


def crear_partida(base_url, espera, max_reintentos):
    """POST /api/v1/matches -> devuelve la partida creada."""
    payload = {
        "modalidad": "BOLA_8",
        "equipoA": ["Ana"],
        "equipoB": ["Luis"],
    }
    r = pedir("POST", f"{base_url}/api/v1/matches", espera, max_reintentos,
              json=payload)
    r.raise_for_status()
    return r.json()


def consultar_partida(base_url, match_id, espera, max_reintentos):
    """GET /api/v1/matches/{id} -> estado actual de la partida."""
    r = pedir("GET", f"{base_url}/api/v1/matches/{match_id}", espera,
              max_reintentos)
    r.raise_for_status()
    return r.json()


def ids_por_nombre(partida):
    """Mapa nombre -> id de jugador (la API da el turno por nombre)."""
    jugadores = partida["equipoA"] + partida["equipoB"]
    return {j["nombre"]: j["id"] for j in jugadores}


def generar_tiro(jugador_id, bolas_disponibles, blanca):
    """Arma el cuerpo de un tiro simulado (como si lo leyera la camara).

    Por defecto la primera bola tocada es una bola numerada valida. Con
    blanca=True se envia 0 (la blanca): el motor de Bola 8 lo trata como
    primer contacto ilegal, asi que ese tiro sera falta.

    No se embolsa nunca la bola 8 para que la partida no termine antes
    de completar los tiros.
    """
    embolsa = bolas_disponibles and random.random() < 0.5
    bolas = []
    troneras = []
    if embolsa:
        bola = random.choice(bolas_disponibles)
        bolas = [bola]
        troneras = [random.choice(TRONERAS)]
    primera = 0 if blanca else random.choice(bolas_disponibles or [1])
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
    parser.add_argument("--pausa", type=float, default=PAUSA_SEGUNDOS,
                        help="segundos entre tiros")
    parser.add_argument("--espera", type=float, default=ESPERA_REINTENTO,
                        help="segundos de espera entre reintentos")
    parser.add_argument("--reintentos", type=int, default=MAX_REINTENTOS,
                        help="intentos maximos por peticion")
    parser.add_argument("--blanca", action="store_true",
                        help="enviar primeraBolaTocada = 0 (la blanca)")
    parser.add_argument(
        "--match-id",
        help="Usar una partida existente en lugar de crear una nueva",
    )
    args = parser.parse_args()
    base_url = args.base_url.rstrip("/")
    espera, intentos = args.espera, args.reintentos

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

        # Bolas 1-15 sin la 8
        bolas_disponibles = [b for b in range(1, 16) if b != 8]

        for i in range(1, NUM_TIROS + 1):
            partida = consultar_partida(base_url, match_id, espera, intentos)
            if partida.get("fase") != "EN_CURSO":
                print(f"La partida termino (fase: {partida.get('fase')}).")
                break

            nombre = partida["jugadorActual"]
            tiro = generar_tiro(ids[nombre], bolas_disponibles, args.blanca)

            r = pedir("POST", f"{base_url}/api/v1/matches/{match_id}/events",
                      espera, intentos, json=tiro)
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
