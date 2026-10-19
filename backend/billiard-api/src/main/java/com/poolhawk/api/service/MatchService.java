package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.*;
import com.poolhawk.rules.*;
import org.springframework.stereotype.Service;

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
    private final Map<String, Partida> partidasEnMemoria = new HashMap<>(); // puente temporal

    public MatchService(MatchStorage storage) {
        this.storage = storage;
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
    public Partida buscar(String id) {
        return Optional.ofNullable(partidasEnMemoria.get(id))
                .orElseThrow(() -> new RuntimeException("Partida no encontrada"));
    }

    /**
     * Lista todas las partidas almacenadas.
     *
     * @return lista de partidas
     */
    public List<Partida> listarTodas() {
        return new ArrayList<>(partidasEnMemoria.values());
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