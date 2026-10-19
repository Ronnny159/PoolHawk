

# Modelo Entidad-Relación (MER)

```mermaid
erDiagram
    PARTIDA ||--|{ EQUIPO : "tiene"
    PARTIDA ||--o{ EVENTO_TIRO : "registra"
    EQUIPO ||--|{ JUGADOR : "contiene"
    JUGADOR ||--o{ EVENTO_TIRO : "realiza"

    PARTIDA {
        UUID id PK
        VARCHAR modalidad
        VARCHAR fase
        TIMESTAMP fecha_creacion
    }
    EQUIPO {
        UUID id PK
        UUID partida_id FK
        VARCHAR nombre
    }
    JUGADOR {
        UUID id PK
        UUID equipo_id FK
        VARCHAR nombre
    }
    EVENTO_TIRO {
        UUID id PK
        UUID partida_id FK
        UUID jugador_id FK
        BOOLEAN es_falta
        INTEGER puntos_ganados
        TIMESTAMP timestamp
    }
