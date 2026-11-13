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

    /**
     * Registra un tiro aplicando idempotencia <b>transparente</b>:
     * <ul>
     *   <li>Si el {@code eventId} ya fue procesado con el mismo payload, se devuelve el
     *       {@link ResultadoTiro} original con HTTP 200 y <b>sin mutar el estado</b>.</li>
     *   <li>Si el {@code eventId} ya fue procesado con un payload diferente, se responde
     *       HTTP 400 (Bad Request).</li>
     *   <li>Si es un {@code eventId} nuevo, se evalúa, aplica, persiste y emite por WebSocket.</li>
     * </ul>
     *
     * @param partidaId identificador de la partida
     * @param request   payload del tiro
     * @return veredicto del motor de reglas (original o recién calculado)
     */
    @Transactional
    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        Object lock = locksPorPartida.computeIfAbsent(partidaId, k -> new Object());

        synchronized (lock) {
            Partida partida = buscar(partidaId);

            // 1. IDEMPOTENCIA TRANSPARENTE
            if (request.eventId() == null || request.eventId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El eventId es obligatorio para garantizar idempotencia.");
            }

            Optional<ResultadoTiro> resultadoOriginal =
                partida.getEstado().buscarResultadoPorEvento(request.eventId());

            if (resultadoOriginal.isPresent()) {
                TiroEvent tiroOriginal = partida.getEstado().getHistorial().stream()
                    .filter(e -> e.id().equals(request.eventId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                        "Inconsistencia de idempotencia: resultado registrado sin evento."));

                if (!mismoPayload(request, tiroOriginal)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El eventId ya fue utilizado con un payload diferente.");
                }

                // Reintento idéntico: 200 OK con el veredicto original, sin tocar estado.
                return resultadoOriginal.get();
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

            if (entityExistente != null) {
                entityNueva.setVersion(entityExistente.getVersion());
            }

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

    /**
     * Compara el payload del request con el {@link TiroEvent} originalmente procesado.
     * Considera identidad si coinciden jugador, primera bola, bolas embolsadas,
     * troneras y duración.
     *
     * <p>Regla estricta de idempotencia (H-03): <b>no se normalizan</b> valores nulos.
     * Un reintento que incluya {@code null} en {@code troneras} se considera un payload
     * distinto al original (que fue validado y no puede contener {@code null}), y por
     * tanto se rechaza con 400. Aceptar silenciosamente un payload inválido como
     * "idéntico" abriría la puerta a reintentos con datos divergentes.</p>
     *
     * @param request  payload recibido en el reintento
     * @param original evento almacenado en el historial
     * @return {@code true} si representan el mismo tiro
     */
    private boolean mismoPayload(TiroEventRequest request, TiroEvent original) {
        if (!Objects.equals(request.jugadorId(), original.jugador().id())) {
            return false;
        }

        Integer primeraOriginal = original.primeraBolaTocada() != null
            ? original.primeraBolaTocada().numero()
            : null;
        if (!Objects.equals(request.primeraBolaTocada(), primeraOriginal)) {
            return false;
        }

        List<Integer> bolasOriginal = original.bolasEmbolsadas() == null
            ? List.of()
            : original.bolasEmbolsadas().stream().map(Bola::numero).toList();
        List<Integer> bolasRequest = request.bolasEmbolsadas() == null
            ? List.of()
            : request.bolasEmbolsadas();
        if (!bolasOriginal.equals(bolasRequest)) {
            return false;
        }

        // H-03: comparación estricta de troneras, sin filtrado de nulos.
        List<String> tronerasOriginal = original.troneras() == null
            ? List.of()
            : original.troneras().stream().map(Enum::name).toList();

        List<String> tronerasRequest;
        if (request.troneras() == null) {
            tronerasRequest = List.of();
        } else {
            tronerasRequest = new ArrayList<>(request.troneras().size());
            for (String t : request.troneras()) {
                if (t == null) {
                    // Un null en el reintento nunca puede ser equivalente al original,
                    // que fue validado y no admite nulos.
                    return false;
                }
                tronerasRequest.add(t.toUpperCase());
            }
        }
        if (!tronerasOriginal.equals(tronerasRequest)) {
            return false;
        }

        long duracionOriginal = original.duracion() != null ? original.duracion().getSeconds() : 5L;
        long duracionRequest = request.duracionSegundos() != null ? request.duracionSegundos() : 5L;
        return duracionOriginal == duracionRequest;
    }

    private TiroEvent construirEventoValidado(Partida partida, TiroEventRequest request, Jugador jugadorActual) {
        String eventId = request.eventId();

        // Validar primera bola tocada
        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            primeraTocada = partida.getMesa().buscarBola(request.primeraBolaTocada())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola no existe."));
            if (!primeraTocada.enMesa()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La bola ya no está en la mesa.");
            }
        }

        // Validar bolas embolsadas (incluyendo nulls y duplicados)
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

        // Validar troneras
        List<TroneraNombre> troneras = new ArrayList<>();
        if (request.troneras() != null) {
            if (request.troneras().size() != embolsadas.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Troneras no coinciden con bolas embolsadas.");
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan las troneras.");
        }

        Duration duracion = request.duracionSegundos() != null ?
            Duration.ofSeconds(request.duracionSegundos()) : Duration.ofSeconds(5);

        return new TiroEvent(eventId, partida.getId(), jugadorActual,
            primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }
}