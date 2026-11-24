package com.poolhawk.api.event;

/**
 * Evento de aplicación que se publica después de registrar un tiro.
 * El listener {@code MatchEventListener} lo consume con
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} para emitir
 * el WebSocket únicamente cuando la transacción quedó confirmada.
 */
public record PartidaActualizadaEvent(String partidaId, String json) {
}