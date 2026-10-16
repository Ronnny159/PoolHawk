package com.poolhawk.api.dto;

import com.poolhawk.domain.match.TipoModalidad;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para la creacion de una partida.
 *
 * <p>El cliente envia la modalidad y los nombres de los jugadores de
 * cada equipo. El servicio se encarga de construir el dominio a partir
 * de esta informacion.</p>
 *
 * @param modalidad modalidad de la partida (BOLA_8 o POR_PUNTOS)
 * @param equipoA   nombres de los jugadores del equipo A
 * @param equipoB   nombres de los jugadores del equipo B
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record CrearPartidaRequest(
        @NotNull(message = "La modalidad es obligatoria")
        TipoModalidad modalidad,

        @NotEmpty(message = "El equipo A debe tener al menos un jugador")
        List<String> equipoA,

        @NotEmpty(message = "El equipo B debe tener al menos un jugador")
        List<String> equipoB) {
}