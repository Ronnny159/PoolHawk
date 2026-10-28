package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.FasePartida;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.*;
import com.poolhawk.rules.*;
import org.springframework.stereotype.Service;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
    private final ObjectMapper jsonMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Map<String, Partida> partidasEnMemoria = new HashMap<>();

    public MatchService(MatchStorage storage, MatchLiveHandler liveHandler, PartidaMapper mapper) {
        this.storage = storage;
        this.liveHandler = liveHandler;
        this.mapper = mapper;
    }

    /**
     * Crea una nueva partida con sus jugadores y equipos.
     *
     * @param request datos de creacion
     * @return la partida creada
     */
    public Partida crear(CrearPartidaRequest request) {
        String id = UUID.randomUUID().toString();
        Equipo equipoA = construirEquipo("eA-" + id, request.equipoA());
        Equipo equipoB = construirEquipo("eB-" + id, request.equipoB());
        Mesa mesa = construirMesaInicial();

        Partida partida = new Partida(id, request.modalidad(), equipoA, equipoB, mesa, Instant.now());
        partida.iniciar();
        partidasEnMemoria.put(id, partida);

        // Guardar en BD
        PartidaEntity entity = toEntity(partida);
        storage.guardar(entity);
        return partida;
    }

    /**
     * Busca una partida por su id.
     *
     * @param id identificador de la partida
     * @return la partida encontrada
     * @throws PartidaNoEncontradaException si no existe
     */
       /**
     * Busca una partida por su id.
     *
     * @param id identificador de la partida
     * @return la partida encontrada
     * @throws PartidaNoEncontradaException si no existe
     */
    public Partida buscar(String id) {
        // 1. Si está en memoria, la devolvemos rápido (es la que está en juego ahora mismo)
        if (partidasEnMemoria.containsKey(id)) {
            return partidasEnMemoria.get(id);
        }

        // 2. Si no está (porque reiniciaste el server), la buscamos en PostgreSQL
        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));

        // 3. Reconstruimos la partida desde la BD
        List<Jugador> jugadoresA = entity.getJugadores().stream()
            .filter(j -> "A".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();
            
        List<Jugador> jugadoresB = entity.getJugadores().stream()
            .filter(j -> "B".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();

        Equipo equipoA = new Equipo("eA-" + entity.getId(), jugadoresA.isEmpty() ? "Equipo A" : jugadoresA.get(0).nombre());
        jugadoresA.forEach(equipoA::agregarJugador);

        Equipo equipoB = new Equipo("eB-" + entity.getId(), jugadoresB.isEmpty() ? "Equipo B" : jugadoresB.get(0).nombre());
        jugadoresB.forEach(equipoB::agregarJugador);

        // Creamos la mesa y la partida
        Mesa mesa = construirMesaInicial(); // Volvemos a armar la mesa
        Partida partida = new Partida(
            entity.getId(),
            TipoModalidad.valueOf(entity.getModalidad()),
            equipoA,
            equipoB,
            mesa,
            entity.getFechaCreacion()
        );

        // (Opcional) Si la partida en la BD estaba EN_CURSO, la iniciamos
        if (FasePartida.valueOf(entity.getFase()) == FasePartida.EN_CURSO) {
            partida.iniciar();
        }

        // La guardamos en memoria para que el siguiente tiro sea rápido
        partidasEnMemoria.put(id, partida);
        
        return partida;
    }
    /**
     * Lista todas las partidas almacenadas.
     *
     * @return lista de partidas
     */
    @Transactional(readOnly = true)
    public List<Partida> listarTodas() {
        List<PartidaEntity> entities = storage.listarTodas();
        
        return entities.stream().map(entity -> {
            // 1. Extraemos los jugadores de la entidad
            List<Jugador> jugadoresA = entity.getJugadores().stream()
                .filter(j -> "A".equals(j.getEquipo()))
                .map(j -> new Jugador(j.getId(), j.getNombre()))
                .toList();
                
            List<Jugador> jugadoresB = entity.getJugadores().stream()
                .filter(j -> "B".equals(j.getEquipo()))
                .map(j -> new Jugador(j.getId(), j.getNombre()))
                .toList();

            // 2. Creamos los equipos con el constructor de 2 argumentos
            String nombreA = jugadoresA.isEmpty() ? "Equipo A" : jugadoresA.get(0).nombre();
            Equipo equipoA = new Equipo("eA-" + entity.getId(), nombreA);
            jugadoresA.forEach(equipoA::agregarJugador); // Agregamos los jugadores uno por uno

            String nombreB = jugadoresB.isEmpty() ? "Equipo B" : jugadoresB.get(0).nombre();
            Equipo equipoB = new Equipo("eB-" + entity.getId(), nombreB);
            jugadoresB.forEach(equipoB::agregarJugador);

            // 3. Retornamos la partida
            return new Partida(
                entity.getId(),
                TipoModalidad.valueOf(entity.getModalidad()),
                equipoA,
                equipoB,
                new Mesa(), // Mesa vacía para el historial
                entity.getFechaCreacion()
            );
        }).collect(Collectors.toList());
    }

    /**
     * Registra un tiro y devuelve el resultado de su evaluacion.
     *
     * @param partidaId id de la partida
     * @param request   datos del tiro
     * @return el resultado del tiro segun las reglas
     */
    public ResultadoTiro registrarTiro(String partidaId, TiroEventRequest request) {
        Partida partida = buscar(partidaId);
        TiroEvent evento = construirEvento(partida, request);
        ReglasBillar reglas = ReglasFactory.crear(partida.getModalidad(), ReglasConfig.porDefecto());
        ResultadoTiro resultado = reglas.evaluar(partida.getEstado(), evento);
        
        partida.aplicarTiro(evento, resultado);
        
        if (resultado.esVictoria()) {
            partida.finalizar(evento.jugador());
        }
        
        // Actualizar en BD
        storage.guardar(toEntity(partida));

        // ✨ NUEVO: Emitir el estado actualizado por WebSocket
        try {
            String json = jsonMapper.writeValueAsString(mapper.toResponse(partida));
            liveHandler.broadcast(json);
        } catch (Exception e) {
            System.err.println("Error serializando partida para WebSocket: " + e.getMessage());
        }

        return resultado;
    }


    // ───────────────────────────────────────────────────────────
    // Helpers privados
    // ───────────────────────────────────────────────────────────

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
        throw new IllegalArgumentException(
            "Un equipo debe tener 1 o 2 jugadores. Recibidos: " + nombres.size());
    }

    /**
     * Construye una mesa con las 16 bolas en su posicion inicial estandar.
     *
     * <p>La blanca se coloca en el lado izquierdo. El triangulo de bolas
     * se arma en el lado derecho con la bola 8 en el centro.</p>
     */
    private Mesa construirMesaInicial() {
        Mesa mesa = new Mesa();
        mesa.agregarBola(Bola.crear(0, new Coordenada(63, 63)));

        double cx = 190, cy = 63;
        double dx = 6;
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
            .orElseThrow(() -> new IllegalArgumentException(
                "Jugador no encontrado en la partida: " + request.jugadorId()));

        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            primeraTocada = partida.getEstado().getMesa()
                .buscarBola(request.primeraBolaTocada())
                .orElse(null);
        }

        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null) {
            for (Integer numero : request.bolasEmbolsadas()) {
                partida.getEstado().getMesa().buscarBola(numero)
                    .ifPresent(embolsadas::add);
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

        return new TiroEvent(
            UUID.randomUUID().toString(),
            partida.getId(),
            jugador,
            primeraTocada,
            embolsadas,
            troneras,
            Instant.now(),
            duracion);
    }

    private JugadorEntity toJugadorEntity(Jugador j, String equipo, PartidaEntity partida) {
        JugadorEntity je = new JugadorEntity();
        je.setId(j.id());
        je.setNombre(j.nombre());
        je.setEquipo(equipo);
        je.setPartida(partida);
        return je;
    }

    private PartidaEntity toEntity(Partida partida) {
        PartidaEntity entity = new PartidaEntity();
        entity.setId(partida.getId());
        entity.setModalidad(partida.getModalidad().name());
        entity.setFase(partida.getFase().name());
        entity.setFechaCreacion(partida.getFechaCreacion());
        entity.setGanadorId(partida.getGanador() != null ? partida.getGanador().id() : null);

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
}