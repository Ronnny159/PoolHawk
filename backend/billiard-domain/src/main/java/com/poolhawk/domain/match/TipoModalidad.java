package com.poolhawk.domain.match;

/**
 * Modalidades de billar soportadas por PoolHawk.
 *
 * <p>Cada modalidad define un conjunto de reglas específicas que se
 * implementan en módulos aparte. Esta enumeración solo identifica cuál
 * modalidad está activa en una partida.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public enum TipoModalidad {

    /**
     * Bola 8 individual: dos jugadores compiten por embolsar la bola 8
     * después de limpiar su grupo asignado (lisas o rayadas).
     */
    BOLA_8("Bola 8", 2, false),

    /**
     * Por Puntos en parejas: dos equipos de dos jugadores acumulan puntos
     * por cada bola embolsada, alternando turnos en rotación circular
     * A1 → B1 → A2 → B2.
     */
    POR_PUNTOS("Por Puntos", 4, true);

    private final String nombreVisible;
    private final int cantidadJugadores;
    private final boolean esPorEquipos;

    /**
     * Constructor del enum.
     *
     * @param nombreVisible     nombre para mostrar al usuario
     * @param cantidadJugadores número total de jugadores en la partida
     * @param esPorEquipos      {@code true} si se juega en equipos
     */
    TipoModalidad(String nombreVisible, int cantidadJugadores, boolean esPorEquipos) {
        this.nombreVisible = nombreVisible;
        this.cantidadJugadores = cantidadJugadores;
        this.esPorEquipos = esPorEquipos;
    }

    /**
     * Retorna el nombre visible de la modalidad para mostrar al usuario.
     *
     * @return nombre en español
     */
    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Retorna el número total de jugadores que participan en la modalidad.
     *
     * @return cantidad de jugadores
     */
    public int getCantidadJugadores() {
        return cantidadJugadores;
    }

    /**
     * Verifica si la modalidad se juega en equipos de dos personas.
     *
     * @return {@code true} si es por equipos
     */
    public boolean esPorEquipos() {
        return esPorEquipos;
    }

    /**
     * Verifica si la modalidad es individual.
     *
     * @return {@code true} si es individual
     */
    public boolean esIndividual() {
        return !esPorEquipos;
    }
}