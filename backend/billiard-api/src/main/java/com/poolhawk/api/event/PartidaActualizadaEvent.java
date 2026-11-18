package com.poolhawk.api.event;

/**
 * Evento de dominio publicado <b>dentro</b> de la transacción de
 * {@code MatchService.registrarTiro}. Su propósito es desacoplar el envío
 * por WebSocket del commit de la base de datos: el listener correspondiente
 * usa {@code @TransactionalEventListener(phase = AFTER_COMMIT)} y por tanto
 * solo se ejecuta cuando la transacción ha hecho commit real.
 *
 * <p>Si la transacción hace rollback, el evento se descarta y ningún cliente
 * recibe un estado que no fue persistido (H-01).</p>
 *
 * @param partidaId identificador de la partida actualizada
 * @param json      snapshot serializado del {@code PartidaResponse}
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record PartidaActualizadaEvent(String partidaId, String json) {
}