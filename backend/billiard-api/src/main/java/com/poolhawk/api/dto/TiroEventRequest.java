package com.poolhawk.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * DTO para registrar un tiro.
 *
 * <p>El cliente envia los hechos observados del tiro: quien tiro, que
 * bola toco primero, que bolas embolso y en que troneras, y cuanto
 * duro. La API se encarga de convertirlo al evento de dominio
 * correspondiente.</p>
 *
 * @param jugadorId         id del jugador que ejecuto el tiro
 * @param primeraBolaTocada numero de la primera bola tocada, o {@code null}
 * @param bolasEmbolsadas   numeros de bolas embolsadas (puede estar vacia)
 * @param troneras          nombres de las troneras donde cayeron las bolas
 * @param duracionSegundos  duracion del tiro en segundos
 * @param versionEsperada   opcional: {@code cantidadTiros} de la partida que el cliente observo
 *                          al confirmar el tiro. Si no coincide con el estado actual, el
 *                          backend responde 409 en lugar de atribuir el tiro a otro turno
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record TiroEventRequest(
        String eventId, // NUEVO: ID único generado por el cliente

        @NotNull(message = "El jugador es obligatorio")
        String jugadorId,

        Integer primeraBolaTocada,
        List<Integer> bolasEmbolsadas,
        List<String> troneras,
        Integer duracionSegundos,

        Integer versionEsperada) {

    /**
     * Constructor de compatibilidad sin {@code versionEsperada} (control de versión desactivado).
     * Lo usan el simulador del frontend y las pruebas existentes.
     *
     * @param eventId           id unico generado por el cliente
     * @param jugadorId         id del jugador que ejecuto el tiro
     * @param primeraBolaTocada numero de la primera bola tocada, o {@code null}
     * @param bolasEmbolsadas   numeros de bolas embolsadas
     * @param troneras          nombres de las troneras
     * @param duracionSegundos  duracion del tiro en segundos
     */
    public TiroEventRequest(String eventId, String jugadorId, Integer primeraBolaTocada,
                            List<Integer> bolasEmbolsadas, List<String> troneras,
                            Integer duracionSegundos) {
        this(eventId, jugadorId, primeraBolaTocada, bolasEmbolsadas, troneras, duracionSegundos, null);
    }
}
