package com.poolhawk.domain.table;

/**
 * Clasificación de las bolas de billar según su patrón de color.
 *
 * <p>En las modalidades Bola 8 y Por Puntos, las bolas se dividen en cuatro
 * categorías bien definidas: las lisas (números 1 al 7), la bola 8, las
 * rayadas (números 9 al 15) y la bola blanca (sin número, usada por el
 * jugador para golpear).</p>
 *
 * <p>Este tipo es esencial para validar tiros legales: en Bola 8, un
 * jugador solo puede golpear primero las bolas de su grupo asignado
 * (lisas o rayadas).</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public enum TipoBola {

    /** Bola blanca o de tacada. Sin número, usada para golpear las demás. */
    BLANCA("Blanca", 0),

    /** Bolas del 1 al 7, con color sólido sin franja. */
    LISA("Lisa", 1),

    /** Bola número 8, de color negro. Es la bola final de la modalidad Bola 8. */
    OCHO("Ocho", 8),

    /** Bolas del 9 al 15, con franja blanca alrededor. */
    RAYADA("Rayada", 9);

    private final String nombreVisible;
    private final int primerNumero;

    /**
     * Constructor del enum.
     *
     * @param nombreVisible nombre para mostrar al usuario
     * @param primerNumero  número mínimo de bola que pertenece a este tipo
     */
    TipoBola(String nombreVisible, int primerNumero) {
        this.nombreVisible = nombreVisible;
        this.primerNumero = primerNumero;
    }

    /**
     * Retorna el nombre visible del tipo para mostrar al usuario.
     *
     * @return nombre en español
     */
    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Retorna el primer número de bola que corresponde a este tipo.
     *
     * @return número inicial del rango
     */
    public int getPrimerNumero() {
        return primerNumero;
    }

    /**
     * Determina el tipo de bola a partir de su número.
     *
     * <p>Reglas de derivación:</p>
     * <ul>
     *   <li>0 → {@link #BLANCA}</li>
     *   <li>1 al 7 → {@link #LISA}</li>
     *   <li>8 → {@link #OCHO}</li>
     *   <li>9 al 15 → {@link #RAYADA}</li>
     * </ul>
     *
     * @param numero número de la bola (0 a 15)
     * @return tipo de bola correspondiente
     * @throws IllegalArgumentException si el número está fuera del rango 0-15
     */
    public static TipoBola desdeNumero(int numero) {
        if (numero < 0 || numero > 15) {
            throw new IllegalArgumentException(
                "El número de bola debe estar entre 0 y 15. " +
                "Valor recibido: " + numero
            );
        }
        if (numero == 0) return BLANCA;
        if (numero == 8) return OCHO;
        if (numero <= 7) return LISA;
        return RAYADA;
    }

    /**
     * Verifica si este tipo corresponde a una bola de grupo (lisa o rayada).
     *
     * <p>Las bolas de grupo son las que un jugador debe embolsar para
     * completar su asignación antes de tirar por la bola 8.</p>
     *
     * @return {@code true} si es LISA o RAYADA
     */
    public boolean esDeGrupo() {
        return this == LISA || this == RAYADA;
    }

    /**
     * Verifica si este tipo es la bola final de la modalidad Bola 8.
     *
     * @return {@code true} si es la bola OCHO
     */
    public boolean esBolaFinal() {
        return this == OCHO;
    }
}