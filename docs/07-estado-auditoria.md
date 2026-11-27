# Estado de auditoría técnica y plan de estabilización

> **Referencia:** rama `feature/vision-tracking-agent`, commit `43ae3cba25f5dcdead273982cb1f810df0222ba8`.
>
> **Tipo de revisión:** inspección estática del código y comparación de ramas. No se ejecutaron pruebas durante esta auditoría. Los hallazgos deben revalidarse sobre el HEAD actual antes de cerrar tareas.

## 1. Estado de integración en el corte

En la comparación de este commit contra `develop` se registró:
- 59 commits por delante y 31 por detrás.
- 107 archivos modificados: 62 añadidos, 43 modificados y 2 eliminados.
- Diferencia de líneas: +8.692 / -540.
- Estado de ramas: divergente.

Estas cifras describen exclusivamente el corte indicado y no deben reutilizarse como estadísticas actuales sin volver a comparar las ramas.

## 2. Hallazgos registrados

| ID | Hallazgo | Consecuencia / riesgo | Prioridad |
|---|---|---|---|
| PH-01 | El flujo que genera el comando del agente no entrega credenciales para descargar un video protegido. | OpenCV puede no acceder al archivo servido por un endpoint autenticado. | P0 |
| PH-02 | El nombre físico combina UUID y nombre original, pero la expresión regular del endpoint admite un conjunto más limitado de caracteres. | Nombres habituales pueden ser rechazados al descargar. | P0 |
| PH-03 | La conexión de cámara en `VideoAnalysisConfig.vue` se simula con una espera y un cambio de estado. | La interfaz puede mostrar conexión sin probar cámara ni agente. | P1 |
| PH-04 | En el corte auditado, `calibracion.py` invocaba `abrir()`/`cerrar()` en `CameraSource`, cuya API expone `read()`/`release()` y context manager. | La calibración puede fallar por métodos inexistentes. | P1 |
| PH-05 | El JWT normal no incorpora `matchId`, mientras que el interceptor intenta extraerlo para agrupar sesiones; el listener publica por ID de partida. | Las sesiones pueden quedar en grupo global y no recibir el evento de su partida. | P0 |
| PH-06 | La política HTTP del cliente trata respuestas inferiores a 500 como respuestas no reintentables. | Códigos recuperables como 408/429 pueden acabar rechazando eventos pendientes. | P1 |
| PH-07 | Docker Compose define volumen persistente para PostgreSQL, pero no para `uploads/videos`. | Los videos podrían perderse al recrear el contenedor backend. | P0 |
| PH-08 | La ausencia prolongada de detecciones se interpreta como bolas embolsadas u ocultas. | Una oclusión, fallo de cámara o problema de iluminación puede producir una inferencia incorrecta. | P1 |
| PH-09 | En el método `_kalman_predict` revisado, se calculaban predicción y covarianza sin asignarlas al estado interno. | Las predicciones posteriores pueden partir de estado obsoleto. | P1 |
| PH-10 | La validación de configuración geométrica no cubría de forma completa ROI, dimensiones y coordenadas de troneras. | Configuraciones incoherentes pueden llegar al procesamiento. | P1 |
| PH-11 | El frontend construía la URL del backend usando `http://localhost:8080`. | En un despliegue remoto, localhost apunta al equipo cliente y no al servidor. | P2 |

## 3. Mejoras positivas observadas

- Separación de responsabilidades del servicio de partidas mediante componentes como `MatchFactory` y `MatchPersistenceMapper`.
- Persistencia relacional con PostgreSQL y entidades JPA.
- Uso de `@Version` en `PartidaEntity` para control optimista.
- Carga del estado desde almacenamiento antes de procesar un tiro.
- Idempotencia mediante `eventId`, incluyendo rechazo cuando se reutiliza con payload diferente.
- Validación de versión esperada y jugador del turno.
- Publicación de actualizaciones después del commit.
- Caché de lectura con TTL de 30 minutos y limpieza programada.
- Interceptor de handshake WebSocket que valida JWT y rechaza tokens inválidos o ausentes.
- Agente dividido en módulos de captura, detección, clasificación, seguimiento, análisis, API, cola y trazabilidad.
- Cola local para conservar eventos pendientes y mecanismos de reintento/bloqueo ante conflictos.

