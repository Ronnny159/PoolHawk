package com.poolhawk.api.event;

import com.poolhawk.api.websocket.MatchLiveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener que emite la actualización de la partida por WebSocket
 * SOLO después de que la transacción haya confirmado correctamente.
 * Envía el mensaje solo a las sesiones conectadas a esa partida específica.
 */
@Component
public class MatchEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchEventListener.class);

    private final MatchLiveHandler liveHandler;

    public MatchEventListener(MatchLiveHandler liveHandler) {
        this.liveHandler = liveHandler;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPartidaActualizada(PartidaActualizadaEvent event) {
        try {
            // Enviar solo a las sesiones de esta partida específica
            liveHandler.broadcastToMatch(event.partidaId(), event.json());
        } catch (Exception e) {
            log.warn("Error emitiendo WebSocket para partida {}", event.partidaId(), e);
        }
    }
}