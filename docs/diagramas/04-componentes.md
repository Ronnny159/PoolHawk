# Diagrama de componentes de PoolHawk

```mermaid
flowchart TB
    subgraph Cliente["Cliente"]
      UI["Frontend Vue 3 / Vite"]
      UIAPI["Axios / módulos API"]
      UI <--> UIAPI
    end

    subgraph Backend["Backend Spring Boot"]
      REST["Controladores REST"]
      APP["Servicios de aplicación"]
      DOMAIN["billiard-domain"]
      RULES["billiard-rules"]
      PERSIST["JPA / MatchStorage / Mappers"]
      SEC["JWT / Security"]
      WS["WebSocket / eventos"]
      REST --> APP
      APP --> DOMAIN
      APP --> RULES
      RULES --> DOMAIN
      APP --> PERSIST
      REST --> SEC
      APP --> WS
    end

    subgraph Infra["Infraestructura"]
      DB[("PostgreSQL")]
      FILES[("uploads/videos — almacenamiento local")]
    end

    subgraph Vision["Agente de visión"]
      SOURCE["CameraSource"]
      DET["BallDetector"]
      CLS["BallClassifier"]
      TRACK["BallTracker"]
      SHOT["ShotAnalyzer"]
      CLIENT["PoolHawkApiClient"]
      QUEUE["ColaEventos / traza"]
      SOURCE --> DET --> CLS --> TRACK --> SHOT --> CLIENT
      CLIENT <--> QUEUE
    end

    UIAPI <-->|HTTP REST| REST
    UI <-->|WebSocket| WS
    PERSIST <-->|JPA| DB
    REST --> FILES
    CLIENT <-->|HTTP REST| REST
```

## Alcance del diagrama
- La arquitectura refleja componentes presentes en el repositorio.
- El agente es un proceso Python independiente; no forma parte del proceso Spring.
- La carpeta de videos se muestra como almacenamiento local porque el controlador usa `uploads/videos`. En el Docker Compose del corte no se configuró un volumen persistente para esa carpeta.
- El diagrama no implica que la conexión de cámara del frontend esté implementada: el indicador de conexión en `VideoAnalysisConfig.vue` es simulado en este corte.
