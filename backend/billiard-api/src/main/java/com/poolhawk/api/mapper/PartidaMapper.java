package com.poolhawk.api.mapper;

import com.poolhawk.api.dto.JugadorResponse;
import com.poolhawk.api.dto.PartidaResponse;
import com.poolhawk.domain.match.Grupo;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper entre el dominio {@link Partida} y los DTOs de la API.
 *
 * <p>Su responsabilidad es traducir el modelo de dominio a la vista
 * publica que expone la API, sin filtrar detalles internos.</p>
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
        // 1) Mapas reales del dominio (usando Grupo, no Enum)
        Map<Jugador, Integer> puntajesRaw = partida.getEstado().getPuntajes();
        Map<Jugador, Grupo> gruposRaw = partida.getEstado().getGruposAsignados(); 

        // 2) Normalizar
        Map<String, Integer> puntajes = normalizarPuntajes(puntajesRaw);
        Map<String, String> grupos = normalizarGrupos(gruposRaw);

        // 3) Totales por equipo
        Map<String, Integer> puntajesPorEquipo = Map.of(
            "A", sumarEquipo(partida.getEquipoA(), puntajes),
            "B", sumarEquipo(partida.getEquipoB(), puntajes)
        );

        // 4) Resto de datos (Ajusta el turno según tu dominio)
        String jugadorActual = obtenerNombreJugador(partida.getTurnoActual());

        return new PartidaResponse(
            partida.getId(),
            partida.getModalidad().name(),
            partida.getFase().name(),
            // Asumiendo que Equipo tiene un método para obtener sus jugadores (ej. getJugadores() o jugadores())
            partida.getEquipoA().getJugadores().stream().map(this::toJugadorResponse).toList(),
            partida.getEquipoB().getJugadores().stream().map(this::toJugadorResponse).toList(),
            puntajes,
            grupos,
            puntajesPorEquipo,
            jugadorActual,
            partida.getGanador() != null ? partida.getGanador().nombre() : null, // Ajusta a getNombre() si no es record
            partida.getEstado().getTurnos() != null ? partida.getEstado().getTurnos().size() : 0,
            partida.getFechaCreacion()
        );
    }

    // ── HELPERS CORREGIDOS ──────────────────────────────────────

    private Map<String, Integer> normalizarPuntajes(Map<Jugador, Integer> raw) {
        if (raw == null) return Map.of();
        return raw.entrySet().stream().collect(Collectors.toMap(
            e -> limpiar(e.getKey().nombre()), // Si no es record, usa getName()
            Map.Entry::getValue,
            Integer::sum
        ));
    }

    private Map<String, String> normalizarGrupos(Map<Jugador, Grupo> raw) {
        if (raw == null) return Map.of();
        return raw.entrySet().stream().collect(Collectors.toMap(
            e -> limpiar(e.getKey().nombre()), // Si no es record, usa getName()
            e -> e.getValue().name(), // Asumiendo que Grupo es un Enum y tiene name()
            (a, b) -> a
        ));
    }

    private int sumarEquipo(Equipo equipo, Map<String, Integer> puntajes) {
        if (equipo == null || equipo.getJugadores() == null) return 0;
        return equipo.getJugadores().stream()
            .filter(Objects::nonNull)
            .mapToInt(j -> puntajes.getOrDefault(limpiar(j.nombre()), 0)) // Si no es record, getName()
            .sum();
    }

    private String obtenerNombreJugador(Object turnoActual) {
        // Aquí ajusta al getter real de tu clase Turno
        return ""; 
    }

    private String limpiar(String s) {
        return s == null ? null : s.trim();
    }

    private JugadorResponse toJugadorResponse(Jugador jugador) {
        return new JugadorResponse(
            jugador.id(), // Si no es record, usa getId()
            limpiar(jugador.nombre()) // Si no es record, usa getNombre()
        );
    }
}