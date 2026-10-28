package com.poolhawk.domain.player;

import java.util.Objects;

/**
 * Representa a un jugador de billar.
 *
 * <p>Un jugador es una persona identificada por un identificador único y
 * un nombre visible. Puede competir de forma individual (modalidad Bola 8)
 * o como parte de un equipo (modalidad Por Puntos en parejas).</p>
 *
 * <p>Esta clase es inmutable: una vez creado, el jugador no cambia. Si
 * se requiere modificar el nombre, se crea una nueva instancia. Esto
 * garantiza que el jugador sea seguro de compartir entre múltiples
 * partidas y componentes del sistema.</p>
 *
 * @param id     identificador único del jugador
 * @param nombre nombre visible para mostrar al usuario
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record Jugador(String id, String nombre) {

    /**
     * Constructor compacto con validaciones.
     *
     * <p>El identificador no puede ser nulo ni estar en blanco. El nombre
     * tampoco puede ser nulo ni estar en blanco, y no puede exceder los
     * 80 caracteres (límite razonable para un nombre visible).</p>
     *
     * @throws NullPointerException     si el id o el nombre son nulos
     * @throws IllegalArgumentException si el id o el nombre están en blanco
     *                                  o si el nombre excede 80 caracteres
     */
    public Jugador {
        Objects.requireNonNull(id, "El identificador del jugador no puede ser nulo");
        Objects.requireNonNull(nombre, "El nombre del jugador no puede ser nulo");

        if (id.isBlank()) {
            throw new IllegalArgumentException(
                "El identificador del jugador no puede estar en blanco"
            );
        }

        if (nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre del jugador no puede estar en blanco"
            );
        }

        if (nombre.length() > 80) {
            throw new IllegalArgumentException(
                "El nombre del jugador no puede exceder los 80 caracteres. " +
                "Longitud actual: " + nombre.length()
            );
        }

        // Normalizar espacios en blanco al inicio y fin
        nombre = nombre.trim();
    }

    /**
     * Crea un nuevo jugador con un nombre actualizado.
     *
     * <p>Como la clase es inmutable, cambiar el nombre implica crear una
     * nueva instancia. El jugador original no se modifica.</p>
     *
     * @param nuevoNombre nuevo nombre del jugador
     * @return nuevo jugador con el nombre actualizado
     * @throws IllegalArgumentException si el nuevo nombre es inválido
     */
    public Jugador renombrar(String nuevoNombre) {
        return new Jugador(this.id, nuevoNombre);
    }
}