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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        PartidaEntity entity = storage.buscar(id).orElseThrow(() -> new PartidaNoEncontradaException(id));
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
            
            // 1. IDEMPOTENCIA
            if (request.eventId() != null && !request.eventId().isBlank()) {
                boolean yaProcesado = partida.getEstado().getHistorial().stream()
                    .anyMatch(e -> e.id().equals(request.eventId()));
                if (yaProcesado) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El tiro ya fue registrado.");
                }
            }

            // 2. VALIDACIÓN DE TURNO
            Jugador jugadorActual = partida.jugadorActual();
            if (!jugadorActual.id().equals(request.jugadorId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No es el turno de este jugador.");
            }

            // 3. CONSTRUCCIÓN Y VALIDACIÓN ESTRICTA
            TiroEvent evento = construirEventoValidado(partida, request, jugadorActual);
            
            // 4. EVALUAR Y APLICAR
            ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
            ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);
            
            partida.aplicarTiro(evento, resultado);
            
            if (resultado.esVictoria()) {
                partida.finalizar(evento.jugador());
            } else if (resultado.esDerrota()) {
                Jugador oponente = partida.getEquipoA().contiene(evento.jugador())
                    ? partida.getEquipoB().getJugadores().get(0)
                    : partida.getEquipoA().getJugadores().get(0);
                partida.finalizar(oponente);
            }
            
            // 5. PERSISTIR
            PartidaEntity entityExistente = storage.buscar(partidaId).orElse(null);
            PartidaEntity entityNueva = persistenceMapper.toEntity(partida);
            if (entityExistente != null) entityNueva.setVersion(entityExistente.getVersion());
            storage.guardar(entityNueva);

            // 6. WEBSOCKET
            try {
                String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
                liveHandler.broadcast(json);
            } catch (Exception e) {
                System.err.println("Error serializando partida: " + e.getMessage());
            }

            return resultado;
        }
    }

    private TiroEvent construirEventoValidado(Partida partida, TiroEventRequest request, Jugador jugadorActual) {
        // 1. El eventId es OBLIGATORIO
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventId es obligatorio para garantizar idempotencia.");
        }
        String eventId = request.eventId();

        // Validar primera bola tocada
        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            primeraTocada = partida.getMesa().buscarBola(request.primeraBolaTocada())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola " + request.primeraBolaTocada() + " no existe."));
            if (!primeraTocada.enMesa()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola " + request.primeraBolaTocada() + " ya no está en la mesa.");
            }
        }

        // Validar bolas embolsadas (sin duplicados, sin nulos, que existan en la mesa)
        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null && !request.bolasEmbolsadas().isEmpty()) {
            Set<Integer> bolasUnicas = new HashSet<>();
            for (Integer numero : request.bolasEmbolsadas()) {
                if (numero == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Número de bola nulo no permitido.");
                }
                if (!bolasUnicas.add(numero)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola duplicada en el tiro: " + numero);
                }
                Bola bola = partida.getMesa().buscarBola(numero)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola " + numero + " no existe."));
                if (!bola.enMesa()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola " + numero + " ya fue embolsada.");
                }
                embolsadas.add(bola);
            }
        }

        // Validar troneras (sin nulos, coherentes con las bolas)
        List<TroneraNombre> troneras = new ArrayList<>();
        if (request.troneras() != null) {
            if (request.troneras().size() != embolsadas.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de troneras (" + request.troneras().size() + ") no coincide con las bolas embolsadas (" + embolsadas.size() + ").");
            }
            for (String nombre : request.troneras()) {
                if (nombre == null || nombre.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de tronera nulo o vacío.");
                }
                try {
                    troneras.add(TroneraNombre.valueOf(nombre.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tronera inválida: " + nombre);
                }
            }
        } else if (!embolsadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe especificar la tronera para cada bola embolsada.");
        }

        // Duración
        Duration duracion = request.duracionSegundos() != null ? 
            Duration.ofSeconds(request.duracionSegundos()) : Duration.ofSeconds(5);

        return new TiroEvent(eventId, partida.getId(), jugadorActual,
            primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }
}