package com.poolhawk.api.event;

import com.poolhawk.api.websocket.MatchLiveHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener que emite la actualización de partida por WebSocket únicamente
 * <b>después</b> del commit de la transacción origen (H-01).
 *
 * <p>Al usar {@link TransactionPhase#AFTER_COMMIT}, si la transacción que
 * publicó el {@link PartidaActualizadaEvent} hace rollback, el evento se
 * descarta y no se emite nada a los clientes. Esto elimina la inconsistencia
 * "cliente recibe un estado que la base de datos nunca confirmó".</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Component
public class PartidaActualizadaListener {

    private static final Logger log = LoggerFactory.getLogger(PartidaActualizadaListener.class);

    private final MatchLiveHandler liveHandler;

    public PartidaActualizadaListener(MatchLiveHandler liveHandler) {
        this.liveHandler = liveHandler;
    }

    /**
     * Emite el snapshot de la partida por WebSocket tras el commit exitoso.
     *
     * <p>Cualquier fallo del broadcast se captura y se loguea: no debe
     * afectar al resultado de la operación ya persistida.</p>
     *
     * @param event evento publicado dentro de la transacción origen
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPartidaActualizada(PartidaActualizadaEvent event) {
        try {
            liveHandler.broadcast(event.json());
        } catch (Exception e) {
            log.warn("Error emitiendo actualización por WebSocket de la partida {}: {}",
                event.partidaId(), e.getMessage(), e);
        }
    }
}