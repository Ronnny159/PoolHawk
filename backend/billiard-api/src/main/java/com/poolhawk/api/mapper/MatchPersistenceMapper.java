package com.poolhawk.api.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.entity.JugadorEntity;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.domain.match.*;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Mesa;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

        List<Jugador> listaPlana = new ArrayList<>();
        if (jugadoresA.size() == 2 && jugadoresB.size() == 2) {
            listaPlana.add(jugadoresA.get(0));
            listaPlana.add(jugadoresB.get(0));
            listaPlana.add(jugadoresA.get(1));
            listaPlana.add(jugadoresB.get(1));
        } else if (jugadoresA.size() == 1 && jugadoresB.size() == 1) {
            listaPlana.add(jugadoresA.get(0));
            listaPlana.add(jugadoresB.get(0));
        } else {
            listaPlana.addAll(jugadoresA);
            listaPlana.addAll(jugadoresB);
        }

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

        MatchState estado = new MatchState(entity.getId(), TipoModalidad.valueOf(entity.getModalidad()), 
            new Turnos<>(listaPlana), mesa, entity.getFechaCreacion());
            
        FormatoJuego formatoGuardado = entity.getFormato() != null 
            ? FormatoJuego.valueOf(entity.getFormato()) 
            : (listaPlana.size() == 4 ? FormatoJuego.PAREJAS : FormatoJuego.INDIVIDUAL);

        Partida partida = new Partida(entity.getId(), TipoModalidad.valueOf(entity.getModalidad()), 
            listaPlana, estado, entity.getFechaCreacion(), formatoGuardado);
        
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
        
        // Guardamos el formato
        entity.setFormato(partida.getFormato().name());

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