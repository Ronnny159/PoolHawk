package com.poolhawk.api.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.entity.TiroEntity;
import com.poolhawk.api.exception.PartidaCorruptaException;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.*;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Mesa;
import com.poolhawk.domain.table.TroneraNombre;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper entre el agregado {@link Partida} y su representación JPA.
 *
 * <p>Reglas de robustez aplicadas en Fase 5:</p>
 * <ul>
 *   <li><b>H-01:</b> toda corrupción en la lectura se propaga como
 *       {@link PartidaCorruptaException}; no se omiten tiros ni se devuelven
 *       partidas parciales.</li>
 *   <li><b>H-02:</b> la serialización aborta con excepción; nunca se persiste
 *       un tiro sin {@code resultado_json}.</li>
 *   <li><b>H-05:</b> el snapshot de mesa incluye {@code ancho} y {@code alto}.
 *       Se mantiene compatibilidad con el formato viejo (array de bolas) para
 *       partidas persistidas antes del cambio.</li>
 *   <li><b>H-06:</b> cada tiro se valida estructuralmente al reconstruir:
 *       jugador existente, bola existente, troneras coincidentes, campos
 *       obligatorios presentes.</li>
 * </ul>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Component
public class MatchPersistenceMapper {

    private final ObjectMapper jsonMapper;

