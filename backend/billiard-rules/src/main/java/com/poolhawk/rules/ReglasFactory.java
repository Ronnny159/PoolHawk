package com.poolhawk.rules;

import com.poolhawk.domain.match.TipoModalidad;

import java.util.Objects;

/**
 * Fábrica de instancias de {@link ReglasBillar}.
 *
 * <p>Esta clase implementa el patrón <b>Factory</b> para centralizar la
 * creación de las implementaciones de reglas. El código cliente no
 * necesita saber qué clase concreta instanciar; solo indica la modalidad
 * y la configuración deseada.</p>
 *
 * <p><b>Nota:</b> las implementaciones concretas se agregarán en ramas
 * posteriores. Por ahora la fábrica lanza una excepción controlada para
 * las modalidades aún no implementadas.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public final class ReglasFactory {

    /**
     * Constructor privado para evitar instanciación de la clase utilitaria.
     */
    private ReglasFactory() {
        throw new UnsupportedOperationException(
            "ReglasFactory es una clase utilitaria, no debe instanciarse"
        );
    }

    /**
     * Crea una instancia de reglas para la modalidad indicada.
     *
     * @param modalidad modalidad de la partida
     * @param config    configuración de reglas opcionales
     * @return instancia de reglas para la modalidad
     * @throws NullPointerException     si algún parámetro es nulo
     * @throws IllegalArgumentException si la modalidad no está soportada
     */
    public static ReglasBillar crear(TipoModalidad modalidad, ReglasConfig config) {
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(config, "La configuración no puede ser nula");

        return switch (modalidad) {
            case BOLA_8 -> crearReglasBola8(config);
            case POR_PUNTOS -> crearReglasPorPuntos(config);
        };
    }

    /**
     * Crea una instancia de reglas con la configuración por defecto.
     *
     * @param modalidad modalidad de la partida
     * @return instancia de reglas con configuración por defecto
     */
    public static ReglasBillar crear(TipoModalidad modalidad) {
        return crear(modalidad, ReglasConfig.porDefecto());
    }

    /**
     * Crea las reglas de Bola 8.
     *
     * <p><b>TODO:</b> implementar en la rama {@code feature/rules-bola8}.</p>
     *
     * @param config configuración de reglas
     * @return instancia de reglas de Bola 8
     */
    private static ReglasBillar crearReglasBola8(ReglasConfig config) {
        throw new UnsupportedOperationException(
            "Las reglas de Bola 8 se implementarán en la rama feature/rules-bola8"
        );
    }

    /**
     * Crea las reglas de Por Puntos.
     *
     * <p><b>TODO:</b> implementar en la rama {@code feature/rules-porpuntos}.</p>
     *
     * @param config configuración de reglas
     * @return instancia de reglas de Por Puntos
     */
    private static ReglasBillar crearReglasPorPuntos(ReglasConfig config) {
        throw new UnsupportedOperationException(
            "Las reglas de Por Puntos se implementarán en la rama feature/rules-porpuntos"
        );
    }
}