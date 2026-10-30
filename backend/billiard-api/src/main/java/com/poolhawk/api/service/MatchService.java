package com.poolhawk.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.*;
import com.poolhawk.rules.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que orquesta la logica de partidas.
 *
 * <p>Es el punto donde convergen el dominio, las reglas y la API.
 * Recibe las peticiones REST, construye los objetos de dominio,
 * consulta las reglas y aplica los resultados al estado.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Service
public class MatchService {
    private final MatchStorage storage;
    private final MatchLiveHandler liveHandler;
    private final PartidaMapper mapper;
    
    // ObjectMapper con el módulo para fechas (Instant) y configuración tolerante
    private final ObjectMapper jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    private final Map<String, Partida> partidasEnMemoria = new HashMap<>();

    public MatchService(MatchStorage storage, MatchLiveHandler liveHandler, PartidaMapper mapper) {
        this.storage = storage;
        this.liveHandler = liveHandler;
        this.mapper = mapper;
    }

    public Partida crear(CrearPartidaRequest request) {
        String id = UUID.randomUUID().toString();
        Equipo equipoA = construirEquipo("eA-" + id, request.equipoA());
        Equipo equipoB = construirEquipo("eB-" + id, request.equipoB());
        Mesa mesa = construirMesaInicial();

        Partida partida = new Partida(id, request.modalidad(), equipoA, equipoB, mesa, Instant.now());
        partida.iniciar();
        partidasEnMemoria.put(id, partida);

        PartidaEntity entity = toEntity(partida);
        storage.guardar(entity);
        return partida;
    }

