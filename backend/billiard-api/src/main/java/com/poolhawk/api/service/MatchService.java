package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.*;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.*;
import com.poolhawk.rules.*;
import org.springframework.stereotype.Service;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchService {
    private final MatchStorage storage;
    private final MatchLiveHandler liveHandler;
    private final PartidaMapper mapper;
    
    private final ObjectMapper jsonMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
    private final Map<String, Partida> partidasEnMemoria = new HashMap<>();

    public MatchService(MatchStorage storage, MatchLiveHandler liveHandler, PartidaMapper mapper) {
        this.storage = storage;
        this.liveHandler = liveHandler;
        this.mapper = mapper;
    }

    public Partida crear(CrearPartidaRequest request) {
        String id = UUID.randomUUID().toString();
        
        double ancho = request.anchoMesa() != null ? request.anchoMesa() : Mesa.ANCHO_DEFECTO;
        double alto = request.altoMesa() != null ? request.altoMesa() : Mesa.ALTO_DEFECTO;
        
        Mesa mesa = construirMesaInicial(ancho, alto); 
        
        if (request.jugadores() == null || request.jugadores().size() < 2 || request.jugadores().size() > 4) {
            throw new IllegalArgumentException("Debe haber entre 2 y 4 jugadores.");
        }
        if (request.formato() == FormatoJuego.PAREJAS && request.jugadores().size() % 2 != 0) {
            throw new IllegalArgumentException("Las parejas requieren un número par de jugadores.");
        }

        List<Jugador> listaJugadores = request.jugadores().stream()
            .map(nombre -> new Jugador(UUID.randomUUID().toString(), nombre))
            .toList();

        Turnos<Jugador> turnos;
        if (request.formato() == FormatoJuego.INDIVIDUAL) {
            turnos = new Turnos<>(listaJugadores);
        } else {
            // Para parejas, el frontend envía [A1, B1, A2, B2]. El turno ya es ese orden.
            turnos = new Turnos<>(listaJugadores);
        }

        MatchState estado = new MatchState(id, request.modalidad(), turnos, mesa, Instant.now());
        estado.iniciar();

        // El constructor de Partida ahora se encarga de armar los equipos correctamente (1v1 o 2v2)
        Partida partida = new Partida(id, request.modalidad(), listaJugadores, estado, Instant.now());
        
        partidasEnMemoria.put(id, partida);
        storage.guardar(toEntity(partida));
        
        return partida;
    }

    public Partida buscar(String id) {
        if (partidasEnMemoria.containsKey(id)) return partidasEnMemoria.get(id);

        PartidaEntity entity = storage.buscar(id)
            .orElseThrow(() -> new PartidaNoEncontradaException(id));

        return reconstruirPartida(entity);
    }

    @Transactional(readOnly = true)
    public List<Partida> listarTodas() {
        return storage.listarTodas().stream()
            .map(this::reconstruirPartida)
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
        
        storage.guardar(toEntity(partida));

        try {
            String json = jsonMapper.writeValueAsString(mapper.toResponse(partida));
            liveHandler.broadcast(json);
        } catch (Exception e) {
            System.err.println("Error serializando partida: " + e.getMessage());
        }

        return resultado;
    }

    private Mesa construirMesaInicial(double ancho, double alto) {
        Mesa mesa = new Mesa(ancho, alto);
        mesa.agregarBola(Bola.crear(0, new Coordenada(ancho * 0.25, alto * 0.5)));
        
        double cx = ancho * 0.75;
        double cy = alto * 0.5;
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

    private Partida reconstruirPartida(PartidaEntity entity) {
        List<Jugador> jugadoresA = entity.getJugadores().stream()
            .filter(j -> "A".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();
        List<Jugador> jugadoresB = entity.getJugadores().stream()
            .filter(j -> "B".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();

        // Ahora reconstruimos de forma segura. Si por alguna razón vieja no hay jugadores, evitamos el crash.
        Equipo equipoA = new Equipo("eA-" + entity.getId(), "Equipo A");
        jugadoresA.forEach(equipoA::agregarJugador);
        
        Equipo equipoB = new Equipo("eB-" + entity.getId(), "Equipo B");
        jugadoresB.forEach(equipoB::agregarJugador);

        Mesa mesa = new Mesa();
        if (entity.getEstadoMesaJson() != null && !entity.getEstadoMesaJson().isEmpty()) {
            try {
                List<Bola> bolasGuardadas = jsonMapper.readValue(
                    entity.getEstadoMesaJson(),
                    jsonMapper.getTypeFactory().constructCollectionType(List.class, Bola.class)
                );
                bolasGuardadas.forEach(mesa::agregarBola);
            } catch (Exception e) {
                System.err.println("Error leyendo mesa desde JSON: " + e.getMessage());
            }
        }

        Partida partida = new Partida(
            entity.getId(),
            TipoModalidad.valueOf(entity.getModalidad()),
            equipoA, equipoB, mesa, entity.getFechaCreacion()
        );
        
        if (FasePartida.valueOf(entity.getFase()) == FasePartida.EN_CURSO) {
            partida.iniciar();
        }
        return partida;
    }

    private PartidaEntity toEntity(Partida partida) {
        PartidaEntity entity = new PartidaEntity();
        entity.setId(partida.getId());
        entity.setModalidad(partida.getModalidad().name());
        entity.setFase(partida.getFase().name());
        entity.setFechaCreacion(partida.getFechaCreacion());
        entity.setGanadorId(partida.getGanador() != null ? partida.getGanador().id() : null);

        try {
            entity.setEstadoMesaJson(jsonMapper.writeValueAsString(partida.getMesa().getBolas()));
        } catch (Exception e) {
            System.err.println("Error guardando mesa en JSON: " + e.getMessage());
        }

        List<JugadorEntity> jugadores = new ArrayList<>();
        // Guardamos explícitamente al Equipo A y Equipo B
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

    private TiroEvent construirEvento(Partida partida, TiroEventRequest request) {
        Jugador jugador = partida.getEstado().getTurnos().jugadores().stream()
            .filter(j -> j.id().equals(request.jugadorId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Jugador no encontrado"));

        Bola primeraTocada = null;
        if (request.primeraBolaTocada() != null) {
            primeraTocada = partida.getMesa().buscarBola(request.primeraBolaTocada()).orElse(null);
        }

        List<Bola> embolsadas = new ArrayList<>();
        if (request.bolasEmbolsadas() != null) {
            for (Integer numero : request.bolasEmbolsadas()) {
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