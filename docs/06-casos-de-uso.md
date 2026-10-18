# Casos de Uso del Sistema (Punto 7)

A continuación, se detallan los actores principales y los casos de uso críticos del sistema PoolHawk.

## 1. Actores del Sistema
- **Árbitro:** Persona encargada de crear la partida, supervisar el marcador y corregir errores en caso de fallos del sistema.
- **Jugador:** Persona que participa en la partida y consume la información del marcador en tiempo real.
- **Sistema de Captura (Futuro):** Agente automático (Python) que envía eventos de tiros a la API.

## 2. Casos de Uso Principales

### CU-01: Crear Partida
- **Actor principal:** Árbitro.
- **Precondición:** El sistema debe estar en línea.
- **Flujo normal:** El árbitro selecciona la modalidad (Bola 8 o Por Puntos), ingresa los nombres de los jugadores y presiona "Iniciar Partida". El sistema crea el estado y muestra el marcador.
- **Flujo alternativo:** Si faltan nombres, el sistema muestra un mensaje de error.

### CU-02: Registrar Tiro (Simulado)
- **Actor principal:** Árbitro (o Sistema de Captura).
- **Precondición:** Debe existir una partida en curso.
- **Flujo normal:** Se envía la información del tiro (jugador, bola tocada, bolas embolsadas). El sistema evalúa el tiro con el motor de reglas, actualiza los puntos y cambia el turno si es necesario.
- **Flujo alternativo:** Si el tiro es una falta, el sistema lo registra, muestra la alerta visual y cambia el turno.

### CU-03: Jugar Revancha
- **Actor principal:** Árbitro.
- **Precondición:** La partida actual debe estar finalizada.
- **Flujo normal:** El árbitro presiona el botón "Jugar Revancha". El sistema reinicia los puntajes, mantiene a los mismos jugadores y vuelve a la fase "EN CURSO".

## 3. Diagrama de Casos de Uso (Mermaid)

```mermaid
usecaseDiagram
    actor Árbitro
    actor Jugador
    actor "Sistema de Captura" as Python

    package "PoolHawk System" {
        usecase "Crear Partida" as UC1
        usecase "Registrar Tiro" as UC2
        usecase "Consultar Marcador" as UC3
        usecase "Jugar Revancha" as UC4
    }

    Árbitro --> UC1
    Árbitro --> UC2
    Árbitro --> UC4
    Jugador --> UC3
    Python --> UC2