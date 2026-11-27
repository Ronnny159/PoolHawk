package com.poolhawk.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.CrearPartidaRapidaRequest;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
import java.util.concurrent.ConcurrentMap;
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
    // Estructura: partidaId -> CacheEntry (partida + timestamp de última acceso)
    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // TTL de 30 minutos para entradas de caché
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    // Limpieza cada 10 minutos
    private static final long CLEANUP_INTERVAL_MS = 10 * 60 * 1000;

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

    /**
     * Limpieza periódica de entradas expiradas del caché.
     * Se ejecuta cada 10 minutos para eliminar partidas no accedidas en 30 minutos.
     */
    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
    public void limpiarCacheExpirado() {
        Instant ahora = Instant.now();
        int eliminadas = 0;
        
        for (var entry : cache.entrySet()) {
            if (Duration.between(entry.getValue().ultimoAcceso(), ahora).compareTo(CACHE_TTL) > 0) {
                if (cache.remove(entry.getKey(), entry.getValue())) {
                    eliminadas++;
                }
            }
        }
        
        if (eliminadas > 0) {
            log.debug("Limpieza de caché: {} entradas expiradas eliminadas", eliminadas);
        }
    }

    // Clase interna para almacenar la partida y su timestamp de último acceso
    private record CacheEntry(Partida partida, Instant ultimoAcceso) {
        CacheEntry(Partida partida) {
            this(partida, Instant.now());
        }
    }

    private Partida obtenerDeCache(String id) {
        CacheEntry entry = cache.get(id);
        if (entry != null) {
            // Actualizar timestamp de último acceso
            cache.put(id, new CacheEntry(entry.partida(), Instant.now()));
            return entry.partida();
        }
        return null;
    }

    private void ponerEnCache(String id, Partida partida) {
        cache.put(id, new CacheEntry(partida));
    }

    private void removerDeCache(String id) {
        cache.remove(id);
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

    @Transactional
    public Partida crearRapidaParaVideo(String jugador1, String jugador2) {
        if (jugador1 == null || jugador1.isBlank() || jugador2 == null || jugador2.isBlank()) {
            throw new IllegalArgumentException("Ambos nombres son obligatorios.");
        }
        
        CrearPartidaRequest request = new CrearPartidaRequest(
            com.poolhawk.domain.match.TipoModalidad.POR_PUNTOS,
            com.poolhawk.domain.match.FormatoJuego.INDIVIDUAL,
            List.of(jugador1.trim(), jugador2.trim()),
            254.0, // mesa 9 pies (Double)
            127.0  // mesa 9 pies (Double)
        );
        
        return crear(request);
    }

    @Transactional(readOnly = true)
    public Partida buscar(String id) {
        Partida cacheada = obtenerDeCache(id);
        if (cacheada != null) {
            return cacheada;
        }
        
        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));
        Partida reconstruida = persistenceMapper.toDomain(entity);
        ponerEnCache(id, reconstruida);
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
            log.warn("Validación fallida en registrarTiro [partidaId={}]: El eventId es obligatorio", partidaId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventId es obligatorio.");
        }

        log.info("Registrando tiro: partidaId={}, eventId={}, jugadorId={}, primeraBolaTocada={}, bolasEmbolsadas={}, troneras={}, duracionSegundos={}, versionEsperada={}",
                partidaId, request.eventId(), request.jugadorId(), 
                request.primeraBolaTocada(), request.bolasEmbolsadas(), 
                request.troneras(), request.duracionSegundos(), request.versionEsperada());

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
                log.warn("Validación fallida en registrarTiro [partidaId={}]: eventId {} ya usado con payload diferente", 
                        partidaId, request.eventId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventId ya fue utilizado con otro payload.");
            }
            log.info("Tiro idempotente detectado, devolviendo resultado original: eventId={}", request.eventId());
            return resultadoOriginal.get();
        }

        // 3. Validación de versión esperada (Optimistic Locking a nivel de aplicación)
        if (request.versionEsperada() != null
                && request.versionEsperada().intValue() != partida.getEstado().cantidadTiros()) {
            log.warn("Validación fallida en registrarTiro [partidaId={}]: Conflicto de versión. Esperada: {}, Actual: {}", 
                    partidaId, request.versionEsperada(), partida.getEstado().cantidadTiros());
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Estado desactualizado. Versión esperada: " + request.versionEsperada() + 
                ", versión actual en BD: " + partida.getEstado().cantidadTiros());
        }

        // 4. Validación de turno (Auditoría: evitar que un jugador tire por otro)
        Jugador jugadorActual = partida.jugadorActual();
        if (!jugadorActual.id().equals(request.jugadorId())) {
            log.warn("Validación fallida en registrarTiro [partidaId={}]: No es turno del jugador {}. Turno actual: {}", 
                    partidaId, request.jugadorId(), jugadorActual.id());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No es el turno de este jugador.");
        }

        // 5. Construcción y evaluación del evento
        TiroEvent evento;
        try {
            evento = construirEventoValidado(partida, request, jugadorActual);
            log.debug("Evento construido exitosamente: eventId={}, primeraBola={}, embolsadas={}, troneras={}", 
                    evento.id(), evento.primeraBolaTocada() != null ? evento.primeraBolaTocada().numero() : null,
                    evento.bolasEmbolsadas().stream().map(Bola::numero).toList(),
                    evento.troneras());
        } catch (ResponseStatusException e) {
            log.warn("Validación fallida al construir evento [partidaId={}]: {}", partidaId, e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al construir evento [partidaId={}]: {}", partidaId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error construyendo el evento: " + e.getMessage());
        }

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
            removerDeCache(partidaId);
            log.warn("Conflicto de versión en partida {}. Caché invalidada.", partidaId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida fue modificada por otro proceso. Intente de nuevo.");
        } catch (RuntimeException e) {
            removerDeCache(partidaId);
            log.error("Fallo al registrar tiro en partida {}. Caché invalidada.", partidaId, e);
            throw e;
        } catch (Exception e) {
            removerDeCache(partidaId);
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
                    removerDeCache(partidaId);
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
                    removerDeCache(partidaId);
                }
            });
        } else {
            removerDeCache(partidaId);
        }
    }

    private void publicarEnCacheTrasCommit(String partidaId, Partida partida) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ponerEnCache(partidaId, partida);
                }
            });
        } else {
            ponerEnCache(partidaId, partida);
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
            // Si no se especifican troneras pero hay bolas embolsadas (análisis de video sin calibración)
            // Asignar tronera DESCONOCIDA a cada bola
            for (int i = 0; i < embolsadas.size(); i++) {
                troneras.add(TroneraNombre.DESCONOCIDA);
            }
        }

        Duration duracion = request.duracionSegundos() != null ? Duration.ofSeconds(request.duracionSegundos()) : Duration.ofSeconds(5);
        return new TiroEvent(request.eventId(), partida.getId(), jugadorActual, primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }

    /**
     * Corrige el último tiro de la partida.
     * 
     * @param partidaId id de la partida
     * @param request datos corregidos del tiro
     * @return el veredicto recalculado
     */
    @Transactional
    public ResultadoTiro corregirUltimoTiro(String partidaId, TiroEventRequest request) {
        log.info("Corrigiendo último tiro: partidaId={}, eventId={}, jugadorId={}", 
                partidaId, request.eventId(), request.jugadorId());
        
        registrarInvalidacionEnRollback(partidaId);
        
        // 1. Cargar snapshot fresco desde BD
        PartidaEntity entityCargada = storage.buscar(partidaId)
            .orElseThrow(() -> new PartidaNoEncontradaException(partidaId));
        long versionCargada = entityCargada.getVersion();
        Partida partida = persistenceMapper.toDomain(entityCargada);
        
        // 2. Verificar que hay al menos un tiro para corregir
        if (partida.getEstado().getHistorial().isEmpty()) {
            log.warn("Corrección fallida [partidaId={}]: No hay tiros registrados para corregir", partidaId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No hay tiros registrados para corregir.");
        }
        
        // 3. Obtener el último evento y su resultado
        TiroEvent ultimoEvento = partida.getEstado().getHistorial().get(partida.getEstado().getHistorial().size() - 1);
        String ultimoEventId = ultimoEvento.id();
        log.debug("Último evento a corregir: eventId={}, jugador={}", ultimoEventId, ultimoEvento.jugador().id());
        
        // 4. Verificar que el eventoId del request coincide con "CORRECCION:" pattern
        if (!request.eventId().startsWith("CORRECCION:")) {
            log.warn("Corrección fallida [partidaId={}]: EventId inválido '{}', debe empezar con 'CORRECCION:'", 
                    partidaId, request.eventId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EventId de corrección inválido.");
        }
        
        // 5. Restaurar el estado previo al último tiro
        partida.getEstado().restaurarEstadoPrevio();
        log.debug("Estado previo restaurado para partida {}", partidaId);
        
        // 6. Eliminar el resultado del último evento (para permitir re-registro con nuevo eventId)
        partida.getEstado().removerResultadoPorEvento(ultimoEventId);
        
        // 7. Validar versión esperada
        if (request.versionEsperada() != null
                && request.versionEsperada().intValue() != partida.getEstado().cantidadTiros()) {
            log.warn("Corrección fallida [partidaId={}]: Conflicto de versión. Esperada: {}, Actual: {}", 
                    partidaId, request.versionEsperada(), partida.getEstado().cantidadTiros());
            throw new ResponseStatusException(HttpStatus.CONFLICT, 
                "Estado desactualizado. Versión esperada: " + request.versionEsperada() + 
                ", versión actual en BD: " + partida.getEstado().cantidadTiros());
        }
        
        // 8. Validar turno (debe ser el mismo jugador que tiró originalmente)
        Jugador jugadorActual = partida.jugadorActual();
        if (!jugadorActual.id().equals(request.jugadorId())) {
            log.warn("Corrección fallida [partidaId={}]: No es turno del jugador {}. Turno actual: {}", 
                    partidaId, request.jugadorId(), jugadorActual.id());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No es el turno de este jugador para la corrección.");
        }
        
        // 9. Construir y evaluar el evento corregido
        TiroEvent eventoCorregido;
        try {
            eventoCorregido = construirEventoValidado(partida, request, jugadorActual);
            log.debug("Evento corregido construido: eventId={}, primeraBola={}, embolsadas={}, troneras={}", 
                    eventoCorregido.id(), 
                    eventoCorregido.primeraBolaTocada() != null ? eventoCorregido.primeraBolaTocada().numero() : null,
                    eventoCorregido.bolasEmbolsadas().stream().map(Bola::numero).toList(),
                    eventoCorregido.troneras());
        } catch (ResponseStatusException e) {
            log.warn("Validación fallida al construir evento corregido [partidaId={}]: {}", partidaId, e.getReason());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al construir evento corregido [partidaId={}]: {}", partidaId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error construyendo el evento corregido: " + e.getMessage());
        }
        
        ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
        ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), eventoCorregido);
        
        try {
            // 10. Aplicar el tiro corregido
            partida.aplicarTiro(eventoCorregido, resultado);
            if (resultado.esVictoria()) {
                partida.finalizar(eventoCorregido.jugador());
            } else if (resultado.esDerrota()) {
                Jugador oponente = partida.getEquipoA().contiene(eventoCorregido.jugador()) 
                    ? partida.getEquipoB().getJugadores().get(0) 
                    : partida.getEquipoA().getJugadores().get(0);
                partida.finalizar(oponente);
            }
            
            // 11. Persistencia
            PartidaEntity entityNueva = persistenceMapper.toEntity(partida);
            entityNueva.setVersion(versionCargada);
            storage.guardar(entityNueva);
            
            // 12. Invalidar caché y publicar WebSocket
            invalidarCacheTrasCommit(partidaId);
            
            try {
                String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
                eventPublisher.publishEvent(new PartidaActualizadaEvent(partidaId, json));
            } catch (Exception e) {
                log.warn("Error serializando partida {} para WebSocket", partidaId, e);
            }
            
            log.info("Corrección exitosa en partida {}: eventId={}, resultado={}", 
                    partidaId, request.eventId(), resultado.descripcion());
            return resultado;
            
        } catch (OptimisticLockingFailureException e) {
            removerDeCache(partidaId);
            log.warn("Conflicto de versión en partida {}. Caché invalidada.", partidaId);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida fue modificada por otro proceso. Intente de nuevo.");
        } catch (RuntimeException e) {
            removerDeCache(partidaId);
            log.error("Fallo al corregir tiro en partida {}. Caché invalidada.", partidaId, e);
            throw e;
        } catch (Exception e) {
            removerDeCache(partidaId);
            log.error("Error inesperado al corregir tiro en partida {}.", partidaId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al corregir tiro: " + e.getMessage());
        }
    }
}