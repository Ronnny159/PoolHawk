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

    private final Map<String, Partida> partidasEnMemoria = new ConcurrentHashMap<>();
    
    // MAPA DE LOCKS PARA EVITAR CONDICIONES DE CARRERA EN LOS TIROS
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

    /**
     * Crea una nueva partida aplicando las validaciones estrictas de la matriz de juego.
     *
     * @param request datos de creación
     * @return la partida creada
     * @throws IllegalArgumentException si la combinación de modalidad, formato y jugadores es inválida
     */
    public Partida crear(CrearPartidaRequest request) {
        // 1. Validaciones estructurales
        if (request.modalidad() == null || request.formato() == null || request.jugadores() == null) {
            throw new IllegalArgumentException("Configuración incompleta: Modalidad, formato y jugadores son obligatorios.");
        }

        int cantidad = request.jugadores().size();
        if (request.jugadores().stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Los nombres de los jugadores no pueden estar vacíos.");
        }

        // 2. Matriz de validación estricta
        if (request.formato() == FormatoJuego.PAREJAS && cantidad != 4) {
            throw new IllegalArgumentException("El formato de Parejas requiere exactamente 4 jugadores.");
        }
        if (request.formato() == FormatoJuego.INDIVIDUAL && cantidad != 2 && cantidad != 4) {
            throw new IllegalArgumentException("El formato Individual requiere 2 o 4 jugadores.");
        }
        if (request.modalidad() == TipoModalidad.BOLA_8 && cantidad != 2) {
            throw new IllegalArgumentException("La modalidad Bola 8 solo permite 1 vs 1.");
        }

        // 3. Creación usando el Factory
        String id = UUID.randomUUID().toString();
        Partida partida = matchFactory.crearDesdeRequest(id, request);
        
        // 4. Guardado
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
     * Lista todas las partidas, ignorando aquellas que estén corruptas o sean viejas.
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

    /**
     * Registra un tiro en una partida.
     * Utiliza un bloqueo (lock) por partida para garantizar que no haya condiciones de carrera.
     */
    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        // Obtenemos o creamos un lock para esta partida específica
        Object lock = locksPorPartida.computeIfAbsent(partidaId, k -> new Object());
        
        synchronized (lock) {
            try {
                Partida partida = buscar(partidaId);
                
                // VALIDACIÓN DE TURNO: El jugador debe ser el del turno actual
                Jugador jugadorActual = partida.jugadorActual();
                if (!jugadorActual.id().equals(request.jugadorId())) {
                    throw new IllegalArgumentException("No es el turno de este jugador. Turno actual: " + jugadorActual.nombre());
                }

                // 1. Evaluar el tiro con el motor de reglas
                TiroEvent evento = construirEvento(partida, request, jugadorActual);
                ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
                ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);
                
                // 2. Aplicar el tiro a la partida
                partida.aplicarTiro(evento, resultado);
                if (resultado.esVictoria()) {
                    partida.finalizar(evento.jugador());
                }
                
                // 3. Persistir los cambios
                storage.guardar(persistenceMapper.toEntity(partida));

                // 4. Notificar a los navegadores conectados por WebSocket
                try {
                    String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
                    liveHandler.broadcast(json);
                } catch (Exception e) {
                    System.err.println("Error serializando partida: " + e.getMessage());
                }

                return resultado;
            } finally {
                // Limpieza de memoria: si la partida terminó, eliminamos el lock
                Partida partida = partidasEnMemoria.get(partidaId);
                if (partida != null && partida.estaTerminada()) {
                    locksPorPartida.remove(partidaId);
                }
            }
        }
    }

    private TiroEvent construirEvento(Partida partida, TiroEventRequest request, Jugador jugadorActual) {
        // Ya no buscamos al jugador por ID, porque validamos que sea el jugadorActual
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