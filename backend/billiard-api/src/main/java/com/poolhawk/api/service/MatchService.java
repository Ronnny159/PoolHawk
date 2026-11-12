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
import com.poolhawk.domain.match.TipoModalidad;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MatchService {
    private final MatchStorage storage;
    private final MatchLiveHandler liveHandler;
    private final PartidaMapper responseMapper;
    private final MatchPersistenceMapper persistenceMapper;
    private final MatchFactory matchFactory;
    private final ObjectMapper jsonMapper;

    private final Map<String, Partida> partidasEnMemoria = new ConcurrentHashMap<>();
    private final Map<String, Object> locksPorPartida = new ConcurrentHashMap<>();

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

    @Transactional
    public Partida crear(CrearPartidaRequest request) {
        if (request.modalidad() == null || request.formato() == null || request.jugadores() == null) {
            throw new IllegalArgumentException("Configuración incompleta.");
        }
        int cantidad = request.jugadores().size();
        if (request.jugadores().stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Los nombres no pueden estar vacíos.");
        }
        if (request.formato() == FormatoJuego.PAREJAS && cantidad != 4) {
            throw new IllegalArgumentException("Parejas requiere 4 jugadores.");
        }
        if (request.formato() == FormatoJuego.INDIVIDUAL && cantidad != 2 && cantidad != 4) {
            throw new IllegalArgumentException("Individual requiere 2 o 4 jugadores.");
        }
        if (request.modalidad() == TipoModalidad.BOLA_8 && cantidad != 2) {
            throw new IllegalArgumentException("Bola 8 solo permite 1 vs 1.");
        }

        String id = UUID.randomUUID().toString();
        Partida partida = matchFactory.crearDesdeRequest(id, request);
        
        PartidaEntity entity = persistenceMapper.toEntity(partida);
        storage.guardar(entity);
        
        partidasEnMemoria.put(id, partida);
        
        return partida;
    }

    public Partida buscar(String id) {
        if (partidasEnMemoria.containsKey(id)) return partidasEnMemoria.get(id);

        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));

        return persistenceMapper.toDomain(entity);
    }

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

    @Transactional
    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        Object lock = locksPorPartida.computeIfAbsent(partidaId, k -> new Object());
        
        synchronized (lock) {
            Partida partida = buscar(partidaId);
            
            Jugador jugadorActual = partida.jugadorActual();
            if (!jugadorActual.id().equals(request.jugadorId())) {
                throw new IllegalArgumentException("No es el turno de este jugador. Turno actual: " + jugadorActual.nombre());
            }

            TiroEvent evento = construirEvento(partida, request, jugadorActual);
            ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
            ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);
            
            partida.aplicarTiro(evento, resultado);
            
            // Manejo de estados terminales
            if (resultado.esVictoria()) {
                partida.finalizar(evento.jugador());
            } else if (resultado.esDerrota()) {
                // El ganador es el oponente del que cometió la falta
                Jugador oponente = partida.getEquipoA().contiene(evento.jugador())
                    ? partida.getEquipoB().getJugadores().get(0)
                    : partida.getEquipoA().getJugadores().get(0);
                partida.finalizar(oponente);
            }
            
            PartidaEntity entityExistente = storage.buscar(partidaId).orElse(null);
            PartidaEntity entityNueva = persistenceMapper.toEntity(partida);
            
            if (entityExistente != null) {
                entityNueva.setVersion(entityExistente.getVersion());
            }
            
            storage.guardar(entityNueva);

            try {
                String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
                liveHandler.broadcast(json);
            } catch (Exception e) {
                System.err.println("Error serializando partida: " + e.getMessage());
            }

            return resultado;
        }
    }

    private TiroEvent construirEvento(Partida partida, TiroEventRequest request, Jugador jugadorActual) {
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

        return new TiroEvent(UUID.randomUUID().toString(), partida.getId(), jugadorActual,
            primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }
}