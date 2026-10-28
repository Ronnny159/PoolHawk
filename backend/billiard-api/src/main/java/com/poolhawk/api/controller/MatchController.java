package com.poolhawk.api.controller;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.PartidaResponse;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.service.MatchService;
import com.poolhawk.domain.match.Partida;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para la gestion de partidas.
 *
 * <p>Expone endpoints para crear partidas, consultarlas por id y
 * listar todas las partidas existentes.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;
    private final PartidaMapper mapper;

    public MatchController(MatchService matchService, PartidaMapper mapper) {
        this.matchService = matchService;
        this.mapper = mapper;
    }

    /**
     * Crea una nueva partida.
     *
     * @param request datos de creacion
     * @return la partida creada con HTTP 201
     */
    @PostMapping
    public ResponseEntity<PartidaResponse> crear(
            @Valid @RequestBody CrearPartidaRequest request) {
        Partida partida = matchService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mapper.toResponse(partida));
    }

    /**
     * Consulta una partida por su id.
     *
     * @param id identificador de la partida
     * @return la partida con HTTP 200, o 404 si no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<PartidaResponse> obtener(@PathVariable String id) {
        Partida partida = matchService.buscar(id);
        return ResponseEntity.ok(mapper.toResponse(partida));
    }

    /**
     * Lista todas las partidas almacenadas.
     *
     * @return lista de partidas con HTTP 200
     */
    @GetMapping
    public ResponseEntity<List<PartidaResponse>> listar() {
        List<PartidaResponse> partidas = matchService.listarTodas()
            .stream().map(mapper::toResponse).toList();
        return ResponseEntity.ok(partidas);
    }
}