package com.poolhawk.domain.table;

import java.util.Objects;

/**
 * Representa una tronera de la mesa de billar.
 *
 * <p>Cada tronera tiene un nombre (según su posición), una coordenada
 * central y un radio que define el área de captura. Una bola cae en la
 * tronera cuando su centro está dentro del radio de captura.</p>
 *
 * <p>Los radios típicos son:</p>
 * <ul>
 *   <li>Troneras de esquina: ~11 cm</li>
 *   <li>Troneras laterales: ~13 cm</li>
 * </ul>
 *
 * <p>Esta clase es inmutable: una vez creada, la tronera no cambia.</p>
 *
 * @param nombre nombre de la tronera según su ubicación
 * @param posicion coordenada central de la tronera
 * @param radio radio de captura en centímetros
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record Tronera(TroneraNombre nombre, Coordenada posicion, double radio) {

    /** Radio por defecto para troneras de esquina, en centímetros. */
    public static final double RADIO_ESQUINA = 11.0;

    /** Radio por defecto para troneras laterales, en centímetros. */
    public static final double RADIO_LATERAL = 13.0;

    /**
     * Constructor compacto con validaciones.
     *
     * @throws NullPointerException     si el nombre o la posición son nulos
     * @throws IllegalArgumentException si el radio no es positivo
     */
    public Tronera {
        Objects.requireNonNull(nombre, "El nombre de la tronera no puede ser nulo");
        Objects.requireNonNull(posicion, "La posición de la tronera no puede ser nula");

        if (radio <= 0) {
            throw new IllegalArgumentException(
                "El radio de la tronera debe ser positivo. Valor recibido: " + radio
            );
        }
    }

    /**
     * Crea una tronera con el radio por defecto según su tipo (esquina o lateral).
     *
     * @param nombre   nombre de la tronera
     * @param posicion posición central
     * @return tronera con radio por defecto
     */
    public static Tronera conRadioPorDefecto(TroneraNombre nombre, Coordenada posicion) {
        double radio = nombre.esDeEsquina() ? RADIO_ESQUINA : RADIO_LATERAL;
        return new Tronera(nombre, posicion, radio);
    }

    /**
     * Verifica si una bola cae dentro de esta tronera según su posición.
     *
     * <p>Una bola cae en la tronera si su centro está a una distancia
     * menor o igual al radio de captura.</p>
     *
     * @param posicionBola posición central de la bola
     * @return {@code true} si la bola está dentro del radio de captura
     * @throws NullPointerException si la posición es nula
     */
    public boolean captura(Coordenada posicionBola) {
        Objects.requireNonNull(posicionBola, "La posición de la bola no puede ser nula");
        return posicion.estaCercaDe(posicionBola, radio);
    }

    /**
     * Verifica si una bola cae dentro de esta tronera.
     *
     * @param bola bola a verificar
     * @return {@code true} si la bola está dentro del radio de captura
     * @throws NullPointerException si la bola es nula
     */
    public boolean captura(Bola bola) {
        Objects.requireNonNull(bola, "La bola no puede ser nula");
        return captura(bola.posicion());
    }
}