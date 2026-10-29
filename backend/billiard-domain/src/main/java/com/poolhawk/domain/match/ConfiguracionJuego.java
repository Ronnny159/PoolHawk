package com.poolhawk.domain.match;

/**
 * Configuración inicial de una partida.
 * Define las reglas de negocio de cómo se estructura el juego.
 *
 * @param modalidad       El estilo de juego (BOLA_8 o POR_PUNTOS)
 * @param cantidadJugadores De 2 a 4 jugadores
 * @param formato         INDIVIDUAL o PAREJAS
 */
public record ConfiguracionJuego(
        TipoModalidad modalidad,
        int cantidadJugadores,
        FormatoJuego formato) {

    public ConfiguracionJuego {
        if (cantidadJugadores < 2 || cantidadJugadores > 4) {
            throw new IllegalArgumentException("La cantidad de jugadores debe ser entre 2 y 4.");
        }
        
        if (formato == FormatoJuego.PAREJAS && cantidadJugadores % 2 != 0) {
            throw new IllegalArgumentException("Para jugar en parejas, la cantidad de jugadores debe ser par (2 o 4).");
        }
    }

    /**
     * Calcula cuántos equipos/bandos hay en la partida.
     * Si es individual, cada jugador es un bando.
     * Si es parejas, hay 2 bandos.
     */
    public int obtenerCantidadBandos() {
        return formato == FormatoJuego.INDIVIDUAL ? cantidadJugadores : cantidadJugadores / 2;
    }
}