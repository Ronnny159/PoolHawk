package com.poolhawk.api.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.domain.match.FasePartida;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Mesa;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapper especializado en convertir entre el modelo de dominio y las entidades JPA.
 * Encapsula toda la lógica de serialización/deserialización de JSON.
 */
@Component
public class MatchPersistenceMapper {

    private final ObjectMapper jsonMapper;

    public MatchPersistenceMapper(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public Partida toDomain(PartidaEntity entity) {
        List<Jugador> jugadoresA = entity.getJugadores().stream()
            .filter(j -> "A".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();
        List<Jugador> jugadoresB = entity.getJugadores().stream()
            .filter(j -> "B".equals(j.getEquipo()))
            .map(j -> new Jugador(j.getId(), j.getNombre()))
            .toList();

        Equipo equipoA = new Equipo("eA-" + entity.getId(), "Equipo A");
        jugadoresA.forEach(equipoA::agregarJugador);
        
        Equipo equipoB = new Equipo("eB-" + entity.getId(), "Equipo B");
        jugadoresB.forEach(equipoB::agregarJugador);

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

    public PartidaEntity toEntity(Partida partida) {
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