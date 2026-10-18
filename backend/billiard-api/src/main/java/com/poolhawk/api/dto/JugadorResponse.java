package com.poolhawk.api.dto;

/**
 * DTO con la informacion visible de un jugador.
 *
 * @param id     identificador unico del jugador
 * @param nombre nombre visible para mostrar al usuario
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record JugadorResponse(String id, String nombre) {
}