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
 * Mapper entre el dominio Partida y los DTOs de la API.
 */
@Component
public class PartidaMapper {

    public PartidaResponse toResponse(Partida partida) {
        List<JugadorResponse> equipoA = partida.getEquipoA().getJugadores()
            .stream().map(this::toJugadorResponse).toList();
        List<JugadorResponse> equipoB = partida.getEquipoB().getJugadores()
            .stream().map(this::toJugadorResponse).toList();

        Map<String, Integer> puntajes = new LinkedHashMap<>();
        partida.getEstado().getPuntajes().forEach(
            (j, p) -> puntajes.put(j.nombre(), p));

        return new PartidaResponse(
            partida.getId(),
            partida.getModalidad().name(),
            partida.getFormato().name(), // <-- AGREGADO AQUI
            partida.getFase().name(),
            equipoA,
            equipoB,
            puntajes,
            partida.jugadorActual().nombre(),
            partida.getGanador() != null ? partida.getGanador().nombre() : null,
            partida.getEstado().cantidadTiros(),
            partida.getFechaCreacion());
    }

    private JugadorResponse toJugadorResponse(Jugador j) {
        return new JugadorResponse(j.id(), j.nombre());
    }
}