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
| `api_client.py` | Cliente REST idempotente + `ReintentadorPendientes` (segundo plano, espera incremental, cierre controlado) |
| `traza.py` | Registro JSON Lines de cada **transición** de un tiro (`OBSERVADO → PENDIENTE → REINTENTO → ENVIADO`…) con el mismo `eventId` |
| `config_agente.py` | `configuracion.json` único: ROI, troneras, detector y clasificador ligados a UNA resolución (se valida contra la fuente) |
| `calibracion.py` | Calibrar troneras/ROI (guarda en `configuracion.json`) y **medir** detección y clasificación con grabaciones reales |
| `vision_agent.py` | Orquesta todo el pipeline |
| `simulador.py` | Simulador de eventos contra la API (independiente de la visión) |

> Todos los módulos usan nombres en **minúscula** (`snake_case`): Linux (CI y despliegue) distingue mayúsculas.

## Uso

```bash
pip install -e ".[gui]"          # con ventana (cv2.imshow); o ".[headless]" en servidores/CI
python vision_agent.py --source 0                       # webcam con ventana
python vision_agent.py --source partida.mp4 --no-gui    # vídeo grabado, headless
python vision_agent.py --source 0 --roi 80,40,480,400   # limitar a la mesa
# enviar tiros al backend usando la calibración guardada (requiere troneras)
python vision_agent.py --match-id <ID> --api-url http://localhost:8080 --config configuracion.json
```

Códigos de salida: `0` todo contabilizado · `1` fuente no disponible · `2` configuración
inválida/incompatible con la fuente · `4` terminó con eventos pendientes o el reintentador no
cerró a tiempo (el agente lo avisa; no finge haber terminado bien).

Archivos que genera el agente (ya ignorados por Git): `.poolhawk_cola.json` (pendientes) y
`.poolhawk_traza.jsonl` (auditoría de cada tiro).

## Calibración (hazla antes de confiar en los resultados)

```bash
python calibracion.py roi         --source partida.mp4 --config configuracion.json
python calibracion.py troneras    --source partida.mp4 --config configuracion.json   # 6 clics
python calibracion.py diagnostico --source partida.mp4 --bolas-esperadas 16
python calibracion.py evaluar     --source partida.mp4 --esperado partida.esperado.json \
                                  --min-precision 0.9 --min-recall 0.9
```

El archivo guarda la resolución en la que calibraste. Si luego la fuente tiene la misma
proporción, el agente reescala ROI, troneras y radios; si cambió el encuadre, lo rechaza (código
2) y hay que recalibrar. Los umbrales HSV del clasificador y los parámetros del detector también
viven ahí (secciones `clasificador` y `detector`).

`diagnostico` sugiere si hay falsos positivos/negativos o demasiadas bolas `DESCONOCIDA`.
`evaluar` mide precisión, recall y exactitud del número contra fotogramas anotados a mano
(formato en la cabecera de `calibracion.py`).

## Pruebas

```bash
pip install -e ".[dev,headless]"
python -m pytest test -q          # pyproject.toml fija pythonpath; las pruebas no tocan sys.path
```

Para incluir grabaciones reales en las pruebas, define `POOLHAWK_VIDEOS` con una carpeta que
contenga `partida.mp4` y `partida.esperado.json`; sin ella esas pruebas se omiten.

## Cómo se decide si un tiro se envía

El agente **propone**; el backend valida reglas, turno y versión. Un tiro NO se envía y pasa a
revisión si: faltan troneras calibradas, la blanca no se observó en movimiento, una bola
desapareció lejos de toda tronera o sin número, o el **primer contacto es ambiguo** (varias
bolas empiezan a moverse dentro de la ventana de contacto, o la trayectoria de la blanca no
pasa cerca de la bola señalada). Cada decisión queda en la traza con su confianza.

### Qué pasa cuando falla la API

* **Falla el `GET` de la partida** (para fijar jugador y versión): no se inventa nada y el tiro
  **no** se encola. Queda en la traza como `ESTADO_NO_DISPONIBLE` para revisión manual.
* **Falla el `POST`**: el evento ya está en la cola con su payload fijado; `ReintentadorPendientes`
  lo reenvía con el mismo `eventId` (espera 2 s, 4 s… hasta 60 s). Cada intento y la confirmación
  final quedan en la traza (`REINTENTO`, `ENVIADO`).
* **Un evento agota 50 fallos de red**: la cola se **bloquea**. El evento no se descarta ni se
  envían los siguientes (llevarían una versión obsoleta) y los tiros nuevos no se encolan
  (`BLOQUEADO`). Reconcilia con el backend y entonces:

```bash
python vision_agent.py --reanudar-cola                      # reintenta el evento bloqueante
python vision_agent.py --descartar-evento <EVENT_ID>        # o descártalo (queda en rechazados)
```

Requiere el backend con `jugadorActualId` y `versionEsperada`.

## Limitaciones conocidas

- Los rangos HSV y umbrales por defecto **no están calibrados** con tu mesa; usa `calibracion.py`.
- La inferencia de "primera bola tocada" sigue siendo heurística; los casos ambiguos se marcan,
  pero con pocos fps (< 15) habrá más tiros en revisión.
- Si falla el `GET` inicial el tiro no se puede atribuir y queda solo en la traza para revisión manual.
- El agente son módulos sueltos con `pyproject.toml` (`pip install -e .` funciona); convertirlo en
  un paquete con subcarpeta y imports relativos queda como mejora futura.
