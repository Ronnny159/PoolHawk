# Diagrama de Clases

```mermaid
classDiagram
    class Coordenada {
        +double x
        +double y
        +distanciaA(Coordenada) double
    }
    class Bola {
        +int numero
        +TipoBola tipo
        +Coordenada posicion
        +boolean enMesa
    }
    class Mesa {
        +List~Bola~ bolas
        +List~Tronera~ troneras
        +agregarBola(Bola)
    }
    class Jugador {
        +String id
        +String nombre
    }
    class Equipo {
        +List~Jugador~ jugadores
    }
    class Partida {
        +String id
        +TipoModalidad modalidad
        +Equipo equipoA
        +Equipo equipoB
        +MatchState estado
    }
    class MatchState {
        +Map puntajes
        +Turnos turnos
    }
    class ReglasBillar {
        <<interface>>
        +evaluar(MatchState, TiroEvent) ResultadoTiro
    }
    class AbstractReglasBillar {
        <<abstract>>
        #evaluarTiroLegal(MatchState, TiroEvent)* ResultadoTiro
    }
    class ReglasBola8 {
        #evaluarTiroLegal(MatchState, TiroEvent) ResultadoTiro
    }
    class ReglasPorPuntos {
        #evaluarTiroLegal(MatchState, TiroEvent) ResultadoTiro
    }
    class ReglasFactory {
        +crear(TipoModalidad) ReglasBillar
    }

    Partida *-- MatchState
    Partida *-- Equipo
    MatchState *-- Turnos
    Mesa *-- Bola
    Bola *-- Coordenada
    Equipo *-- Jugador
    ReglasBillar <|.. AbstractReglasBillar
    AbstractReglasBillar <|-- ReglasBola8
    AbstractReglasBillar <|-- ReglasPorPuntos
    ReglasFactory ..> ReglasBillar