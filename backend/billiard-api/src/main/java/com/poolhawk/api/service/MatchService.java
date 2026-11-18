package com.poolhawk.api.service;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
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

/**
 * Servicio de aplicación para el ciclo de vida de las partidas y el registro de tiros.
 *
 * <p><b>Modelo de concurrencia:</b> no se utilizan bloqueos de aplicación
 * ({@code synchronized} ni mapas de locks). El único árbitro de escrituras
 * concurrentes es el bloqueo optimista de JPA ({@code @Version} en
 * {@link PartidaEntity}). Esto elimina la fuga de memoria por locks acumulados
 * y permite que dos transacciones compitan realmente por la misma versión.</p>
 *
 * <p><b>Aislamiento de estado:</b> la ruta de escritura SIEMPRE carga un snapshot
 * fresco desde la base de datos, de modo que cada transacción muta su propia
 * copia del dominio. La caché {@code partidasEnMemoria} se usa únicamente como
 * atajo de lectura y se actualiza tras un commit exitoso, o se invalida si la
 * transacción termina en rollback.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Service
public class MatchService {

    private static final Logger log = LoggerFactory.getLogger(MatchService.class);

    private final MatchStorage storage;
    private final PartidaMapper responseMapper;
    private final MatchPersistenceMapper persistenceMapper;
    private final MatchFactory matchFactory;
    private final ObjectMapper jsonMapper;
    private final ApplicationEventPublisher eventPublisher;

    /** Caché de lectura. Nunca se comparte como objeto mutable entre escritores. */
    private final Map<String, Partida> partidasEnMemoria = new ConcurrentHashMap<>();

    /**
     * Construye el servicio con sus colaboradores.
     *
     * @param storage           almacenamiento transaccional de partidas.
     * @param responseMapper    mapper de dominio a DTO de respuesta.
     * @param persistenceMapper mapper de dominio a entidades JPA y viceversa.
     * @param matchFactory      fábrica de partidas de dominio.
     * @param jsonMapper        serializador JSON para eventos WebSocket.
     * @param eventPublisher    publicador de eventos de aplicación.
     */
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
     * Crea una partida nueva y la persiste.
     *
     * @param request configuración de la partida (modalidad, formato, jugadores, mesa).
     * @return la partida creada.
     * @throws IllegalArgumentException si la configuración es incompleta o incoherente.
     */
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

    /**
     * Busca una partida por identificador, usando la caché de lectura si está disponible.
     *
     * @param id identificador de la partida.
     * @return la partida encontrada.
     * @throws PartidaNoEncontradaException si no existe en caché ni en base de datos.
     */
    @Transactional(readOnly = true)
    public Partida buscar(String id) {
        Partida cacheada = partidasEnMemoria.get(id);
        if (cacheada != null) {
            return cacheada;
        }
        PartidaEntity entity = storage.buscarConHistorial(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));
        return persistenceMapper.toDomain(entity);
    }

    /**
     * Lista todas las partidas persistidas con su historial.
     *
     * @return lista de partidas reconstruidas desde la base de datos.
     */
    @Transactional(readOnly = true)
    public List<Partida> listarTodas() {
        return storage.listarTodasConHistorial().stream()
            .map(persistenceMapper::toDomain)
            .collect(Collectors.toList());
    }

    /**
     * Registra un tiro de forma idempotente y transaccional.
     *
     * <p>La invalidación de caché se registra <b>antes</b> de mutar el estado, para
     * cubrir tanto los fallos inmediatos como los que ocurren durante el commit
     * (fuera del alcance del {@code try/catch} de este método).</p>
     *
     * @param partidaId identificador de la partida.
     * @param request   evento de tiro enviado por el cliente.
     * @return el veredicto del motor de reglas, o el original si el {@code eventId} ya fue procesado.
     * @throws ResponseStatusException 400 si el payload es inválido o el {@code eventId} se reutilizó
     *                                 con otro contenido; 409 si otra transacción modificó la partida.
     */
    @Transactional
    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        if (request.eventId() == null || request.eventId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El eventId es obligatorio.");
        }

        // Se registra PRIMERO: si el commit falla, la caché queda invalidada igual.
        registrarInvalidacionEnRollback(partidaId);

        // Snapshot propio de esta transacción: nunca el objeto compartido de la caché.
        Partida partida = cargarParaEscritura(partidaId);

        Optional<ResultadoTiro> resultadoOriginal =
            partida.getEstado().buscarResultadoPorEvento(request.eventId());
        if (resultadoOriginal.isPresent()) {
            TiroEvent tiroOriginal = partida.getEstado().getHistorial().stream()
                .filter(e -> e.id().equals(request.eventId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Inconsistencia de idempotencia."));
            if (!mismoPayload(request, tiroOriginal)) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "El eventId ya fue utilizado con otro payload.");
            }
            return resultadoOriginal.get();
        }

        Jugador jugadorActual = partida.jugadorActual();
        if (!jugadorActual.id().equals(request.jugadorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No es el turno de este jugador.");
        }

        TiroEvent evento = construirEventoValidado(partida, request, jugadorActual);
        ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
        ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);

        try {
            partida.aplicarTiro(evento, resultado);
            if (resultado.esVictoria()) {
                partida.finalizar(evento.jugador());
            } else if (resultado.esDerrota()) {
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

            publicarActualizacion(partidaId, partida);
            actualizarCacheTrasCommit(partidaId, partida);
            return resultado;

        } catch (OptimisticLockingFailureException | OptimisticLockException e) {
            partidasEnMemoria.remove(partidaId);
            log.warn("Conflicto de versión en partida {}. Caché invalidada.", partidaId);
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La partida fue modificada por otro proceso. Intente de nuevo.");

        } catch (RuntimeException e) {
            partidasEnMemoria.remove(partidaId);
            log.error("Fallo al registrar tiro en partida {}. Caché invalidada.", partidaId, e);
            throw e;
        }
    }

    /**
     * Carga un snapshot independiente para la transacción de escritura actual.
     * Si la partida aún no existe en base de datos (escenarios de prueba con
     * almacenamiento simulado), recae en la instancia de la caché.
     *
     * @param partidaId identificador de la partida.
     * @return instancia de dominio propia de esta transacción.
     */
    private Partida cargarParaEscritura(String partidaId) {
        return storage.buscarConHistorial(partidaId)
            .map(persistenceMapper::toDomain)
            .orElseGet(() -> buscar(partidaId));
    }

    /**
     * Registra la invalidación de caché si la transacción no llega a confirmar.
     *
     * @param partidaId identificador de la partida afectada.
     */
    private void registrarInvalidacionEnRollback(String partidaId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
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

    /**
     * Refresca la caché de lectura únicamente después de un commit exitoso.
     *
     * @param partidaId identificador de la partida.
     * @param partida   estado confirmado.
     */
    private void actualizarCacheTrasCommit(String partidaId, Partida partida) {
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

    /**
     * Publica el evento de actualización. El listener WebSocket lo emite
     * después del commit ({@code @TransactionalEventListener}).
     *
     * @param partidaId identificador de la partida.
     * @param partida   estado a serializar.
     */
    private void publicarActualizacion(String partidaId, Partida partida) {
        try {
            String json = jsonMapper.writeValueAsString(responseMapper.toResponse(partida));
            eventPublisher.publishEvent(new PartidaActualizadaEvent(partidaId, json));
        } catch (Exception e) {
            log.warn("Error serializando partida {} para WebSocket", partidaId);
        }
    }

    /**
     * Compara el payload de un reintento con el tiro originalmente registrado.
     *
     * @param request  payload del reintento.
     * @param original tiro almacenado en el historial.
     * @return {@code true} si ambos payloads son equivalentes.
     */
    private boolean mismoPayload(TiroEventRequest request, TiroEvent original) {
        if (!Objects.equals(request.jugadorId(), original.jugador().id())) return false;

        Integer primeraOriginal = original.primeraBolaTocada() != null
            ? original.primeraBolaTocada().numero() : null;
        if (!Objects.equals(request.primeraBolaTocada(), primeraOriginal)) return false;

        List<Integer> bolasOriginal = original.bolasEmbolsadas() == null
            ? List.of() : original.bolasEmbolsadas().stream().map(Bola::numero).toList();
        List<Integer> bolasRequest = request.bolasEmbolsadas() == null
            ? List.of() : request.bolasEmbolsadas();
        if (!bolasOriginal.equals(bolasRequest)) return false;

        List<String> tronerasOriginal = original.troneras() == null
            ? List.of() : original.troneras().stream().map(Enum::name).toList();
        List<String> tronerasRequest = request.troneras() == null
            ? List.of()
            : request.troneras().stream()
                .map(t -> t != null ? t.toUpperCase(Locale.ROOT) : null).toList();
        if (tronerasRequest.contains(null) || !tronerasOriginal.equals(tronerasRequest)) return false;

        long duracionOriginal = original.duracion() != null ? original.duracion().getSeconds() : 5L;
        long duracionRequest = request.duracionSegundos() != null ? request.duracionSegundos() : 5L;
        return duracionOriginal == duracionRequest;
    }

    /**
     * Valida el payload del tiro y construye el evento de dominio.
     *
     * @param partida       partida sobre la que se tira.
     * @param request       payload del cliente.
     * @param jugadorActual jugador que tiene el turno.
     * @return evento de tiro validado.
     * @throws ResponseStatusException 400 si alguna validación falla.
     */
    private TiroEvent construirEventoValidado(Partida partida, TiroEventRequest request, Jugador jugadorActual) {
        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            primeraTocada = partida.getMesa().buscarBola(request.primeraBolaTocada())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola no existe."));
            if (!primeraTocada.enMesa()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola ya no está en la mesa.");
            }
        }

        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null && !request.bolasEmbolsadas().isEmpty()) {
            Set<Integer> unicas = new HashSet<>();
            for (Integer n : request.bolasEmbolsadas()) {
                if (n == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola nula.");
                }
                if (!unicas.add(n)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola duplicada.");
                }
                Bola b = partida.getMesa().buscarBola(n)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola inexistente."));
                if (!b.enMesa()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bola ya embolsada.");
                }
                embolsadas.add(b);
            }
        }

        List<TroneraNombre> troneras = new ArrayList<>();
        if (request.troneras() != null) {
            if (request.troneras().size() != embolsadas.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Troneras no coinciden.");
            }
            for (String nombre : request.troneras()) {
                if (nombre == null || nombre.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tronera nula.");
                }
                try {
                    troneras.add(TroneraNombre.valueOf(nombre.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tronera inválida: " + nombre);
                }
            }
        } else if (!embolsadas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan troneras.");
        }

        Duration duracion = request.duracionSegundos() != null
            ? Duration.ofSeconds(request.duracionSegundos()) : Duration.ofSeconds(5);

        return new TiroEvent(
            request.eventId(), partida.getId(), jugadorActual, primeraTocada,
            embolsadas, troneras, Instant.now(), duracion);
    }
}