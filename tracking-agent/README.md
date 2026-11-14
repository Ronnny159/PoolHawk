# Agente de visión de PoolHawk

Pipeline: **fuente de vídeo → `BallDetector` → `clasificar_bola` → `BallTracker` → `ShotAnalyzer` → `PoolHawkApiClient`**.

| Módulo | Responsabilidad |
|---|---|
| `camera_source.py` | Abre webcam (índice), URL (DroidCam) o vídeo grabado |
| `ball_detector.py` | Localiza círculos (Hough) con parámetros configurables y ROI de mesa |
| `ball_classifier.py` | Color, número y **confianza** de cada bola (HSV) |
| `ball_tracker.py` | Identidad persistente; **asignación global óptima** (húngaro) con coste de distancia, radio y clasificación |
| `shot_analyzer.py` | Convierte seguimiento en un `TiroObservado`; el primer contacto ambiguo pasa a revisión |
| `cola_eventos.py` | Cola en disco de tiros pendientes (atómica y segura entre hilos) |
| `api_client.py` | Cliente REST idempotente + `ReintentadorPendientes` (reintento en segundo plano con espera incremental) |
| `traza.py` | Registro JSON Lines de cada tiro: enviado, pendiente, conflicto, en revisión… |
| `calibracion.py` | Calibrar troneras/ROI y **medir** detección y clasificación con grabaciones reales |
| `vision_agent.py` | Orquesta todo el pipeline |
| `simulador.py` | Simulador de eventos contra la API (independiente de la visión) |

> Todos los módulos usan nombres en **minúscula** (`snake_case`): Linux (CI y despliegue) distingue mayúsculas.

## Uso

```bash
pip install -r requirements.txt
python vision_agent.py --source 0                       # webcam con ventana
python vision_agent.py --source partida.mp4 --no-gui    # vídeo grabado, headless
python vision_agent.py --source 0 --roi 80,40,480,400   # limitar a la mesa
# enviar tiros al backend (requiere --troneras calibradas)
python vision_agent.py --match-id <ID> --api-url http://localhost:8080 --troneras troneras.json
```

Archivos que genera el agente (ya ignorados por Git): `.poolhawk_cola.json` (pendientes) y
`.poolhawk_traza.jsonl` (auditoría de cada tiro).

## Calibración (hazla antes de confiar en los resultados)

```bash
python calibracion.py roi         --source partida.mp4        # imprime el --roi
python calibracion.py troneras    --source partida.mp4        # 6 clics -> troneras.json
python calibracion.py diagnostico --source partida.mp4 --bolas-esperadas 16
python calibracion.py evaluar     --source partida.mp4 --esperado partida.esperado.json \
                                  --min-precision 0.9 --min-recall 0.9
```

`diagnostico` sugiere si hay falsos positivos/negativos o demasiadas bolas `DESCONOCIDA`.
`evaluar` mide precisión, recall y exactitud del número contra fotogramas anotados a mano
(formato en la cabecera de `calibracion.py`).

## Pruebas

```bash
pip install -r requirements-dev.txt
python -m pytest test -q
```

Para incluir grabaciones reales en las pruebas, define `POOLHAWK_VIDEOS` con una carpeta que
contenga `partida.mp4` y `partida.esperado.json`; sin ella esas pruebas se omiten.

## Cómo se decide si un tiro se envía

El agente **propone**; el backend valida reglas, turno y versión. Un tiro NO se envía y pasa a
revisión si: faltan troneras calibradas, la blanca no se observó en movimiento, una bola
desapareció lejos de toda tronera o sin número, o el **primer contacto es ambiguo** (varias
bolas empiezan a moverse dentro de la ventana de contacto, o la trayectoria de la blanca no
pasa cerca de la bola señalada). Cada decisión queda en la traza con su confianza.

Si la API cae, el tiro queda en la cola y `ReintentadorPendientes` lo reenvía con el mismo
`eventId` (espera 2 s, 4 s… hasta 60 s). Tras 50 fallos de red el evento pasa a la lista de
rechazados para revisión manual. Requiere el backend con `jugadorActualId` y `versionEsperada`.

## Limitaciones conocidas

- Los rangos HSV y umbrales por defecto **no están calibrados** con tu mesa; usa `calibracion.py`.
- La inferencia de "primera bola tocada" sigue siendo heurística; los casos ambiguos se marcan,
  pero con pocos fps (< 15) habrá más tiros en revisión.
- Si el `GET` inicial de la partida falla no hay forma segura de atribuir el tiro: queda para
  revisión manual y no se guarda en la cola.
