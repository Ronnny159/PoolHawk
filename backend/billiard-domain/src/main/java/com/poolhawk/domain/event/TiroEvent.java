package com.poolhawk.domain.event;

import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.TroneraNombre;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Evento que representa un tiro ejecutado por un jugador.
 *
 * <p>Un {@code TiroEvent} captura los hechos observados durante un tiro:
 * quién tiró, qué bola tocó primero, qué bolas se embolsaron y en qué
 * troneras, cuándo ocurrió y cuánto duró.</p>
 *
 * <p><b>Importante:</b> este evento NO contiene el veredicto del motor
 * de reglas. Para eso existe {@link ResultadoTiro}. Un evento se genera
 * con datos objetivos (observados por el agente de captura o reportados
 * manualmente); el resultado se calcula a partir del evento y las reglas
 * de la modalidad.</p>
 *
 * <p>Esta clase es inmutable: una vez generado, el evento no cambia. Es
 * un hecho histórico de la partida.</p>
 *
 * @param id                  identificador único del evento (UUID string) para idempotencia
 * @param partidaId           identificador de la partida a la que pertenece
 * @param jugador             jugador que ejecutó el tiro
 * @param primeraBolaTocada   primera bola que tocó la blanca, o {@code null} si no tocó ninguna
 * @param bolasEmbolsadas     lista de bolas embolsadas en este tiro (puede estar vacía)
 * @param troneras            lista de troneras donde cayeron las bolas (paralela a {@code bolasEmbolsadas})
 * @param timestamp           momento en que ocurrió el tiro
 * @param duracion            duración del tiro desde el golpe hasta que las bolas se detuvieron
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record TiroEvent(
    String id,
    String partidaId,
    Jugador jugador,
    Bola primeraBolaTocada,
    List<Bola> bolasEmbolsadas,
    List<TroneraNombre> troneras,
    Instant timestamp,
    Duration duracion
) {

    /**
     * Constructor compacto con validaciones.
     *
     * <p>Verifica que los campos obligatorios no sean nulos, que las
     * listas paralelas tengan la misma longitud, y que la duración no
     * sea negativa. Además, convierte las listas a inmutables.</p>
     *
     * @throws NullPointerException     si algún campo obligatorio es nulo
     * @throws IllegalArgumentException si las listas tienen distinta longitud
     *                                  o la duración es negativa
     */
    public TiroEvent {
        Objects.requireNonNull(id, "El id del evento no puede ser nulo");
        Objects.requireNonNull(partidaId, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        Objects.requireNonNull(bolasEmbolsadas, "La lista de bolas embolsadas no puede ser nula");
        Objects.requireNonNull(troneras, "La lista de troneras no puede ser nula");
        Objects.requireNonNull(timestamp, "El timestamp no puede ser nulo");
        Objects.requireNonNull(duracion, "La duración no puede ser nula");

        if (id.isBlank()) {
            throw new IllegalArgumentException(
                "El id del evento no puede estar en blanco"
            );
        }

        if (partidaId.isBlank()) {
            throw new IllegalArgumentException(
                "El id de la partida no puede estar en blanco"
            );
        }

        if (bolasEmbolsadas.size() != troneras.size()) {
            throw new IllegalArgumentException(
                "La cantidad de bolas embolsadas (" + bolasEmbolsadas.size() +
                ") debe coincidir con la cantidad de troneras (" + troneras.size() + ")"
            );
        }

        if (duracion.isNegative()) {
            throw new IllegalArgumentException(
                "La duración del tiro no puede ser negativa. " +
                "Valor recibido: " + duracion
            );
        }

        // Copia defensiva e inmutable
        bolasEmbolsadas = Collections.unmodifiableList(new ArrayList<>(bolasEmbolsadas));
        troneras = Collections.unmodifiableList(new ArrayList<>(troneras));
    }

    /**
     * Verifica si el tiro tocó al menos una bola.
     *
     * @return {@code true} si se tocó alguna bola
     */
    public boolean tocoAlgunaBola() {
        return primeraBolaTocada != null;
    }

    /**
     * Verifica si el tiro embolsó al menos una bola.
     *
     * @return {@code true} si se embolsó algo
     */
    public boolean embolsoAlgunaBola() {
        return !bolasEmbolsadas.isEmpty();
    }

    /**
     * Retorna cuántas bolas se embolsaron en este tiro.
     *
     * @return cantidad de bolas embolsadas
     */
    public int cantidadBolasEmbolsadas() {
        return bolasEmbolsadas.size();
    }

    /**
     * Verifica si alguna de las bolas embolsadas es la blanca.
     *
     * @return {@code true} si la blanca fue embolsada
     */
    public boolean embolsoLaBlanca() {
        return bolasEmbolsadas.stream().anyMatch(Bola::esBlanca);
    }

    /**
     * Verifica si alguna de las bolas embolsadas es la bola 8.
     *
     * @return {@code true} si la 8 fue embolsada
     */
    public boolean embolsoLaOcho() {
        return bolasEmbolsadas.stream().anyMatch(Bola::esOcho);
    }

    /**
     * Retorna la tronera donde cayó una bola específica, si aplica.
     *
     * @param numero número de la bola
     * @return la tronera donde cayó, o {@code null} si no fue embolsada
     */
    public TroneraNombre troneraDeBola(int numero) {
        for (int i = 0; i < bolasEmbolsadas.size(); i++) {
            if (bolasEmbolsadas.get(i).numero() == numero) {
                return troneras.get(i);
            }
        }
        return null;
    }
}