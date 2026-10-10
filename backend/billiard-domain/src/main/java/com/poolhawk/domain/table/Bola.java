package com.poolhawk.domain.table;

import java.util.Objects;

/**
 * Representa una bola de billar sobre la mesa.
 *
 * <p>Cada bola se identifica por su número (0 para la blanca, 1 a 15 para
 * las numeradas) y conoce su tipo (lisa, rayada, ocho o blanca), su
 * posición actual y si está en juego o ya fue embolsada.</p>
 *
 * <p>Esta clase es inmutable: mover una bola implica crear una nueva
 * instancia con la nueva posición. Esto evita efectos colaterales y
 * facilita la trazabilidad de los tiros en el historial.</p>
 *
 * @param numero   número de la bola (0 a 15)
 * @param tipo     clasificación de la bola
 * @param posicion posición actual sobre la mesa
 * @param enMesa   {@code true} si está sobre la mesa, {@code false} si fue embolsada
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record Bola(int numero, TipoBola tipo, Coordenada posicion, boolean enMesa) {

    /**
     * Constructor compacto que valida las invariantes de la bola.
     *
     * <p>Se verifica que el número esté en el rango válido, que el tipo
     * corresponda al número, y que la posición no sea nula.</p>
     *
     * @throws IllegalArgumentException si el número está fuera de rango
     *                                  o si el tipo no corresponde
     * @throws NullPointerException     si la posición o el tipo son nulos
     */
    public Bola {
        Objects.requireNonNull(tipo, "El tipo de bola no puede ser nulo");
        Objects.requireNonNull(posicion, "La posición de la bola no puede ser nula");

        if (numero < 0 || numero > 15) {
            throw new IllegalArgumentException(
                "El número de bola debe estar entre 0 y 15. " +
                "Valor recibido: " + numero
            );
        }

        TipoBola tipoEsperado = TipoBola.desdeNumero(numero);
        if (tipo != tipoEsperado) {
            throw new IllegalArgumentException(
                "El tipo " + tipo + " no corresponde al número " + numero +
                ". Tipo esperado: " + tipoEsperado
            );
        }
    }

    /**
     * Constructor simplificado que deriva el tipo a partir del número.
     *
     * <p>Útil cuando se crean bolas en su posición inicial, ya que el tipo
     * se puede inferir del número sin riesgo de error.</p>
     *
     * @param numero   número de la bola (0 a 15)
     * @param posicion posición inicial sobre la mesa
     * @return nueva bola en juego con el tipo derivado
     */
    public static Bola crear(int numero, Coordenada posicion) {
        return new Bola(numero, TipoBola.desdeNumero(numero), posicion, true);
    }

    /**
     * Crea una nueva instancia de esta bola en una posición diferente.
     *
     * <p>Como la clase es inmutable, mover la bola implica crear una
     * nueva. La bola original no se modifica.</p>
     *
     * @param nuevaPosicion nueva posición sobre la mesa
     * @return nueva bola con la posición actualizada
     * @throws NullPointerException si {@code nuevaPosicion} es nula
     */
    public Bola moverA(Coordenada nuevaPosicion) {
        return new Bola(this.numero, this.tipo, nuevaPosicion, this.enMesa);
    }

    /**
     * Marca esta bola como embolsada (fuera de la mesa).
     *
     * @return nueva bola marcada como fuera de juego
     */
    public Bola embolsar() {
        return new Bola(this.numero, this.tipo, this.posicion, false);
    }

    /**
     * Verifica si esta bola es la blanca (bola de tacada).
     *
     * @return {@code true} si es la blanca
     */
    public boolean esBlanca() {
        return this.tipo == TipoBola.BLANCA;
    }

    /**
     * Verifica si esta bola es la número 8.
     *
     * @return {@code true} si es la ocho
     */
    public boolean esOcho() {
        return this.tipo == TipoBola.OCHO;
    }

    /**
     * Verifica si esta bola pertenece al grupo de las lisas.
     *
     * @return {@code true} si es lisa
     */
    public boolean esLisa() {
        return this.tipo == TipoBola.LISA;
    }

    /**
     * Verifica si esta bola pertenece al grupo de las rayadas.
     *
     * @return {@code true} si es rayada
     */
    public boolean esRayada() {
        return this.tipo == TipoBola.RAYADA;
    }

    /**
     * Verifica si esta bola pertenece a un grupo (lisa o rayada).
     *
     * @return {@code true} si es lisa o rayada
     */
    public boolean esDeGrupo() {
        return this.tipo.esDeGrupo();
    }
}