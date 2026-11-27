# Diagrama de secuencia — Registro de tiro

El diagrama representa el flujo lógico del backend en el corte documentado. Se omiten detalles internos de DTO y manejo de excepciones para facilitar su lectura.

```mermaid
sequenceDiagram
    actor U as Árbitro / Frontend
    participant C as MatchController / EventController
    participant S as MatchService
    participant ST as MatchStorage
    participant PM as MatchPersistenceMapper
    participant RF as ReglasFactory
    participant R as ReglasBillar
    participant DB as PostgreSQL
    participant EP as EventPublisher
    participant WL as MatchEventListener
    participant WS as MatchLiveHandler

    U->>C: POST evento de tiro (JSON)
    C->>S: registrarTiro(matchId, request)
    S->>ST: buscar(matchId)
    ST->>DB: SELECT partida
    DB-->>ST: PartidaEntity
    ST-->>S: entidad
    S->>PM: toDomain(entity)
    PM-->>S: Partida
    S->>S: validar eventId, payload, versión y turno
    S->>RF: crear(modalidad, configuración)
    RF-->>S: ReglasBillar
    S->>R: evaluar(estado, TiroEvent)
    R-->>S: ResultadoTiro
    S->>S: aplicarTiro y actualizar estado
    S->>PM: toEntity(partida)
    PM-->>S: PartidaEntity
    S->>ST: guardar(entity)
    ST->>DB: UPDATE / INSERT con versión
    DB-->>ST: commit
    S->>EP: publicar PartidaActualizadaEvent
    EP-->>WL: evento de partida
    Note over WL,WS: La difusión debe ocurrir después del commit
    WL->>WS: broadcastToMatch(matchId, payload)
    WS-->>U: actualización WebSocket
    S-->>C: ResultadoTiro
    C-->>U: respuesta HTTP
```

## Consideraciones
- La base de datos es la fuente de verdad.
- El servicio realiza comprobación idempotente por `eventId` y valida `versionEsperada`.
- JPA `@Version` protege frente a escrituras concurrentes.
- El evento se publica para difusión después de confirmar la transacción.
- El enrutamiento de sesiones por partida requiere una suscripción autorizada y pruebas de aislamiento; el diagrama representa el objetivo de integración, no certifica que el enrutamiento esté resuelto en este commit.
