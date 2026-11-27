# Diagrama de actividades — Procesamiento de un tiro

```mermaid
flowchart TD
    A([Inicio: llega solicitud de tiro]) --> B{¿Payload válido y eventId presente?}
    B -- No --> E400[Rechazar solicitud]
    B -- Sí --> C[Consultar partida desde almacenamiento]
    C --> D{¿Existe partida?}
    D -- No --> E404[Partida no encontrada]
    D -- Sí --> I{¿eventId ya procesado?}
    I -- Sí --> J{¿Payload coincide?}
    J -- No --> E400
    J -- Sí --> R[Devolver resultado original]
    I -- No --> V{¿Versión esperada vigente?}
    V -- No --> E409[Conflicto de versión]
    V -- Sí --> T{¿Es el turno del jugador?}
    T -- No --> E409
    T -- Sí --> F[Construir TiroEvent validado]
    F --> G[Seleccionar ReglasBillar]
    G --> H[Evaluar reglas]
    H --> K[Aplicar resultado al dominio]
    K --> L[Mapear dominio a entidad]
    L --> M[Guardar con control @Version]
    M --> N{¿Commit exitoso?}
    N -- No --> E409
    N -- Sí --> O[Publicar evento después del commit]
    O --> P[Difundir actualización WebSocket]
    P --> Q([Responder resultado])
```

## Notas
- Los errores de validación, partida inexistente y conflicto no deben mutar el estado persistido.
- Si el evento ya fue procesado con el mismo contenido, se devuelve el resultado original.
- La difusión WebSocket depende de una asociación correcta entre sesión y partida autorizada.
