*(El Diagrama de Secuencia - Punto 10 de Patricia. Muestra cómo viaja un tiro desde el usuario hasta la base de datos).*

```markdown
# Diagrama de Secuencia (Registrar Tiro)

```mermaid
sequenceDiagram
    actor A as Árbitro (Frontend Vue)
    participant C as MatchController (API)
    participant S as MatchService
    participant F as ReglasFactory
    participant R as ReglasBillar (Motor)
    participant DB as PostgreSQL (Futuro)

    A->>C: POST /api/v1/matches/{id}/events (JSON)
    activate C
    C->>S: registrarTiro(id, TiroEventRequest)
    activate S
    S->>DB: buscarPartida(id)
    DB-->>S: Partida
    S->>F: crear(modalidad)
    F-->>S: ReglasBillar (Bola8 o PorPuntos)
    S->>R: evaluar(MatchState, TiroEvent)
    activate R
    R-->>S: ResultadoTiro (Falta o Puntos)
    deactivate R
    S->>S: partida.aplicarTiro()
    S->>DB: guardarPartida(partida)
    S-->>C: ResultadoTiro
    deactivate S
    C-->>A: 200 OK (ResultadoTiroResponse)
    deactivate C