package com.poolhawk.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando una partida no puede reconstruirse desde la base
 * de datos por datos corruptos, inconsistentes o incompletos.
 *
 * <p>Reemplaza los antiguos {@code catch} silenciosos del mapper de
 * persistencia. Si una fila de {@code tiros} tiene JSON inválido, una bola
 * inexistente, una tronera desconocida o un resultado ausente, la partida
 * completa se considera irrecuperable y se propaga esta excepción para que
 * el cliente reciba un error explícito (HTTP 500) en lugar de una partida
 * aparentemente válida con estado parcial.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class PartidaCorruptaException extends RuntimeException {

    private final String partidaId;

    /**
     * Construye la excepción con contexto de partida y detalle.
     *
     * @param partidaId id de la partida corrupta
     * @param detalle   descripción concreta del problema
     */
    public PartidaCorruptaException(String partidaId, String detalle) {
        super("La partida " + partidaId + " no pudo reconstruirse: " + detalle);
        this.partidaId = partidaId;
    }

    /**
     * Construye la excepción con contexto, detalle y causa subyacente.
     *
     * @param partidaId id de la partida corrupta
     * @param detalle   descripción concreta del problema
     * @param cause     causa original (por ejemplo, {@code JsonProcessingException})
     */
    public PartidaCorruptaException(String partidaId, String detalle, Throwable cause) {
        super("La partida " + partidaId + " no pudo reconstruirse: " + detalle, cause);
        this.partidaId = partidaId;
    }

    /**
     * @return id de la partida que falló al reconstruirse
     */
    public String getPartidaId() {
        return partidaId;
    }
}