## 4. Plan de corrección recomendado

### P0 — Bloqueos de integración
1. Resolver acceso autenticado del agente a videos con credencial temporal o mecanismo de servicio.
2. Separar el nombre físico del archivo del nombre original; validar y normalizar la extensión.
3. Añadir volumen persistente para `uploads/videos` y política de retención.
4. Rediseñar la asociación de sesiones WebSocket mediante suscripción autorizada a una partida; no depender de un `matchId` ausente del JWT.
5. Probar aislamiento entre dos partidas concurrentes.

### P1 — Correctitud del agente
1. Alinear las llamadas de calibración con la API real de `CameraSource`.
2. Reemplazar la conexión simulada de cámara por una comprobación real a través de un agente/servicio local.
3. Definir clasificación HTTP explícita: rechazos definitivos, autenticación, conflictos, límites de tasa y errores recuperables.
4. Distinguir desaparición, oclusión, pérdida de señal y embolsamiento confirmado.
5. Actualizar de forma coherente el estado y covarianza de Kalman durante fotogramas perdidos.
6. Validar dimensiones, ROI y coordenadas de troneras al cargar y guardar configuración.

### P2 — Portabilidad y documentación
1. Centralizar la URL base del backend mediante configuración de entorno.
2. Mantener el MER y diccionario de datos sincronizados con entidades y migraciones reales.
3. Completar diagramas y trazabilidad de clases/métodos a partir del código vigente.

## 5. Matriz mínima de validación

| Área | Validación requerida | Criterio de aceptación |
|---|---|---|
| Backend | `mvn clean test` desde el proyecto Maven | Compilación y pruebas aprobadas. |
| Frontend | `npm install` y `npm run build` | Build de producción exitoso. |
| Agente | `pytest` desde `tracking-agent` con entorno virtual | Suite aprobada. |
| Docker | `docker compose up -d` y `docker compose ps` | Servicios saludables y comunicados. |
| Video | Cargar, descargar y abrir video protegido con OpenCV | Acceso autorizado reproducible. |
| Archivos | Recrear contenedor backend | Videos persisten en volumen. |
| WebSocket | Dos partidas activas y usuarios distintos | Cada cliente recibe solo eventos autorizados de su partida. |
| Calibración | Cargar/guardar ROI y troneras válidas e inválidas | Configuración inválida rechazada; válida reproducible. |
| Seguimiento | Oclusiones y pérdidas temporales | Identidad estable y sin falsos embolsamientos. |
| Reintentos | Simular 408, 429, 409 y 5xx | Reintentos y bloqueos según política documentada. |
| Partidas | Eventos duplicados y versiones obsoletas | Idempotencia y conflictos correctos. |

## 6. Criterio de cierre

Un hallazgo solo se marca como **resuelto** cuando:
1. Existe un commit que contiene la corrección.
2. El código del commit actual confirma el cambio.
3. Se ejecutó la prueba asociada y se conserva evidencia.
4. Se actualizó esta tabla y la documentación relacionada.

No considerar la mera existencia de un test, workflow o comentario como evidencia de que pasó. No marcar una corrección como validada si solo se inspeccionó el código.

## 7. Dictamen histórico

Para el commit de referencia, el dictamen fue **NO APROBAR MERGE** debido a bloqueos de integración entre agente, video, almacenamiento y WebSocket, además de riesgos de correctitud visual y reintentos. Este dictamen es histórico y no debe atribuirse automáticamente a commits posteriores sin una nueva auditoría.
