package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.api.mapper.MatchPersistenceMapper;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.TroneraNombre;
import com.poolhawk.rules.ReglasBillar;
import com.poolhawk.rules.ReglasConfig;
import com.poolhawk.rules.ReglasFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio orquestador de las partidas.
 * Delega la creación al Factory, la persistencia al Mapper y notifica por WebSocket.
 */
@Service
public class MatchService {
    private final MatchStorage storage;
    private final MatchLiveHandler liveHandler;
    private final PartidaMapper responseMapper;
    private final MatchPersistenceMapper persistenceMapper;
    private final MatchFactory matchFactory;
    private final ObjectMapper jsonMapper;

    private final Map<String, Partida> partidasEnMemoria = new HashMap<>();

    public MatchService(MatchStorage storage, MatchLiveHandler liveHandler, 
                        PartidaMapper responseMapper, MatchPersistenceMapper persistenceMapper, 
                        MatchFactory matchFactory, ObjectMapper jsonMapper) {
        this.storage = storage;
        this.liveHandler = liveHandler;
        this.responseMapper = responseMapper;
        this.persistenceMapper = persistenceMapper;
        this.matchFactory = matchFactory;
        this.jsonMapper = jsonMapper;
    }

    public Partida crear(CrearPartidaRequest request) {
        if (request.jugadores() == null || request.jugadores().size() < 2 || request.jugadores().size() > 4) {
            throw new IllegalArgumentException("Debe haber entre 2 y 4 jugadores.");
        }
        if (request.formato() == FormatoJuego.PAREJAS && request.jugadores().size() % 2 != 0) {
            throw new IllegalArgumentException("Las parejas requieren un número par de jugadores.");
        }

        String id = UUID.randomUUID().toString();
        Partida partida = matchFactory.crearDesdeRequest(id, request);
        
        partidasEnMemoria.put(id, partida);
        storage.guardar(persistenceMapper.toEntity(partida));
        
        return partida;
    }

    public Partida buscar(String id) {
        if (partidasEnMemoria.containsKey(id)) return partidasEnMemoria.get(id);

        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));

        return persistenceMapper.toDomain(entity);
    }

    /**
     * Lista todas las partidas, ignorando aquellas que estén corruptas (ej. datos viejos).
     */
    @Transactional(readOnly = true)
    public List<Partida> listarTodas() {
        return storage.listarTodas().stream()
            .map(entity -> {
                try {
                    return persistenceMapper.toDomain(entity);
                } catch (Exception e) {
                    System.err.println("Ignorando partida corrupta " + entity.getId() + ": " + e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        Partida partida = buscar(partidaId);
        TiroEvent evento = construirEvento(partida, request);
        ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
        ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);
        
        partida.aplicarTiro(evento, resultado);
        if (resultado.esVictoria()) partida.finalizar(evento.jugador());
        
        storage.guardar(persistenceMapper.toEntity(partida));

        try {
            String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
            liveHandler.broadcast(json);
        } catch (Exception e) {
            System.err.println("Error serializando partida: " + e.getMessage());
        }

        return resultado;
    }

    private TiroEvent construirEvento(Partida partida, TiroEventRequest request) {
        Jugador jugador = partida.getEstado().getTurnos().jugadores().stream()
            .filter(j -> j.id().equals(request.jugadorId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));

        Bola primeraTocada = request.primeraBolaTocada() != null ? 
            partida.getMesa().buscarBola(request.primeraBolaTocada()).orElse(null) : null;

        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null) {
            for (Integer numero : request.bolasEmbolsadas()) {
                partida.getMesa().buscarBola(numero).ifPresent(embolsadas::add);
            }
        }

        List<TroneraNombre> troneras = request.troneras() != null ? 
            request.troneras().stream().map(TroneraNombre::valueOf).toList() : new ArrayList<>();

        Duration duracion = request.duracionSegundos() != null ? 
            Duration.ofSeconds(request.duracionSegundos()) : Duration.ofSeconds(5);

        return new TiroEvent(UUID.randomUUID().toString(), partida.getId(), jugador,
            primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }
}