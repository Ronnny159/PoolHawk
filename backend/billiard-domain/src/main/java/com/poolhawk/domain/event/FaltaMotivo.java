package com.poolhawk.domain.event;

/**
 * Motivos por los que se comete una falta en una partida de billar.
 *
 * <p>Los motivos aplican a ambas modalidades salvo indicación contraria.
 * Cada modalidad valida un subconjunto específico de motivos.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public enum FaltaMotivo {

    /** La bola blanca no tocó ninguna otra bola. */
    NO_TOCAR_BOLA("La bola blanca no tocó ninguna bola"),

    /** El primer contacto fue con una bola que no corresponde al grupo asignado. */
    BOLA_EQUIVOCADA("Primer contacto con bola del grupo equivocado"),

    /** La bola blanca fue embolsada. */
    BLANCA_EMBOLSADA("La bola blanca fue embolsada"),

    /** En Bola 8, la bola 8 fue embolsada antes de limpiar el grupo. */
    BOLA_8_ANTES_DE_TIEMPO("La bola 8 fue embolsada antes de tiempo"),

    /** En Bola 8, la bola 8 fue embolsada en la tronera equivocada. */
    BOLA_8_TRONERA_INCORRECTA("La bola 8 fue embolsada en la tronera incorrecta"),

    /** En Bola 8, el tiro de apertura no impactó la bola 8. */
    APERTURA_INVALIDA("El tiro de apertura no impactó la bola 8"),

    /** No se tocó ninguna banda después del primer contacto. */
    NO_TOCAR_BANDA("No se tocó ninguna banda después del primer contacto"),

    /** El tiro excedió el tiempo permitido. */
    TIEMPO_EXCEDIDO("El tiro excedió el tiempo permitido"),

    /** El jugador tocó las bolas con la mano estando en juego. */
    MANO_EN_MESA("El jugador tocó las bolas con la mano"),

    /** El jugador no realizó un tiro legal (movió bolas al apuntar). */
    TIRO_ILEGAL("El tiro no fue legal"),

    /** En Por Puntos, el tiro no respetó el orden de rotación. */
    ORDEN_INCORRECTO("El jugador tiró fuera de turno"),

    /** Falta genérica sin motivo específico identificado. */
    OTRO("Otro motivo no especificado");

    private final String descripcion;

    /**
     * Constructor del enum.
     *
     * @param descripcion descripción de la falta para mostrar al usuario
     */
    FaltaMotivo(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Retorna la descripción de la falta para mostrar al usuario.
     *
     * @return descripción en español
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Verifica si esta falta aplica solo a la modalidad Bola 8.
     *
     * @return {@code true} si es exclusiva de Bola 8
     */
    public boolean esExclusivaDeBola8() {
        return this == BOLA_8_ANTES_DE_TIEMPO
            || this == BOLA_8_TRONERA_INCORRECTA
            || this == APERTURA_INVALIDA;
    }

    /**
     * Verifica si esta falta aplica solo a la modalidad Por Puntos.
     *
     * @return {@code true} si es exclusiva de Por Puntos
     */
    public boolean esExclusivaDePorPuntos() {
        return this == ORDEN_INCORRECTO;
    }

    /**
     * Verifica si esta falta aplica a cualquier modalidad.
     *
     * @return {@code true} si es común a todas las modalidades
     */
    public boolean esComun() {
        return !esExclusivaDeBola8() && !esExclusivaDePorPuntos();
    }
}