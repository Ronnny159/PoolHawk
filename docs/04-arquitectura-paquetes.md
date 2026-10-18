# Arquitectura y Diagrama de Paquetes

El sistema **PoolHawk** sigue una arquitectura de **3 capas** (Domain-Driven Design) para garantizar la separación de responsabilidades, el bajo acoplamiento y la alta cohesión.

## 1. Estructura de Paquetes (Módulos Maven)

El backend está dividido en tres módulos principales:

```text
com.poolhawk
│
├── billiard-domain (Dominio Puro - Sin frameworks)
│   ├── table: Entidades de la mesa (Coordenada, Bola, Mesa, Tronera).
│   ├── player: Entidades de jugadores (Jugador, Equipo).
│   ├── match: Lógica del juego (Partida, Turnos, MatchState, Enums).
│   └── event: Eventos del juego (TiroEvent, ResultadoTiro).
│
├── billiard-rules (Motor de Reglas - Java Puro)
│   ├── ReglasBillar (Interfaz Strategy).
│   ├── AbstractReglasBillar (Template Method).
│   ├── impl: Implementaciones (ReglasBola8, ReglasPorPuntos).
│   └── ReglasFactory (Patrón Factory).
│
└── billiard-api (Capa de Infraestructura y API REST)
    ├── controller: Endpoints REST (MatchController, EventController).
    ├── service: Orquestación (MatchService).
    ├── dto: Objetos de transferencia de datos.
    ├── mapper: Transformadores de Dominio a DTO.
    ├── config: Configuración (CORS).
    └── exception: Manejo global de errores.
```
2. Justificación de la Arquitectura
billiard-domain no tiene dependencias externas (cero Spring, cero JPA). Esto permite que las reglas del billar se prueben de forma aislada y rápida con JUnit.

billiard-rules depende únicamente del dominio. Aplica el principio de Inversión de Dependencias (SOLID).

billiard-api es la única capa que conoce HTTP y Spring Boot. Actúa como un "adaptador" que traduce las peticiones web al núcleo del sistema.
