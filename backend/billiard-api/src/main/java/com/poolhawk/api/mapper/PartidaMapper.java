package com.poolhawk.api.mapper;

import com.poolhawk.api.dto.JugadorResponse;
import com.poolhawk.api.dto.PartidaResponse;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.player.Jugador;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper entre el dominio {@link Partida} y los DTOs de la API.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Component
public class PartidaMapper {

    /**
     * Convierte una partida del dominio en su DTO de respuesta.
     *
     * @param partida partida del dominio
     * @return DTO con la vista publica de la partida
     */
    public PartidaResponse toResponse(Partida partida) {
        List<JugadorResponse> equipoA = partida.getEquipoA().getJugadores()
            .stream().map(this::toJugadorResponse).toList();
        List<JugadorResponse> equipoB = partida.getEquipoB().getJugadores()
            .stream().map(this::toJugadorResponse).toList();

        Map<String, Integer> puntajes = new LinkedHashMap<>();
        partida.getEstado().getPuntajes().forEach(
            (j, p) -> puntajes.put(j.nombre(), p));

        // Exponer el grupo asignado por jugador (LISAS / RAYADAS / SIN_ASIGNAR)
        Map<String, String> grupos = new LinkedHashMap<>();
        partida.getEstado().getGruposAsignados().forEach(
            (j, g) -> grupos.put(j.nombre(), g.name()));

        return new PartidaResponse(
            partida.getId(),
            partida.getModalidad().name(),
            partida.getFase().name(),
            equipoA,
            equipoB,
            puntajes,
            grupos,
            puntajes, 
            // FIX 1: Usar jugadorActual() en vez de getTurnoActual()
            partida.jugadorActual().nombre(),
            partida.getGanador() != null ? partida.getGanador().nombre() : null,
            // FIX 2: Usar cantidadTiros() que sí existe en MatchState
            partida.getEstado().cantidadTiros(), 
            // Si el error era por la cantidad de jugadores, en Turnos se usa cantidadJugadores()
            partida.getFechaCreacion());
    }

    private JugadorResponse toJugadorResponse(Jugador j) {
        return new JugadorResponse(j.id(), j.nombre());
    }
    
}