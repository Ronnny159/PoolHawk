package com.poolhawk.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO con el estado completo de una partida.
 *
 * <p>Es la vista publica de una {@code Partida} del dominio. Expone
 * unicamente la informacion relevante para el cliente REST, sin
 * acoplar la API a las clases internas del dominio.</p>
 *
 * @param id            identificador de la partida
 * @param modalidad     modalidad actual
 * @param fase          fase actual del ciclo de vida
 * @param equipoA       jugadores del equipo A
 * @param equipoB       jugadores del equipo B
 * @param puntajes      puntaje actual por nombre de jugador
 * @param grupos        grupo asignado por nombre de jugador (LISAS, RAYADAS o SIN_ASIGNAR)
 * @param jugadorActual nombre del jugador con el turno
 * @param ganador       nombre del ganador, o {@code null} si no ha terminado
 * @param cantidadTiros cantidad de tiros registrados
 * @param fechaCreacion momento de creacion de la partida
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record PartidaResponse(
        String id,
        String modalidad,
        String fase,
        List<JugadorResponse> equipoA,
        List<JugadorResponse> equipoB,
        Map<String, Integer> puntajes,
        Map<String, String> grupos,
        Map<String, Integer> puntajesPorEquipo,   // ← NUEVO: totales ya calculados
        String jugadorActual,
        String ganador,
        int cantidadTiros,
        Instant fechaCreacion) {
}