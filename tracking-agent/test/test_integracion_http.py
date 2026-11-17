"""Integración real por HTTP (servidor local falso del backend): agente -> API -> respuesta.

No sustituye una prueba contra el backend Spring real, pero ejercita sockets, JSON y
cabeceras de verdad en lugar de una sesión simulada.
"""
import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

import pytest

import traza
import vision_agent
from api_client import PoolHawkApiClient, ReintentadorPendientes
from cola_eventos import ColaEventos
from shot_analyzer import TiroObservado
from traza import TrazaTiros


class BackendFalso:
    """Imita GET /matches/{id} y POST /matches/{id}/events con idempotencia por eventId."""

    def __init__(self):
        self.posts, self.fallos_pendientes, self.vistos = [], 0, {}
        estado = self

        class H(BaseHTTPRequestHandler):
            def log_message(self, *a):
                pass

            def _responder(self, codigo, cuerpo):
                datos = json.dumps(cuerpo).encode()
                self.send_response(codigo)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(datos)))
                self.end_headers()
                self.wfile.write(datos)

            def do_GET(self):
                self._responder(200, {"id": "m1", "fase": "EN_CURSO", "jugadorActualId": "j2",
                                      "jugadorActual": "Luis", "cantidadTiros": len(estado.vistos)})

            def do_POST(self):
                cuerpo = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
                estado.posts.append((cuerpo, self.headers.get("Authorization")))
                if estado.fallos_pendientes > 0:
                    estado.fallos_pendientes -= 1
                    return self._responder(503, {"error": "no disponible"})
                if cuerpo["eventId"] in estado.vistos:           # reintento idempotente
                    return self._responder(200, estado.vistos[cuerpo["eventId"]])
                if cuerpo.get("versionEsperada") != len(estado.vistos):
                    return self._responder(409, {"error": "estado cambió"})
                estado.vistos[cuerpo["eventId"]] = {"esFalta": False, "puntos": 1}
                self._responder(200, estado.vistos[cuerpo["eventId"]])

        self.servidor = HTTPServer(("127.0.0.1", 0), H)
        self.url = f"http://127.0.0.1:{self.servidor.server_port}"
        threading.Thread(target=self.servidor.serve_forever, daemon=True).start()

    def cerrar(self):
        self.servidor.shutdown()
        self.servidor.server_close()


@pytest.fixture
def backend():
    b = BackendFalso()
    yield b
    b.cerrar()


def tiro(event_id="evt-1"):
    return TiroObservado(2, [5], ["TOP_LEFT"], 3, False, [], event_id)


def test_tiro_llega_con_jugador_version_y_token(backend, tmp_path):
    cliente = PoolHawkApiClient(backend.url, token="secreto", espera=0)
    estado = vision_agent.manejar_tiro(tiro(), cliente, "m1", TrazaTiros(str(tmp_path / "t.jsonl")))
    assert estado == traza.ENVIADO
    cuerpo, auth = backend.posts[0]
    assert cuerpo["jugadorId"] == "j2" and cuerpo["versionEsperada"] == 0
    assert cuerpo["eventId"] == "evt-1" and auth == "Bearer secreto"


def test_503_transitorio_se_reintenta_con_el_mismo_evento_sin_duplicar(backend):
    backend.fallos_pendientes = 2
    cliente = PoolHawkApiClient(backend.url, espera=0)
    cliente.registrar_tiro("m1", tiro())
    assert len(backend.posts) == 3 and len({p[0]["eventId"] for p in backend.posts}) == 1
    assert len(backend.vistos) == 1


def test_estado_cambiado_devuelve_409_y_queda_trazado(backend, tmp_path):
    cliente = PoolHawkApiClient(backend.url, espera=0)
    backend.vistos["otro"] = {}  # otro cliente registró un tiro entre la consulta y el envío
    original = cliente.obtener_estado

    def estado_viejo(m):
        e = original(m)
        backend.vistos["otro2"] = {}
        return e

    cliente.obtener_estado = estado_viejo
    estado = vision_agent.manejar_tiro(tiro(), cliente, "m1", TrazaTiros(str(tmp_path / "t.jsonl")))
    assert estado == traza.CONFLICTO
    assert cliente.cola.pendientes() == [] and len(cliente.cola.rechazados()) == 1


def test_backend_caido_y_recuperado_el_reintentador_entrega_el_tiro(backend):
    backend.fallos_pendientes = 10**6  # "caído"
    cola = ColaEventos()
    cliente = PoolHawkApiClient(backend.url, espera=0, max_reintentos=1, cola=cola)
    assert vision_agent.manejar_tiro(tiro(), cliente, "m1") == traza.PENDIENTE
    r = ReintentadorPendientes(cliente, base=0.01, maximo=0.02, sondeo=0.01)
    r.iniciar()
    try:
        backend.fallos_pendientes = 0  # vuelve el backend
        for _ in range(200):
            if not cola.pendientes():
                break
            threading.Event().wait(0.02)
    finally:
        r.detener()
    assert cola.pendientes() == [] and "evt-1" in backend.vistos
