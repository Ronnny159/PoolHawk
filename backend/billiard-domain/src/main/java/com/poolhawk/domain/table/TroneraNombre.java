package com.poolhawk.domain.table;

/**
 * Nombres estándar de las seis troneras de una mesa de billar.
 *
 * <p>La disposición física de las troneras en una mesa de pool es la
 * siguiente (vista cenital):</p>
 *
 * <pre>
 *   A ───────────────────── B
 *   │                       │
 *   │                       │
 *   C ───────────────────── D
 *   │                       │
 *   │                       │
 *   E ───────────────────── F
 * </pre>
 *
 * <p>Donde A, B, E, F son las cuatro esquinas y C, D son las troneras
 * laterales (a media altura de los lados largos).</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public enum TroneraNombre {

    /** Esquina superior izquierda. */
    TOP_LEFT("Superior izquierda"),

    /** Esquina superior derecha. */
    TOP_RIGHT("Superior derecha"),

    /** Tronera lateral izquierda (a media altura). */
    MIDDLE_LEFT("Lateral izquierda"),

    /** Tronera lateral derecha (a media altura). */
    MIDDLE_RIGHT("Lateral derecha"),

    /** Esquina inferior izquierda. */
    BOTTOM_LEFT("Inferior izquierda"),

    /** Esquina inferior derecha. */
    BOTTOM_RIGHT("Inferior derecha");

    private final String nombreVisible;

    /**
     * Constructor del enum.
     *
     * @param nombreVisible nombre para mostrar al usuario en español
     */
    TroneraNombre(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /**
     * Retorna el nombre visible de la tronera para mostrar al usuario.
     *
     * @return nombre en español
     */
    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Verifica si esta tronera está en una esquina de la mesa.
     *
     * @return {@code true} si es tronera de esquina
     */
    public boolean esDeEsquina() {
        return this == TOP_LEFT || this == TOP_RIGHT
            || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }

    /**
     * Verifica si esta tronera está en el medio de un lado largo.
     *
     * @return {@code true} si es tronera lateral
     */
    public boolean esLateral() {
        return this == MIDDLE_LEFT || this == MIDDLE_RIGHT;
    }

    /**
     * Verifica si esta tronera está en el lado superior de la mesa.
     *
     * @return {@code true} si está arriba
     */
    public boolean esSuperior() {
        return this == TOP_LEFT || this == TOP_RIGHT;
    }

    /**
     * Verifica si esta tronera está en el lado inferior de la mesa.
     *
     * @return {@code true} si está abajo
     */
    public boolean esInferior() {
        return this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }
}