package com.poolhawk.api.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.domain.match.*;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Mesa;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MatchPersistenceMapper {

    private final ObjectMapper jsonMapper;

    public MatchPersistenceMapper(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Partida toDomain(PartidaEntity entity) {
        // 1. Reconstruir equipos explícitos
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

        // 2. Reconstruir la mesa
        Mesa mesa = new Mesa();
        if (entity.getEstadoMesaJson() != null && !entity.getEstadoMesaJson().isEmpty()) {
            try {
                Bola[] bolasGuardadas = jsonMapper.readValue(entity.getEstadoMesaJson(), Bola[].class);
                for (Bola bola : bolasGuardadas) {
                    mesa.agregarBola(bola);
                }
            } catch (Exception e) {
                System.err.println("Error leyendo mesa desde JSON: " + e.getMessage());
            }
        }

        // 3. Construir la rotación (A1 → B1 → A2 → B2)
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

        // 4. Restaurar snapshot
        if (entity.getEstadoPartidaJson() != null && !entity.getEstadoPartidaJson().isEmpty()) {
            try {
                Map<String, Object> snapshot = jsonMapper.readValue(entity.getEstadoPartidaJson(), Map.class);

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
                        .findFirst().orElse(null);
                }

                estado.restaurarEstado(faseGuardada, ganadorGuardado, puntajesGuardados, gruposGuardados);

            } catch (Exception e) {
                System.err.println("Error leyendo estado desde JSON: " + e.getMessage());
            }
        }

        // EL CAMBIO ESTÁ AQUÍ: Usamos el nuevo constructor que recibe el MatchState ya restaurado
        return new Partida(
            entity.getId(), 
            TipoModalidad.valueOf(entity.getModalidad()), 
            equipoA, 
            equipoB, 
            formatoGuardado, 
            entity.getFechaCreacion(), 
            estado // <-- ¡Le pasamos el estado que acabamos de restaurar!
        );
    }

    public PartidaEntity toEntity(Partida partida) {
        PartidaEntity entity = new PartidaEntity();
        entity.setId(partida.getId());
        entity.setModalidad(partida.getModalidad().name());
        entity.setFase(partida.getFase().name());
        entity.setFechaCreacion(partida.getFechaCreacion());
        entity.setGanadorId(partida.getGanador() != null ? partida.getGanador().id() : null);
        entity.setFormato(partida.getFormato().name());

        try {
            entity.setEstadoMesaJson(jsonMapper.writeValueAsString(partida.getMesa().getBolas()));

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
            System.err.println("Error guardando estado en JSON: " + e.getMessage());
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
}