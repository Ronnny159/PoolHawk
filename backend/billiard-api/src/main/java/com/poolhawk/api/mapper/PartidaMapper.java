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
 * Mapea el agregado {@link Partida} a su DTO {@link PartidaResponse},
 * incluyendo el estado actual de la mesa ({@code bolasEnMesa}).
 */
@Component
public class PartidaMapper {

    /**
     * Convierte una {@link Partida} de dominio a su representación HTTP.
     *
     * @param partida partida de dominio
     * @return DTO con todos los campos expuestos al frontend
     */
    public PartidaResponse toResponse(Partida partida) {
        return new PartidaResponse(
            partida.getId(),
            partida.getModalidad().name(),
            partida.getFormato().name(),
            partida.getEstado().getFase().name(),
            mapearEquipo(partida.getEquipoA().getJugadores()),
            mapearEquipo(partida.getEquipoB().getJugadores()),
            mapearPuntajes(partida),
            partida.jugadorActual().id(),
            partida.jugadorActual().nombre(),
            partida.getEstado().getGanador() != null
                ? partida.getEstado().getGanador().nombre()
                : null,
            partida.getEstado().cantidadTiros(),
            partida.getEstado().getFechaInicio(),
            partida.getEstado().bolasEnMesa());
    }

    private List<JugadorResponse> mapearEquipo(List<Jugador> jugadores) {
        return jugadores.stream().map(this::mapearJugador).toList();
    }

    private JugadorResponse mapearJugador(Jugador jugador) {
        return new JugadorResponse(jugador.id(), jugador.nombre());
    }

    private Map<String, Integer> mapearPuntajes(Partida partida) {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        partida.getEstado().getPuntajes()
            .forEach((jugador, puntos) -> mapa.put(jugador.nombre(), puntos));
        return mapa;
    }
}