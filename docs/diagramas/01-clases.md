# Diagrama de clases del dominio y servicios

> Vista conceptual basada en las clases identificadas en el commit de referencia. No sustituye un UML completo generado desde todos los atributos y métodos del código.

```mermaid
classDiagram
    class Partida {
      +String id
      +TipoModalidad modalidad
      +MatchState estado
      +Equipo equipoA
      +Equipo equipoB
      +Mesa mesa
      +aplicarTiro(evento, resultado)
      +finalizar(jugador)
    }
    class MatchState {
      +Map puntajes
      +Turnos turnos
      +int cantidadTiros
      +buscarResultadoPorEvento(eventId)
    }
    class Jugador {
      +String id
      +String nombre
    }
    class Equipo {
      +List~Jugador~ jugadores
    }
    class Mesa {
      +List~Bola~ bolas
      +List~Tronera~ troneras
    }
    class Bola {
      +int numero
      +TipoBola tipo
      +Coordenada posicion
      +boolean enMesa
    }
    class TiroEvent {
      +String id
      +Jugador jugador
      +Bola primeraBolaTocada
    }
    class ReglasBillar {
      <<interface>>
      +evaluar(MatchState, TiroEvent) ResultadoTiro
    }
    class AbstractReglasBillar {
      <<abstract>>
    }
    class ReglasBola8
    class ReglasPorPuntos
    class ReglasFactory {
      +crear(TipoModalidad, ReglasConfig) ReglasBillar
    }
    class MatchService {
      +crear(request) Partida
      +buscar(id) Partida
      +registrarTiro(id, request) ResultadoTiro
    }
    class MatchFactory
    class MatchStorage
    class MatchPersistenceMapper
    class PartidaEntity {
      +version
    }

    Partida *-- MatchState
    Partida *-- Equipo
    Partida *-- Mesa
    Equipo *-- Jugador
    Mesa *-- Bola
    Bola *-- Coordenada
    ReglasBillar <|.. AbstractReglasBillar
    AbstractReglasBillar <|-- ReglasBola8
    AbstractReglasBillar <|-- ReglasPorPuntos
    ReglasFactory ..> ReglasBillar
    MatchService --> MatchFactory
    MatchService --> MatchStorage
    MatchService --> MatchPersistenceMapper
    MatchService ..> ReglasFactory
    MatchPersistenceMapper ..> PartidaEntity
    MatchStorage ..> PartidaEntity
```

## Observaciones
- El dominio incluye también `FormatoJuego`, `TipoModalidad`, `FasePartida`, `ConfiguracionJuego`, `Grupo`, `Turnos`, `TroneraNombre`, `ResultadoTiro` y `FaltaMotivo`.
- En infraestructura existen entidades JPA como `PartidaEntity`, `JugadorEntity` y `UsuarioEntity`.
- El diagrama resume responsabilidades y relaciones de alto nivel; verificar cardinalidades, atributos y firmas antes de usarlo como diagrama final de clases.
