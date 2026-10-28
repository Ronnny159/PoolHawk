package com.poolhawk.api.dto;

/**
 * DTO con el veredicto de un tiro emitido por el motor de reglas.
 *
 * @param esFalta     {@code true} si el tiro constituyo una falta
 * @param motivo      motivo de la falta, o {@code null} si no hubo falta
 * @param puntos      puntos ganados con el tiro
 * @param cambiaTurno {@code true} si tras el tiro pasa el turno
 * @param esVictoria  {@code true} si el tiro gana la partida
 * @param descripcion descripcion textual del resultado
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record ResultadoTiroResponse(
        boolean esFalta,
        String motivo,
        int puntos,
        boolean cambiaTurno,
        boolean esVictoria,
        String descripcion) {
}