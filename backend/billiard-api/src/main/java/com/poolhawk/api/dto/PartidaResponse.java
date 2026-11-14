package com.poolhawk.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO con el estado completo de una partida.
 *
 * @param id              identificador de la partida
 * @param modalidad       modalidad actual
 * @param formato         formato de juego (INDIVIDUAL, PAREJAS, TODOS_VS_TODOS)
 * @param fase            fase actual
 * @param equipoA         jugadores del equipo A
 * @param equipoB         jugadores del equipo B
 * @param puntajes        puntajes
 * @param jugadorActualId id estable del jugador con el turno (usar este, no el nombre, para identificarlo)
 * @param jugadorActual   nombre del jugador con el turno
 * @param ganador         nombre del ganador
 * @param cantidadTiros   cantidad de tiros registrados
 * @param fechaCreacion   fecha de creación
 * @param bolasEnMesa     números de bola que siguen sobre la mesa (incluye la blanca, id 0)
 */
public record PartidaResponse(
        String id,
        String modalidad,
        String formato,
        String fase,
        List<JugadorResponse> equipoA,
        List<JugadorResponse> equipoB,
        Map<String, Integer> puntajes,
        String jugadorActualId,
        String jugadorActual,
        String ganador,
        int cantidadTiros,
        Instant fechaCreacion,
        List<Integer> bolasEnMesa) {
}