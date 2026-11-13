package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Grupo;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Jugador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @MockBean
    private MatchStorage storage;

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
        
        assertEquals("Ana", partida.getEquipoA().getJugadores().get(0).nombre());
        assertEquals("Carla", partida.getEquipoA().getJugadores().get(1).nombre());
        assertEquals("Beto", partida.getEquipoB().getJugadores().get(0).nombre());
        assertEquals("Diego", partida.getEquipoB().getJugadores().get(1).nombre());
    }

    @Test
    @DisplayName("El servicio finaliza la partida y persiste al ganador si hay derrota terminal")
    void servicioFinalizaPorDerrotaYPersisteGanador() {
        // 1. Crear partida
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0
        );
        Partida partida = matchService.crear(request);
        
        // Limpiamos las invocaciones previas a storage.guardar (del crear)
        clearInvocations(storage);

        // 2. Asignar grupos
        Jugador ana = partida.getEquipoA().getJugadores().get(0);
        Jugador beto = partida.getEquipoB().getJugadores().get(0);
        partida.getEstado().asignarGrupo(ana, Grupo.LISAS);
        partida.getEstado().asignarGrupo(beto, Grupo.RAYADAS);
        
        // 3. Simular tiro de derrota (embolsa la 8 prematuramente)
        TiroEventRequest tiroRequest = new TiroEventRequest(
            ana.id(), 8, List.of(8), List.of("TOP_LEFT"), 5
        );
        
        // 4. Registrar tiro
        ResultadoTiro resultado = matchService.registrarTiro(partida.getId(), tiroRequest);
        
        // 5. Verificaciones de estado
        assertTrue(resultado.esDerrota(), "El resultado debe ser derrota terminal");
        
        Partida actualizada = matchService.buscar(partida.getId());
        assertTrue(actualizada.estaTerminada(), "La partida debe estar finalizada");
        assertEquals(beto, actualizada.getGanador(), "El ganador en memoria debe ser el oponente (Beto)");

        // 6. Verificación de persistencia (El auditor lo exige)
        ArgumentCaptor<PartidaEntity> captor = ArgumentCaptor.forClass(PartidaEntity.class);
        verify(storage, times(1)).guardar(captor.capture());
        
        PartidaEntity entityGuardada = captor.getValue();
        assertEquals(beto.id(), entityGuardada.getGanadorId(), "El ganador persistido en BD debe ser Beto");
        assertEquals("FINALIZADA", entityGuardada.getFase(), "La fase persistida debe ser FINALIZADA");
    }
}