    public MatchPersistenceMapper(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * Reconstruye el agregado {@link Partida} desde su entidad JPA.
     *
     * @param entity entidad con {@code tiros} inicializado (ver {@code @EntityGraph})
     * @return la partida de dominio completamente restaurada
     * @throws PartidaCorruptaException si algún dato persistido es inconsistente
     */
    public Partida toDomain(PartidaEntity entity) {
        List<Jugador> jugadoresA = entity.getJugadores().stream()
            .filter(j -> "A".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();
        List<Jugador> jugadoresB = entity.getJugadores().stream()
            .filter(j -> "B".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();

        FormatoJuego formatoGuardado = entity.getFormato() != null
            ? FormatoJuego.valueOf(entity.getFormato())
            : FormatoJuego.INDIVIDUAL;

        Equipo equipoA;
        Equipo equipoB;
        if (jugadoresA.size() == 2) {
            equipoA = Equipo.pareja("eA-" + entity.getId(), "Equipo A", jugadoresA.get(0), jugadoresA.get(1));
            equipoB = Equipo.pareja("eB-" + entity.getId(), "Equipo B", jugadoresB.get(0), jugadoresB.get(1));
        } else {
            equipoA = Equipo.individual("eA-" + entity.getId(), jugadoresA.get(0).nombre(), jugadoresA.get(0));
            equipoB = Equipo.individual("eB-" + entity.getId(), jugadoresB.get(0).nombre(), jugadoresB.get(0));
        }

        Mesa mesa = reconstruirMesa(entity.getEstadoMesaJson(), entity.getId());

        List<Jugador> rotacion = new ArrayList<>();
        List<Jugador> jugadoresA_ = equipoA.getJugadores();
        List<Jugador> jugadoresB_ = equipoB.getJugadores();
        int maxSize = Math.max(jugadoresA_.size(), jugadoresB_.size());
        for (int i = 0; i < maxSize; i++) {
            if (i < jugadoresA_.size()) rotacion.add(jugadoresA_.get(i));
            if (i < jugadoresB_.size()) rotacion.add(jugadoresB_.get(i));
        }

        Turnos<Jugador> turnos = new Turnos<>(rotacion);
        MatchState estado = new MatchState(entity.getId(), TipoModalidad.valueOf(entity.getModalidad()),
            equipoA, equipoB, turnos, mesa, entity.getFechaCreacion());

        restaurarSnapshot(entity, rotacion, estado);
        restaurarHistorialDesdeEntity(entity, mesa, rotacion, estado);

        return new Partida(
            entity.getId(),
            TipoModalidad.valueOf(entity.getModalidad()),
            equipoA,
            equipoB,
            formatoGuardado,
            entity.getFechaCreacion(),
            estado
        );
    }

    /**
     * Reconstruye la mesa desde su snapshot JSON.
     *
     * <p>Soporta dos formatos:</p>
     * <ul>
     *   <li><b>Nuevo (objeto):</b> {@code {"ancho":254.0,"alto":127.0,"bolas":[...]}}.</li>
     *   <li><b>Viejo (array):</b> {@code [{"numero":0,...},...]}. Se usan las
     *       dimensiones por defecto.</li>
     * </ul>
     *
     * @param json      snapshot persistido, o {@code null}
     * @param partidaId id para contexto de error
     * @return mesa reconstruida
     * @throws PartidaCorruptaException si el JSON no es interpretable
     */
    private Mesa reconstruirMesa(String json, String partidaId) {
        if (json == null || json.isBlank()) {
            return new Mesa();
        }
        try {
            JsonNode node = jsonMapper.readTree(json);
            if (node.isArray()) {
                Mesa mesa = new Mesa();
                for (JsonNode b : node) {
                    mesa.agregarBola(jsonMapper.treeToValue(b, Bola.class));
                }
                return mesa;
            }
            if (node.isObject()) {
                double ancho = node.has("ancho") ? node.get("ancho").asDouble() : Mesa.ANCHO_DEFECTO;
                double alto = node.has("alto") ? node.get("alto").asDouble() : Mesa.ALTO_DEFECTO;
                Mesa mesa = new Mesa(ancho, alto);
                if (node.has("bolas") && node.get("bolas").isArray()) {
                    for (JsonNode b : node.get("bolas")) {
                        mesa.agregarBola(jsonMapper.treeToValue(b, Bola.class));
                    }
                }
                return mesa;
            }
            throw new PartidaCorruptaException(partidaId,
                "estado_mesa_json no es un array ni un objeto");
        } catch (PartidaCorruptaException e) {
            throw e;
        } catch (Exception e) {
            throw new PartidaCorruptaException(partidaId,
                "error leyendo mesa desde JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Restaura puntajes, grupos, turno, fase y ganador desde el snapshot JSON.
     *
     * @param entity   entidad JPA
     * @param rotacion orden de turnos
     * @param estado   estado a hidratar
     * @throws PartidaCorruptaException si el snapshot es inconsistente
     */
    @SuppressWarnings("unchecked")
    private void restaurarSnapshot(PartidaEntity entity, List<Jugador> rotacion, MatchState estado) {
        String json = entity.getEstadoPartidaJson();
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            Map<String, Object> snapshot = jsonMapper.readValue(json, Map.class);

            Map<Jugador, Integer> puntajesGuardados = new HashMap<>();
            if (snapshot.containsKey("puntajes")) {
                Map<String, Integer> puntajesMap = (Map<String, Integer>) snapshot.get("puntajes");
                for (Jugador j : rotacion) {
                    if (puntajesMap.containsKey(j.id())) {
                        puntajesGuardados.put(j, puntajesMap.get(j.id()));
                    }
                }
            }

            Map<Jugador, Grupo> gruposGuardados = new HashMap<>();
            if (snapshot.containsKey("grupos")) {
                Map<String, String> gruposMap = (Map<String, String>) snapshot.get("grupos");
                for (Jugador j : rotacion) {
                    if (gruposMap.containsKey(j.id()) && !gruposMap.get(j.id()).equals("SIN_ASIGNAR")) {
                        gruposGuardados.put(j, Grupo.valueOf(gruposMap.get(j.id())));
                    }
                }
            }

            if (snapshot.containsKey("turnoActualId")) {
                String turnoId = (String) snapshot.get("turnoActualId");
                for (int i = 0; i < rotacion.size(); i++) {
                    if (estado.jugadorActual().id().equals(turnoId)) break;
                    estado.avanzarTurno();
                }
            }

            FasePartida faseGuardada = FasePartida.valueOf(entity.getFase());
            Jugador ganadorGuardado = null;
            if (entity.getGanadorId() != null) {
                ganadorGuardado = rotacion.stream()
                    .filter(j -> j.id().equals(entity.getGanadorId()))
                    .findFirst()
                    .orElseThrow(() -> new PartidaCorruptaException(entity.getId(),
                        "ganador " + entity.getGanadorId() + " no participa en la partida"));
            }

            estado.restaurarEstado(faseGuardada, ganadorGuardado, puntajesGuardados, gruposGuardados);
        } catch (PartidaCorruptaException e) {
            throw e;
        } catch (Exception e) {
            throw new PartidaCorruptaException(entity.getId(),
                "error restaurando snapshot: " + e.getMessage(), e);
        }
    }

    /**
     * Reconstruye el historial de {@link TiroEvent} y el mapa
     * {@code eventId -> ResultadoTiro} desde las entidades {@link TiroEntity}.
     *
     * <p>Cualquier inconsistencia (jugador inexistente, bola inexistente,
     * tronera desconocida, resultado ausente, campos obligatorios nulos)
     * aborta la restauración con {@link PartidaCorruptaException}. No se
     * omiten tiros silenciosamente.</p>
     *
     * @param entity   entidad JPA
     * @param mesa     mesa reconstruida
     * @param rotacion jugadores en orden de turno
     * @param estado   estado donde inyectar el historial
     * @throws PartidaCorruptaException si un tiro no puede reconstruirse
     */
    private void restaurarHistorialDesdeEntity(PartidaEntity entity, Mesa mesa,
                                                List<Jugador> rotacion, MatchState estado) {
        if (entity.getTiros() == null || entity.getTiros().isEmpty()) {
            estado.restaurarHistorial(List.of(), Map.of());
            return;
        }

        List<TiroEvent> historial = new ArrayList<>();
        Map<String, ResultadoTiro> resultados = new HashMap<>();

        for (TiroEntity te : entity.getTiros()) {
            TiroEvent evento = reconstruirTiro(entity, te, mesa, rotacion);
            historial.add(evento);

            if (te.getResultadoJson() == null || te.getResultadoJson().isBlank()) {
                throw new PartidaCorruptaException(entity.getId(),
                    "tiro " + te.getEventId() + " sin resultado_json");
            }
            try {
                ResultadoTiro resultado = jsonMapper.readValue(te.getResultadoJson(), ResultadoTiro.class);
                resultados.put(te.getEventId(), resultado);
            } catch (Exception e) {
                throw new PartidaCorruptaException(entity.getId(),
                    "resultado_json inválido en tiro " + te.getEventId() + ": " + e.getMessage(), e);
            }
        }

        estado.restaurarHistorial(historial, resultados);
    }

    /**
     * Reconstruye un {@link TiroEvent} a partir de su fila JPA, validando
     * integridad estructural.
     *
     * @param entity   entidad de partida
     * @param te       fila del tiro
     * @param mesa     mesa reconstruida
     * @param rotacion jugadores en orden de turno
     * @return el evento reconstruido
     * @throws PartidaCorruptaException si algún dato es inconsistente
     */
    private TiroEvent reconstruirTiro(PartidaEntity entity, TiroEntity te, Mesa mesa, List<Jugador> rotacion) {
        if (te.getEventId() == null || te.getEventId().isBlank()) {
            throw new PartidaCorruptaException(entity.getId(), "tiro sin event_id");
        }

        Jugador jugador = rotacion.stream()
            .filter(j -> j.id().equals(te.getJugadorId()))
            .findFirst()
            .orElseThrow(() -> new PartidaCorruptaException(entity.getId(),
                "jugador " + te.getJugadorId() + " del tiro " + te.getEventId() + " no participa"));

        Bola primeraBola = null;
        if (te.getPrimeraBolaTocada() != null) {
            primeraBola = mesa.buscarBola(te.getPrimeraBolaTocada())
                .orElseThrow(() -> new PartidaCorruptaException(entity.getId(),
                    "primera bola " + te.getPrimeraBolaTocada()
                    + " inexistente en tiro " + te.getEventId()));
        }

        if (te.getBolasEmbolsadasJson() == null || te.getBolasEmbolsadasJson().isBlank()) {
            throw new PartidaCorruptaException(entity.getId(),
                "bolas_embolsadas_json nulo en tiro " + te.getEventId());
        }
        if (te.getTronerasJson() == null || te.getTronerasJson().isBlank()) {
            throw new PartidaCorruptaException(entity.getId(),
                "troneras_json nulo en tiro " + te.getEventId());
        }

        List<Integer> numerosEmbolsados;
        try {
            numerosEmbolsados = jsonMapper.readValue(
                te.getBolasEmbolsadasJson(), new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new PartidaCorruptaException(entity.getId(),
                "bolas_embolsadas_json inválido en tiro " + te.getEventId() + ": " + e.getMessage(), e);
        }

        List<Bola> bolasEmbolsadas = new ArrayList<>();
        for (Integer n : numerosEmbolsados) {
            if (n == null) {
                throw new PartidaCorruptaException(entity.getId(),
                    "bola nula en tiro " + te.getEventId());
            }
            Bola bola = mesa.buscarBola(n)
                .orElseThrow(() -> new PartidaCorruptaException(entity.getId(),
                    "bola " + n + " inexistente en tiro " + te.getEventId()));
            bolasEmbolsadas.add(bola);
        }

        List<String> nombresTroneras;
        try {
            nombresTroneras = jsonMapper.readValue(
                te.getTronerasJson(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new PartidaCorruptaException(entity.getId(),
                "troneras_json inválido en tiro " + te.getEventId() + ": " + e.getMessage(), e);
        }

        if (nombresTroneras.size() != bolasEmbolsadas.size()) {
            throw new PartidaCorruptaException(entity.getId(),
                "troneras (" + nombresTroneras.size() + ") y bolas ("
                + bolasEmbolsadas.size() + ") no coinciden en tiro " + te.getEventId());
        }

        List<TroneraNombre> troneras = new ArrayList<>();
        for (String nombre : nombresTroneras) {
            if (nombre == null) {
                throw new PartidaCorruptaException(entity.getId(),
                    "tronera nula en tiro " + te.getEventId());
            }
            try {
                troneras.add(TroneraNombre.valueOf(nombre));
            } catch (IllegalArgumentException e) {
                throw new PartidaCorruptaException(entity.getId(),
                    "tronera inválida '" + nombre + "' en tiro " + te.getEventId(), e);
            }
        }

        if (te.getTimestamp() == null) {
            throw new PartidaCorruptaException(entity.getId(),
                "timestamp nulo en tiro " + te.getEventId());
        }
        if (te.getDuracionSegundos() == null) {
            throw new PartidaCorruptaException(entity.getId(),
                "duracion_segundos nulo en tiro " + te.getEventId());
        }

        return new TiroEvent(
            te.getEventId(),
            entity.getId(),
            jugador,
            primeraBola,
            bolasEmbolsadas,
            troneras,
            te.getTimestamp(),
            Duration.ofSeconds(te.getDuracionSegundos())
        );
    }

    /**
     * Convierte el agregado {@link Partida} a su entidad JPA, incluyendo el
     * snapshot de mesa con dimensiones, el snapshot de partida y el historial
     * completo de tiros con sus resultados.
     *
     * @param partida partida de dominio
     * @return la entidad lista para persistir
     * @throws IllegalStateException si falla la serialización (provoca rollback)
     */
    public PartidaEntity toEntity(Partida partida) {
        PartidaEntity entity = new PartidaEntity();
        entity.setId(partida.getId());
        entity.setModalidad(partida.getModalidad().name());
        entity.setFase(partida.getFase().name());
        entity.setFechaCreacion(partida.getFechaCreacion());
        entity.setGanadorId(partida.getGanador() != null ? partida.getGanador().id() : null);
        entity.setFormato(partida.getFormato().name());

        try {
            MesaSnapshot mesaSnap = new MesaSnapshot(
                partida.getMesa().getAncho(),
                partida.getMesa().getAlto(),
                partida.getMesa().getBolas());
            entity.setEstadoMesaJson(jsonMapper.writeValueAsString(mesaSnap));

            Map<String, Object> snapshot = new HashMap<>();
            Map<String, Integer> puntajes = new HashMap<>();
            partida.getEstado().getPuntajes().forEach((j, p) -> puntajes.put(j.id(), p));
            snapshot.put("puntajes", puntajes);

            Map<String, String> grupos = new HashMap<>();
            partida.getEstado().getGruposAsignados().forEach((j, g) -> grupos.put(j.id(), g.name()));
            snapshot.put("grupos", grupos);

            snapshot.put("turnoActualId", partida.jugadorActual().id());

            entity.setEstadoPartidaJson(jsonMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            throw new IllegalStateException(
                "Error serializando estado de la partida " + partida.getId() + ": " + e.getMessage(), e);
        }

        List<JugadorEntity> jugadores = new ArrayList<>();
        for (Jugador j : partida.getEquipoA().getJugadores()) {
            jugadores.add(toJugadorEntity(j, "A", entity));
        }
        for (Jugador j : partida.getEquipoB().getJugadores()) {
            jugadores.add(toJugadorEntity(j, "B", entity));
        }
        entity.setJugadores(jugadores);

        List<TiroEntity> tiros = new ArrayList<>();
        for (TiroEvent evento : partida.getEstado().getHistorial()) {
            tiros.add(toTiroEntity(evento, partida, entity));
        }
        entity.setTiros(tiros);

        return entity;
    }

    /**
     * Convierte un {@link TiroEvent} a su entidad JPA, incluyendo el
     * {@link ResultadoTiro} asociado.
     *
     * <p>Si no existe resultado para el evento o falla la serialización,
     * se aborta la operación. Persistir un tiro sin resultado rompería la
     * idempotencia tras un reinicio (H-02).</p>
     *
     * @param evento  tiro de dominio
     * @param partida partida de origen
     * @param entity  entidad padre
     * @return la entidad lista para persistir
     * @throws IllegalStateException si el tiro no tiene resultado o falla la serialización
     */
    private TiroEntity toTiroEntity(TiroEvent evento, Partida partida, PartidaEntity entity) {
        TiroEntity te = new TiroEntity();
        te.setId(evento.id());
        te.setEventId(evento.id());
        te.setPartida(entity);
        te.setJugadorId(evento.jugador().id());
        te.setPrimeraBolaTocada(evento.primeraBolaTocada() != null
            ? evento.primeraBolaTocada().numero()
            : null);
        te.setTimestamp(evento.timestamp());
        te.setDuracionSegundos(evento.duracion() != null ? evento.duracion().getSeconds() : 5L);

        try {
            List<Integer> numerosEmbolsados = evento.bolasEmbolsadas().stream()
                .map(Bola::numero)
                .toList();
            te.setBolasEmbolsadasJson(jsonMapper.writeValueAsString(numerosEmbolsados));

            List<String> nombresTroneras = evento.troneras().stream()
                .map(Enum::name)
                .toList();
            te.setTronerasJson(jsonMapper.writeValueAsString(nombresTroneras));

            ResultadoTiro resultado = partida.getEstado().getResultadosPorEvento().get(evento.id());
            if (resultado == null) {
                throw new IllegalStateException(
                    "El tiro " + evento.id() + " no tiene ResultadoTiro evaluado. "
                    + "No se puede persistir sin resultado (viola H-02).");
            }
            te.setResultadoJson(jsonMapper.writeValueAsString(resultado));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Error serializando tiro " + evento.id() + ": " + e.getMessage(), e);
        }

        return te;
    }

    private JugadorEntity toJugadorEntity(Jugador j, String equipo, PartidaEntity partida) {
        JugadorEntity je = new JugadorEntity();
        je.setId(j.id());
        je.setNombre(j.nombre());
        je.setEquipo(equipo);
        je.setPartida(partida);
        return je;
    }

    /**
     * Snapshot serializable de la mesa: dimensiones + bolas.
     *
     * @param ancho ancho en centímetros
     * @param alto  alto en centímetros
     * @param bolas bolas de la mesa (incluyendo embolsadas con su estado)
     */
    private record MesaSnapshot(double ancho, double alto, List<Bola> bolas) {
    }
}