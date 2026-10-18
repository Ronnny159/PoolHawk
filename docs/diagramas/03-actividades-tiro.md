*(El Diagrama de Actividades - Punto 8 de Patricia. Muestra la lógica de decisión de la API).*


# Diagrama de Actividades (Procesar un Tiro)

```mermaid
flowchart TD
    Inicio([El jugador envía un tiro]) --> ValidarDatos{¿Datos válidos?}
    ValidarDatos -- No --> Error400[Retornar 400 Bad Request]
    ValidarDatos -- Sí --> BuscarPartida[Buscar Partida en BD]
    
    BuscarPartida --> Existe{¿Partida existe?}
    Existe -- No --> Error404[Retornar 404 Not Found]
    Existe -- Sí --> EvaluarReglas[Evaluar tiro con ReglasBillar]
    
    EvaluarReglas --> EsFalta{¿Es falta?}
    
    EsFalta -- Sí --> RegistrarFalta[Registrar Falta + Cambiar Turno]
    EsFalta -- No --> CalcularPuntos[Sumar Puntos / Asignar Grupo]
    
    CalcularPuntos --> EsVictoria{¿Gana la partida?}
    EsVictoria -- Sí --> Finalizar[Finalizar Partida + Declarar Ganador]
    EsVictoria -- No --> SigueTirando[Si embolsó, sigue. Si no, cambia turno]
    
    RegistrarFalta --> Guardar[Guardar Estado en BD]
    Finalizar --> Guardar
    SigueTirando --> Guardar
    
    Guardar --> Retornar200([Retornar 200 OK con Resultado])
