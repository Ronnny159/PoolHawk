# PoolHawk — Contexto y estado del proyecto

> **Corte documental:** rama `feature/vision-tracking-agent`, commit `43ae3cba25f5dcdead273982cb1f810df0222ba8` (27 de noviembre de 2026). Este documento describe ese commit, no una versión posterior.

## 1. ¿Qué es PoolHawk?

PoolHawk es un sistema web para administrar partidas de billar y apoyar el registro de jugadas mediante un agente de visión computarizada. El sistema contempla las modalidades **Bola 8** y **Por Puntos**, con formatos de participación individual o por equipos según las reglas configuradas.

El objetivo funcional es centralizar la creación y consulta de partidas, validar los tiros con un motor de reglas, conservar el estado en una base de datos y distribuir actualizaciones a los clientes. El agente Python/OpenCV analiza una fuente de video, detecta y sigue bolas e infiere tiros cuando dispone de evidencia suficiente. El backend sigue siendo la autoridad sobre el estado y las reglas: el agente propone eventos, no decide por sí solo el resultado oficial.

## 2. Stack tecnológico

| Área | Tecnología / componente | Uso |
|---|---|---|
| Backend | Java 21, Spring Boot 3.3.5, Maven | API, servicios, seguridad y persistencia |
| Backend modular | `billiard-domain`, `billiard-rules`, `billiard-api` | Dominio, reglas e infraestructura/API |
| Frontend | Vue 3 Composition API, Vite, Axios | Interfaz web y consumo de API |
| Base de datos | PostgreSQL 16 | Persistencia relacional |
| Contenedores | Docker Compose | Entorno de base de datos, backend y frontend |
| Visión | Python, OpenCV, NumPy | Captura, detección, clasificación y seguimiento |
| Comunicación | REST/JSON y WebSocket | Comandos del agente y actualización en tiempo real |
| Pruebas | JUnit 5 y pytest | Pruebas automatizadas de backend y agente |

## 3. Arquitectura y módulos

- **Dominio (`billiard-domain`):** Java puro con conceptos como `Partida`, `Jugador`, `Equipo`, `Mesa`, `Bola`, `TiroEvent` y estado de partida.
- **Reglas (`billiard-rules`):** interfaz `ReglasBillar`, clase base `AbstractReglasBillar`, implementaciones `ReglasBola8` y `ReglasPorPuntos`, y `ReglasFactory`.
- **API (`billiard-api`):** controladores REST, DTO, servicios, mappers, entidades JPA, almacenamiento, autenticación JWT y WebSockets.
- **Frontend:** vistas y componentes Vue para autenticación, creación de partidas, marcadores, historial, correcciones, simulación de tiros y configuración de análisis de video.
- **Agente:** módulos Python separados para cámara, detección, clasificación, seguimiento, análisis de tiros, cola de eventos, cliente API y trazabilidad.

## 4. Estado funcional conocido al corte

### Backend
- Creación, consulta y listado de partidas.
- Validación de modalidad, formato y cantidad de jugadores.
- Registro de tiros con validación del jugador del turno, bolas y troneras.
- Evaluación de eventos mediante el motor de reglas.
- Persistencia con PostgreSQL/JPA.
- Idempotencia por `eventId`, comprobación de versión esperada y control optimista con `@Version`.
- Caché de lectura con TTL de 30 minutos y limpieza programada.
- Publicación de eventos después de confirmar la transacción.

### Frontend
- Interfaz Vue 3 con autenticación y componentes de partidas.
- Componentes para marcadores, creación de partidas, historial y correcciones.
- Flujo de carga de video y creación de partida rápida para análisis.
- La conexión de cámara de `VideoAnalysisConfig.vue` todavía está simulada en este corte; no debe presentarse como conexión real.

### Agente de visión
- Pipeline de fuente de video, detección de círculos, clasificación y seguimiento temporal.
- Inferencia de tiros con `ShotAnalyzer`.
- Configuración de ROI, troneras, detector y clasificador.
- Cliente REST, cola persistente de eventos pendientes y archivo de trazas.
- Los tiros ambiguos se marcan para revisión en lugar de enviarse automáticamente.
- La integración física de cámara y calibración requiere validaciones adicionales.

## 5. Objetivo de desarrollo

Consolidar una versión académica funcional, mantenible y demostrable: reglas separadas del transporte, persistencia consistente, agente visual reproducible, integración segura y documentación técnica trazable al código. El alcance es un proyecto universitario; no se declara producto comercial listo para producción.

## 6. Alcance y limitaciones documentales

Este corte se basa en inspección estática del repositorio. No se ejecutaron pruebas durante la auditoría asociada a este commit y no se verificó un resultado exitoso de CI. Las afirmaciones de funcionamiento deben respaldarse con pruebas reproducibles antes de una entrega final.

La rama comparada con `develop` estaba divergida: 59 commits por delante y 31 por detrás en el análisis de este corte. El commit exacto debe conservarse como referencia al leer los hallazgos.
