package com.poolhawk.api.dto;

import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;

import java.util.List;

/**
 * DTO para la creacion de una partida.
 *
 * @param modalidad modalidad de la partida (BOLA_8 o POR_PUNTOS)
 * @param formato   formato de juego (INDIVIDUAL o PAREJAS)
 * @param jugadores nombres de los jugadores (de 2 a 4)
 * @param anchoMesa ancho opcional de la mesa (si es null, se usa el defecto)
 * @param altoMesa  alto opcional de la mesa (si es null, se usa el defecto)
 */
public record CrearPartidaRequest(
        TipoModalidad modalidad,
        FormatoJuego formato,
        List<String> jugadores,
        Double anchoMesa,
        Double altoMesa
) {
}