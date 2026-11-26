package com.poolhawk.api.controller;

import com.poolhawk.api.dto.ResultadoTiroResponse;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.service.MatchService;
import com.poolhawk.domain.event.ResultadoTiro;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el registro y corrección de tiros.
 *
 * <p>Expone endpoints anidados bajo {@code /api/v1/matches/{matchId}/events}
 * para que el agente de captura (o el simulador) envie los tiros de una
 * partida, y para que un árbitro corrija el último tiro.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/matches/{matchId}/events")
public class EventController {

    private final MatchService matchService;

    public EventController(MatchService matchService) {
        this.matchService = matchService;
    }

    /**
     * Registra un tiro sobre una partida existente.
     *
     * @param matchId id de la partida
     * @param request datos del tiro
     * @return el veredicto del motor de reglas con HTTP 200
     */
    @PostMapping
    public ResponseEntity<ResultadoTiroResponse> registrarTiro(
            @PathVariable String matchId,
            @Valid @RequestBody TiroEventRequest request) {
        ResultadoTiro resultado = matchService.registrarTiro(matchId, request);
        return ResponseEntity.ok(toResponse(resultado));
    }

    /**
     * Corrige el último tiro registrado en la partida.
     * 
     * @param matchId id de la partida
     * @param request datos corregidos del tiro
     * @return el veredicto recalculado con HTTP 200
     */
    @PutMapping("/last")
    public ResponseEntity<ResultadoTiroResponse> corregirUltimoTiro(
            @PathVariable String matchId,
            @Valid @RequestBody TiroEventRequest request) {
        // Forzamos el eventId a "CORRECCION:" + timestamp para que sea único
        // pero el service debe manejar esto como una corrección del último evento
        String eventIdCorreccion = "CORRECCION:" + System.currentTimeMillis();
        TiroEventRequest correccionRequest = new TiroEventRequest(
            eventIdCorreccion,
            request.jugadorId(),
            request.primeraBolaTocada(),
            request.bolasEmbolsadas(),
            request.troneras(),
            request.duracionSegundos(),
            request.versionEsperada()
        );
        ResultadoTiro resultado = matchService.registrarTiro(matchId, correccionRequest);
        return ResponseEntity.ok(toResponse(resultado));
    }

    private ResultadoTiroResponse toResponse(ResultadoTiro r) {
        return new ResultadoTiroResponse(
            r.esFalta(),
            r.motivo() != null ? r.motivo().name() : null,
            r.puntos(),
            r.cambiaTurno(),
            r.esVictoria(),
            r.descripcion());
    }
}