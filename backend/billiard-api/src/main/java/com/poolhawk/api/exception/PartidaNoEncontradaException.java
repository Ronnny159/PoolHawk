package com.poolhawk.api.exception;

/**
 * Excepcion lanzada cuando no se encuentra una partida por su id.
 *
 * <p>El {@code GlobalExceptionHandler} la captura y la convierte en
 * una respuesta HTTP 404 con un cuerpo {@code ErrorResponse}.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class PartidaNoEncontradaException extends RuntimeException {

    /**
     * Construye la excepcion con el id de la partida buscada.
     *
     * @param partidaId id de la partida que no se encontro
     */
    public PartidaNoEncontradaException(String partidaId) {
        super("Partida no encontrada: " + partidaId);
    }
}