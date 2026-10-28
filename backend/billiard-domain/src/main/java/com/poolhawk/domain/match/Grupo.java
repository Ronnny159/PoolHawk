package com.poolhawk.domain.match;

import com.poolhawk.domain.table.TipoBola;

/**
 * Grupo de bolas asignado a un jugador en la modalidad Bola 8.
 *
 * <p>En Bola 8, el primer tiro legal determina qué grupo de bolas debe
 * embolsar cada jugador: lisas (1-7) o rayadas (9-15). Mientras no se
 * haya asignado un grupo, el estado es {@link #SIN_ASIGNAR}.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public enum Grupo {

    /** Grupo de bolas lisas (números 1 al 7). */
    LISAS("Lisas", TipoBola.LISA),

    /** Grupo de bolas rayadas (números 9 al 15). */
    RAYADAS("Rayadas", TipoBola.RAYADA),

    /** Estado inicial antes de que el primer tiro legal asigne grupo. */
    SIN_ASIGNAR("Sin asignar", null);

    private final String nombreVisible;
    private final TipoBola tipoBolaAsociado;

    /**
     * Constructor del enum.
     *
     * @param nombreVisible     nombre para mostrar al usuario
     * @param tipoBolaAsociado  tipo de bola asociado, o {@code null} para SIN_ASIGNAR
     */
    Grupo(String nombreVisible, TipoBola tipoBolaAsociado) {
        this.nombreVisible = nombreVisible;
        this.tipoBolaAsociado = tipoBolaAsociado;
    }

    /**
     * Retorna el nombre visible del grupo para mostrar al usuario.
     *
     * @return nombre en español
     */
    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Retorna el tipo de bola asociado a este grupo.
     *
     * @return tipo de bola, o {@code null} si es SIN_ASIGNAR
     */
    public TipoBola getTipoBolaAsociado() {
        return tipoBolaAsociado;
    }

    /**
     * Verifica si el grupo ya fue asignado a un jugador.
     *
     * @return {@code true} si es LISAS o RAYADAS
     */
    public boolean estaAsignado() {
        return this != SIN_ASIGNAR;
    }

    /**
     * Retorna el grupo opuesto (LISAS ↔ RAYADAS).
     *
     * @return el grupo contrario
     * @throws IllegalStateException si el grupo es SIN_ASIGNAR
     */
    public Grupo opuesto() {
        return switch (this) {
            case LISAS -> RAYADAS;
            case RAYADAS -> LISAS;
            case SIN_ASIGNAR -> throw new IllegalStateException(
                "No se puede obtener el opuesto de un grupo sin asignar"
            );
        };
    }

    /**
     * Determina el grupo a partir de un tipo de bola.
     *
     * @param tipo tipo de bola (debe ser LISA o RAYADA)
     * @return grupo correspondiente
     * @throws IllegalArgumentException si el tipo no es LISA ni RAYADA
     */
    public static Grupo desdeTipoBola(TipoBola tipo) {
        if (tipo == TipoBola.LISA) return LISAS;
        if (tipo == TipoBola.RAYADA) return RAYADAS;
        throw new IllegalArgumentException(
            "Solo las bolas LISA o RAYADA tienen grupo asociado. " +
            "Tipo recibido: " + tipo
        );
    }
}