    @Transactional(readOnly = true)
    public Partida buscar(String id) {
        // 1. Primero buscamos en memoria
        Partida partida = partidasEnMemoria.get(id);
        if (partida != null) return partida;

        // 2. Si no está, buscamos en BD y reconstruimos
        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));

        return fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public List<Partida> listarTodas() {
        return storage.listarTodas().stream()
            .map(this::fromEntity)
            .collect(Collectors.toList());
    }

    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        Partida partida = buscar(partidaId);
        TiroEvent evento = construirEvento(partida, request);
        ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
        ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);
        
        partida.aplicarTiro(evento, resultado);
        
        if (resultado.esVictoria()) {
            partida.finalizar(evento.jugador());
        }
        
        // Guardamos en BD (toEntity ahora también guarda el JSON de la mesa)
        storage.guardar(toEntity(partida));

        // Enviar por WebSocket
        try {
            String json = jsonMapper.writeValueAsString(mapper.toResponse(partida));
            liveHandler.broadcast(json);
        } catch (Exception e) {
            System.err.println("Error serializando partida para WebSocket: " + e.getMessage());
        }

        return resultado;
    }

    // ───────────────────────────────────────────────────────────
    // Helpers de conversión (Entity <-> Domain)
    // ───────────────────────────────────────────────────────────

    private Partida fromEntity(PartidaEntity entity) {
        // 1. Reconstruir jugadores
        List<Jugador> jugadoresA = entity.getJugadores().stream()
            .filter(j -> "A".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();
        List<Jugador> jugadoresB = entity.getJugadores().stream()
            .filter(j -> "B".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();

        // 2. Reconstruir equipos
        String nombreA = jugadoresA.isEmpty() ? "Equipo A" : jugadoresA.get(0).nombre();
        Equipo equipoA = new Equipo("eA-" + entity.getId(), nombreA);
        jugadoresA.forEach(equipoA::agregarJugador);

        String nombreB = jugadoresB.isEmpty() ? "Equipo B" : jugadoresB.get(0).nombre();
        Equipo equipoB = new Equipo("eB-" + entity.getId(), nombreB);
        jugadoresB.forEach(equipoB::agregarJugador);

        // 3. Reconstruir la mesa desde el JSON guardado
        Mesa mesa = new Mesa();
        if (entity.getEstadoMesaJson() != null && !entity.getEstadoMesaJson().isEmpty()) {
            try {
                mesa = jsonMapper.readValue(entity.getEstadoMesaJson(), Mesa.class);
            } catch (Exception e) {
                System.err.println("Error leyendo JSON de mesa: " + e.getMessage());
            }
        }

        // 4. Crear la partida
        Partida partida = new Partida(
            entity.getId(),
            TipoModalidad.valueOf(entity.getModalidad()),
            equipoA,
            equipoB,
            mesa,
            entity.getFechaCreacion()
        );
        
        return partida;
    }

    private PartidaEntity toEntity(Partida partida) {
        PartidaEntity entity = new PartidaEntity();
        entity.setId(partida.getId());
        entity.setModalidad(partida.getModalidad().name());
        entity.setFase(partida.getFase().name());
        entity.setFechaCreacion(partida.getFechaCreacion());
        entity.setGanadorId(partida.getGanador() != null ? partida.getGanador().id() : null);

        // CORREGIDO: La mesa se obtiene directamente de la partida
        try {
            entity.setEstadoMesaJson(jsonMapper.writeValueAsString(partida.getMesa()));
        } catch (Exception e) {
            System.err.println("Error guardando JSON de mesa: " + e.getMessage());
        }

        List<JugadorEntity> jugadores = new ArrayList<>();
        for (Jugador j : partida.getEquipoA().getJugadores()) {
            jugadores.add(toJugadorEntity(j, "A", entity));
        }
        for (Jugador j : partida.getEquipoB().getJugadores()) {
            jugadores.add(toJugadorEntity(j, "B", entity));
        }
        entity.setJugadores(jugadores);
        return entity;
    }

    private JugadorEntity toJugadorEntity(Jugador j, String equipo, PartidaEntity partida) {
        JugadorEntity je = new JugadorEntity();
        je.setId(j.id());
        je.setNombre(j.nombre());
        je.setEquipo(equipo);
        je.setPartida(partida);
        return je;
    }

    private Equipo construirEquipo(String id, List<String> nombres) {
        if (nombres.size() == 1) {
            Jugador j = new Jugador(id + "-j1", nombres.get(0));
            return Equipo.individual(id, nombres.get(0), j);
        }
        if (nombres.size() == 2) {
            Jugador j1 = new Jugador(id + "-j1", nombres.get(0));
            Jugador j2 = new Jugador(id + "-j2", nombres.get(1));
            return Equipo.pareja(id, nombres.get(0) + " & " + nombres.get(1), j1, j2);
        }
        throw new IllegalArgumentException("Un equipo debe tener 1 o 2 jugadores.");
    }

    private Mesa construirMesaInicial() {
        Mesa mesa = new Mesa();
        mesa.agregarBola(Bola.crear(0, new Coordenada(63, 63)));
        double cx = 190, cy = 63, dx = 6;
        int[] numeros = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};
        int idx = 0;
        for (int fila = 0; fila < 5; fila++) {
            for (int col = 0; col <= fila; col++) {
                double x = cx + fila * dx;
                double y = cy - fila * 3 + col * 6;
                mesa.agregarBola(Bola.crear(numeros[idx++], new Coordenada(x, y)));
            }
        }
        return mesa;
    }

    private TiroEvent construirEvento(Partida partida, TiroEventRequest request) {
        Jugador jugador = partida.getEstado().getTurnos().jugadores().stream()
            .filter(j -> j.id().equals(request.jugadorId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));

        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            // CORREGIDO: La mesa se obtiene de la partida, no del estado
            primeraTocada = partida.getMesa().buscarBola(request.primeraBolaTocada()).orElse(null);
        }

        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null) {
            for (Integer numero : request.bolasEmbolsadas()) {
                // CORREGIDO: La mesa se obtiene de la partida, no del estado
                partida.getMesa().buscarBola(numero).ifPresent(embolsadas::add);
            }
        }

        List<TroneraNombre> troneras = new ArrayList<>();
        if (request.troneras() != null) {
            for (String nombre : request.troneras()) {
                troneras.add(TroneraNombre.valueOf(nombre));
            }
        }

        Duration duracion = request.duracionSegundos() != null
            ? Duration.ofSeconds(request.duracionSegundos())
            : Duration.ofSeconds(5);

        return new TiroEvent(UUID.randomUUID().toString(), partida.getId(), jugador,
            primeraTocada, embolsadas, troneras, Instant.now(), duracion);
    }
}