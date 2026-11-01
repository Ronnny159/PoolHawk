package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MatchServiceTest {

    @Autowired
    private MatchService matchService;

    @MockBean
    private MatchStorage storage; // Mockeamos la BD para no depender de Postgres

    @Test
    void porPuntosParejasDebeCrear4TurnosIntercalados() {
        // 1. Preparamos la petición: 2 equipos de 2 jugadores
        // El orden enviado es: A1 (Ana), A2 (Carlos), B1 (Luis), B2 (Maria)
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.POR_PUNTOS,
            FormatoJuego.PAREJAS,
            List.of("Ana", "Carlos", "Luis", "Maria"), 
            254.0, 127.0
        );

        // 2. Ejecutamos
        Partida partida = matchService.crear(request);

        // 3. Verificamos los turnos
        var turnos = partida.getEstado().getTurnos().jugadores();
        assertEquals(4, turnos.size(), "Debe haber 4 turnos");
        
        // El orden que produce el backend actualmente es: Ana (A1), Carlos (A2), Luis (B1), Maria (B2)
        // (Si tu lógica de MatchFactory cambió el orden, ajusta estas aserciones)
        assertEquals("Ana", turnos.get(0).nombre());
        assertEquals("Carlos", turnos.get(1).nombre());
        assertEquals("Luis", turnos.get(2).nombre());
        assertEquals("Maria", turnos.get(3).nombre());
    }
}