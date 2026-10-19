*(El Diagrama de Componentes - Punto 11 de Patricia).*

# Diagrama de Componentes

```mermaid
flowchart LR
    subgraph Cliente
        Front[App Web Vue 3]
    end

    subgraph Servidor API
        API[Spring Boot API]
        Rules[Motor de Reglas]
        Domain[Dominio Puro]
        
        API --> Rules
        API --> Domain
        Rules --> Domain
    end

    subgraph Infraestructura
        DB[(PostgreSQL)]
    end

    subgraph Captura
        Py[Agente Python OpenCV]
    end

    Front -- REST/JSON --> API
    Py -- REST/JSON --> API
    API -- JPA/JDBC --> DB
