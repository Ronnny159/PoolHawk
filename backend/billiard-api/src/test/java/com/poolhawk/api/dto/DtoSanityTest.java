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
            List.of("Ana", "Beto", "Carlos")
        );

        assertEquals(TipoModalidad.BOLA_8, request.modalidad());
        assertEquals(FormatoJuego.INDIVIDUAL, request.formato());
        assertEquals(3, request.jugadores().size());
        assertEquals("Ana", request.jugadores().get(0));
    }

    @Test
    @DisplayName("Crea un TiroEventRequest válido")
    void tiroEventRequest() {
        TiroEventRequest request = new TiroEventRequest(
            "j1", 1, List.of(1, 2), List.of("TOP_LEFT", "BOTTOM_RIGHT"), 10
        );

        assertEquals("j1", request.jugadorId());
        assertEquals(1, request.primeraBolaTocada());
        assertEquals(2, request.bolasEmbolsadas().size());
    }

    @Test
    @DisplayName("Crea un ResultadoTiroResponse válido")
    void resultadoTiroResponse() {
        ResultadoTiroResponse response = new ResultadoTiroResponse(
            false, null, 1, true, false, "Tiro legal"
        );

        assertFalse(response.esFalta());
        assertEquals(1, response.puntos());
    }
}