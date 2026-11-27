package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("MatchService - Pruebas de Transacción y AFTER_COMMIT")
class MatchTransactionTest {

    @Autowired
    private MatchService matchService;

    @SpyBean
    private MatchLiveHandler liveHandler;

    @Test
    @DisplayName("Commit exitoso: el listener AFTER_COMMIT emite el broadcast al match específico")
    void commitExitosoEmiteBroadcast() {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0);
        String partidaId = matchService.crear(request).getId();
        String jugadorId = matchService.buscar(partidaId).jugadorActual().id();

        reset(liveHandler);

        TiroEventRequest tiro = new TiroEventRequest("evt-commit", jugadorId, 1, List.of(), List.of(), 5);
        matchService.registrarTiro(partidaId, tiro);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        verify(liveHandler, times(1)).broadcastToMatch(eq(partidaId), anyString());
    }

    @Test
    @DisplayName("Rollback: el listener AFTER_COMMIT NO emite broadcast")
    void rollbackNoEmiteBroadcast() {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0);
        String partidaId = matchService.crear(request).getId();
        String jugadorId = matchService.buscar(partidaId).jugadorActual().id();

        reset(liveHandler);

        TiroEventRequest tiro = new TiroEventRequest("evt-rollback", jugadorId, 1, List.of(), List.of(), 5);
        matchService.registrarTiro(partidaId, tiro);

        TestTransaction.flagForRollback();
        TestTransaction.end();

        verify(liveHandler, never()).broadcastToMatch(anyString(), anyString());
        verify(liveHandler, never()).broadcast(anyString());
    }
}
