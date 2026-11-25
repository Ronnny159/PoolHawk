package com.poolhawk.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.event.PartidaActualizadaEvent;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.api.mapper.MatchPersistenceMapper;
import com.poolhawk.api.mapper.PartidaMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchStorage storage;
    private final PartidaMapper responseMapper;
    private final MatchPersistenceMapper persistenceMapper;
    private final MatchFactory matchFactory;
    private final ObjectMapper jsonMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Cache de solo lectura para mejorar rendimiento en consultas GET.
    // La concurrencia de escritura se gestiona de forma segura con JPA @Version (Optimistic Locking).
    private final Map<String, Partida> partidasEnMemoria = new ConcurrentHashMap<>();

    public MatchService(MatchStorage storage, PartidaMapper responseMapper,
                        MatchPersistenceMapper persistenceMapper, MatchFactory matchFactory,
                        ObjectMapper jsonMapper, ApplicationEventPublisher eventPublisher) {
        this.storage = storage;
        this.responseMapper = responseMapper;
        this.persistenceMapper = persistenceMapper;
        this.matchFactory = matchFactory;
        this.jsonMapper = jsonMapper;
        this.eventPublisher = eventPublisher;
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
        
        registrarInvalidacionEnRollback(id);
        publicarEnCacheTrasCommit(id, partida);
        
        return partida;
    }

    @Transactional(readOnly = true)
    public Partida buscar(String id) {
        Partida cacheada = partidasEnMemoria.get(id);
        if (cacheada != null) {
            return cacheada;
        }
        
        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));
        Partida reconstruida = persistenceMapper.toDomain(entity);
        partidasEnMemoria.put(id, reconstruida);
        return reconstruida;
    }

    @Transactional(readOnly = true)
    public List<Partida> listarTodas() {
        return storage.listarTodas().stream()
            .map(entity -> {
                try {
                    return persistenceMapper.toDomain(entity);
                } catch (Exception e) {
                    log.warn("Ignorando partida corrupta {}: {}", entity.getId(), e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Transactional
    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        // 0. Validación estricta de entrada
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventId es obligatorio.");
        }

        registrarInvalidacionEnRollback(partidaId);
        
        // 1. Cargar snapshot fresco desde BD para esta transacción (Fuente de la verdad)
        PartidaEntity entityCargada = storage.buscar(partidaId)
            .orElseThrow(() -> new PartidaNoEncontradaException(partidaId));
        long versionCargada = entityCargada.getVersion();
        Partida partida = persistenceMapper.toDomain(entityCargada);

        // 2. Idempotencia transparente
        Optional<ResultadoTiro> resultadoOriginal = partida.getEstado().buscarResultadoPorEvento(request.eventId());
        if (resultadoOriginal.isPresent()) {
            TiroEvent tiroOriginal = partida.getEstado().getHistorial().stream()
                .filter(e -> e.id().equals(request.eventId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Inconsistencia de idempotencia."));
            if (!mismoPayload(request, tiroOriginal)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventId ya fue utilizado con otro payload.");
            }
            return resultadoOriginal.get();
        }

        // 3. Validación de versión esperada (Optimistic Locking a nivel de aplicación)
        if (request.versionEsperada() != null
                && request.versionEsperada().intValue() != partida.getEstado().cantidadTiros()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Estado desactualizado. Versión esperada: " + request.versionEsperada() + 
                ", versión actual en BD: " + partida.getEstado().cantidadTiros());
        }

        // 4. Validación de turno (Auditoría: evitar que un jugador tire por otro)
        Jugador jugadorActual = partida.jugadorActual();
        if (!jugadorActual.id().equals(request.jugadorId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No es el turno de este jugador.");
        }

        // 5. Construcción y evaluación del evento
        TiroEvent evento = construirEventoValidado(partida, request, jugadorActual);
        ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
        ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);

        try {
            // 6. Mutación del estado en memoria
            partida.aplicarTiro(evento, resultado);
            if (resultado.esVictoria()) {
                partida.finalizar(evento.jugador());
            } else if (resultado.esDerrota()) {
                Jugador oponente = partida.getEquipoA().contiene(evento.jugador()) 
                    ? partida.getEquipoB().getJugadores().get(0) 
                    : partida.getEquipoA().getJugadores().get(0);
                partida.finalizar(oponente);
            }

            // 7. Persistencia en BD (Aquí es donde JPA @Version detecta conflictos de concurrencia)
            PartidaEntity entityNueva = persistenceMapper.toEntity(partida);
            entityNueva.setVersion(versionCargada);
            storage.guardar(entityNueva);

            // 8. Invalidar caché tras commit exitoso
            invalidarCacheTrasCommit(partidaId);

            // 9. Publicar evento WebSocket (el listener lo emite AFTER_COMMIT)
            try {
                String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
                eventPublisher.publishEvent(new PartidaActualizadaEvent(partidaId, json));
            } catch (Exception e) {
                log.warn("Error serializando partida {} para WebSocket", partidaId, e);
            }

            return resultado;

        } catch (OptimisticLockingFailureException e) {
            // Conflicto de concurrencia detectado por JPA: otro hilo/instancia modificó la partida
            partidasEnMemoria.remove(partidaId);
            log.warn("Conflicto de versión en partida {}. Caché invalidada.", partidaId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida fue modificada por otro proceso. Intente de nuevo.");
        } catch (RuntimeException e) {
            partidasEnMemoria.remove(partidaId);
            log.error("Fallo al registrar tiro en partida {}. Caché invalidada.", partidaId, e);
            throw e;
        } catch (Exception e) {
            partidasEnMemoria.remove(partidaId);
            log.error("Error inesperado al registrar tiro en partida {}.", partidaId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al registrar tiro: " + e.getMessage());
        }
    }

    private void registrarInvalidacionEnRollback(String partidaId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    partidasEnMemoria.remove(partidaId);
                    log.warn("Transacción no confirmada para partida {}. Caché invalidada.", partidaId);
                }
            }
        });
    }

    private void invalidarCacheTrasCommit(String partidaId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    partidasEnMemoria.remove(partidaId);
                }
            });
        } else {
            partidasEnMemoria.remove(partidaId);
        }
    }

    private void publicarEnCacheTrasCommit(String partidaId, Partida partida) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    partidasEnMemoria.put(partidaId, partida);
                }
            });
        } else {
            partidasEnMemoria.put(partidaId, partida);
        }
    }

    private boolean mismoPayload(TiroEventRequest request, TiroEvent original) {
        if (!Objects.equals(request.jugadorId(), original.jugador().id())) return false;
        Integer primeraOriginal = original.primeraBolaTocada() != null ? original.primeraBolaTocada().numero() : null;
        if (!Objects.equals(request.primeraBolaTocada(), primeraOriginal)) return false;
        
        List<Integer> bolasOriginal = original.bolasEmbolsadas() == null ? List.of() : original.bolasEmbolsadas().stream().map(Bola::numero).toList();
        List<Integer> bolasRequest = request.bolasEmbolsadas() == null ? List.of() : request.bolasEmbolsadas();
        if (!bolasOriginal.equals(bolasRequest)) return false;
        
        List<String> tronerasOriginal = original.troneras() == null ? List.of() : original.troneras().stream().map(Enum::name).toList();
        List<String> tronerasRequest = request.troneras() == null ? List.of() : request.troneras().stream().map(t -> t != null ? t.toUpperCase(Locale.ROOT) : null).toList();
        if (tronerasRequest.contains(null) || !tronerasOriginal.equals(tronerasRequest)) return false;
        
        long duracionOriginal = original.duracion() != null ? original.duracion().getSeconds() : 5L;
        long duracionRequest = request.duracionSegundos() != null ? request.duracionSegundos() : 5L;
        return duracionOriginal == duracionRequest;
    }

    private TiroEvent construirEventoValidado(Partida partida, TiroEventRequest request, Jugador jugadorActual) {
        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            primeraTocada = partida.getMesa().buscarBola(request.primeraBolaTocada()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola no existe."));
            if (!primeraTocada.enMesa()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola ya no está en la mesa.");
        }

        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null && !request.bolasEmbolsadas().isEmpty()) {
            Set<Integer> unicas = new HashSet<>();
            for (Integer n : request.bolasEmbolsadas()) {
                if (n == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola nula.");
                if (!unicas.add(n)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola duplicada en el request.");
                Bola b = partida.getMesa().buscarBola(n).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola inexistente."));
                if (!b.enMesa()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola ya embolsada previamente.");
                embolsadas.add(b);
            }
        }

        List<TroneraNombre> troneras = new ArrayList<>();
        if (request.troneras() != null) {
            if (request.troneras().size() != embolsadas.size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de troneras no coincide con las bolas embolsadas.");
            for (String nombre : request.troneras()) {
                if (nombre == null || nombre.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de tronera nulo o vacío.");
                try { 
                    troneras.add(TroneraNombre.valueOf(nombre.toUpperCase(Locale.ROOT))); 
                } catch (IllegalArgumentException e) { 
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tronera inválida: " + nombre); 
                }
            }
        } else if (!embolsadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan especificar las troneras para las bolas embolsadas.");
        }

        Duration duracion = request.duracionSegundos() != null ? Duration.ofSeconds(request.duracionSegundos()) : Duration.ofSeconds(5);
        return new TiroEvent(request.eventId(), partida.getId(), jugadorActual, primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }
}