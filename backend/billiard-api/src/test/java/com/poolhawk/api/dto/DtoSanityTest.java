package com.poolhawk.api.dto;

import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTOs - Pruebas de sanidad")
class DtoSanityTest {

    @Test
    @DisplayName("Crea un CrearPartidaRequest válido")
    void crearPartidaRequest() {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of("Ana", "Beto", "Carlos"),
            254.0,
            127.0
        );

        assertEquals(TipoModalidad.BOLA_8, request.modalidad());
        assertEquals(FormatoJuego.INDIVIDUAL, request.formato());
        assertEquals(3, request.jugadores().size());
        assertEquals("Ana", request.jugadores().get(0));
        assertEquals(254.0, request.anchoMesa());
        assertEquals(127.0, request.altoMesa());
    }

    @Test
    @DisplayName("Crea un TiroEventRequest válido")
    void tiroEventRequest() {
        // CORREGIDO: Agregamos el eventId como primer parámetro
        TiroEventRequest request = new TiroEventRequest(
            "evento-sanidad-123", 
            "j1", 
            1, 
            List.of(1, 2), 
            List.of("TOP_LEFT", "BOTTOM_RIGHT"), 
            10
        );

        assertEquals("evento-sanidad-123", request.eventId());
        assertEquals("j1", request.jugadorId());
        assertEquals(1, request.primeraBolaTocada());
        assertEquals(2, request.bolasEmbolsadas().size());
    }

    @Test
    @DisplayName("Crea un ResultadoTiroResponse válido")
    void resultadoTiroResponse() {
        // NOTA: Si ResultadoTiroResponse cambió en la Fase 3/4, esto debe ajustarse.
        // Asumiendo que sigue igual (6 parámetros).
        ResultadoTiroResponse response = new ResultadoTiroResponse(
            false, null, 1, true, false, "Tiro legal"
        );

        assertFalse(response.esFalta());
        assertEquals(1, response.puntos());
    }
}