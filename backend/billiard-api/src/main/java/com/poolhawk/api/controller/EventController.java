package com.poolhawk.api.controller;

import com.poolhawk.api.dto.ResultadoTiroResponse;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.service.MatchService;
import com.poolhawk.domain.event.ResultadoTiro;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para el registro de tiros.
 *
 * <p>Expone un endpoint anidado bajo {@code /api/v1/matches/{matchId}/events}
 * para que el agente de captura (o el simulador) envie los tiros de una
 * partida.</p>
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