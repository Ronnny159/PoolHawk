package com.poolhawk.api.controller;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.PartidaResponse;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.service.MatchService;
import com.poolhawk.domain.match.Partida;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de partidas.
 * Expone endpoints para crear, consultar y listar partidas.
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
     * @param request datos de creación validados
     * @return la partida creada con HTTP 201
     */
    @PostMapping
    public ResponseEntity<PartidaResponse> crear(@Valid @RequestBody CrearPartidaRequest request) {
        Partida partida = matchService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(partida));
    }

    /**
     * Consulta una partida por su id.
     * 
     * @param id identificador de la partida
     * @return la partida con HTTP 200
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