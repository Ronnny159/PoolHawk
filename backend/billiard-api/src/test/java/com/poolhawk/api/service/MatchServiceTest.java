package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Grupo;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Jugador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de integracion del servicio de partidas.
 * Utiliza la base de datos H2 en local (profile test).
 */
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DisplayName("MatchService - Pruebas de Integracion")
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @Test
    @DisplayName("Por Puntos Parejas debe crear 4 turnos intercalados (A1 -> B1 -> A2 -> B2)")
    void porPuntosParejasDebeCrear4TurnosIntercalados() {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.POR_PUNTOS,
            FormatoJuego.PAREJAS,
            List.of("Ana", "Beto", "Carla", "Diego"),
            254.0, 127.0
        );

        Partida partida = matchService.crear(request);

        var turnos = partida.getEstado().getTurnos().jugadores();
        assertEquals(4, turnos.size());
        assertEquals("Ana", turnos.get(0).nombre());
        assertEquals("Beto", turnos.get(1).nombre());
        assertEquals("Carla", turnos.get(2).nombre());
        assertEquals("Diego", turnos.get(3).nombre());
    }

    @Test
    @DisplayName("El servicio finaliza la partida y persiste al ganador si hay derrota terminal")
    void servicioFinalizaPorDerrotaYPersisteGanador() {
        // 1. Crear partida
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0
        );
        Partida partida = matchService.crear(request);

        Jugador ana = partida.getEquipoA().getJugadores().get(0);
        Jugador beto = partida.getEquipoB().getJugadores().get(0);

        // Asignar grupos para que la regla de derrota funcione correctamente
        partida.getEstado().asignarGrupo(ana, Grupo.LISAS);
        partida.getEstado().asignarGrupo(beto, Grupo.RAYADAS);

        // 2. Simular tiro de derrota (Ana embolsa la 8 antes de limpiar su grupo)
        TiroEventRequest tiroRequest = new TiroEventRequest(
            "evento-derrota-123", ana.id(), 8, List.of(8), List.of("TOP_LEFT"), 5
        );

        // 3. Ejecutar y verificar resultado
        var resultado = matchService.registrarTiro(partida.getId(), tiroRequest);
        assertTrue(resultado.esDerrota(), "El tiro debe ser marcado como derrota");

        // 4. Verificar que la partida se recuperó desde la BD (la caché fue invalidada)
        // y que el estado persistido es correcto.
        Partida actualizada = matchService.buscar(partida.getId());
        assertTrue(actualizada.estaTerminada(), "La partida debe estar terminada");
        assertEquals(beto, actualizada.getGanador(), "Beto debe ser el ganador");
    }
}
