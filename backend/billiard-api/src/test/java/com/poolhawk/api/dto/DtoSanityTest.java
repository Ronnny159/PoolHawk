package com.poolhawk.api.dto;

import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de sanidad de los DTOs de la API.
 *
 * <p>Los DTOs son records sin logica de negocio. Java genera
 * automaticamente los accesores, equals, hashCode y toString.
 * Estas pruebas verifican que los records se construyen
 * correctamente y que sus accesores devuelven los valores
 * esperados.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("DTOs - pruebas de sanidad")
class DtoSanityTest {

    @Nested
    @DisplayName("JugadorResponse")
    class JugadorResponseTest {

        @Test
        @DisplayName("Se construye y expone sus valores")
        void construccion() {
            JugadorResponse dto = new JugadorResponse("j1", "Ana");
            assertEquals("j1", dto.id());
            assertEquals("Ana", dto.nombre());
        }

        @Test
        @DisplayName("Igualdad por valor")
        void igualdad() {
            JugadorResponse a = new JugadorResponse("j1", "Ana");
            JugadorResponse b = new JugadorResponse("j1", "Ana");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    @Nested
    @DisplayName("CrearPartidaRequest")
    class CrearPartidaRequestTest {

        @Test
        @DisplayName("Se construye con datos validos")
        void construccion() {
            CrearPartidaRequest dto = new CrearPartidaRequest(
                TipoModalidad.BOLA_8,
                List.of("Ana"),
                List.of("Beto"));

            assertEquals(TipoModalidad.BOLA_8, dto.modalidad());
            assertEquals(1, dto.equipoA().size());
            assertEquals("Ana", dto.equipoA().get(0));
            assertEquals("Beto", dto.equipoB().get(0));
        }

        @Test
        @DisplayName("Acepta equipos de dos jugadores")
        void equiposDeDos() {
            CrearPartidaRequest dto = new CrearPartidaRequest(
                TipoModalidad.POR_PUNTOS,
                List.of("Ana", "Carla"),
                List.of("Beto", "Diego"));

            assertEquals(2, dto.equipoA().size());
            assertEquals(2, dto.equipoB().size());
        }
    }

    @Nested
    @DisplayName("PartidaResponse")
    class PartidaResponseTest {

        @Test
        @DisplayName("Se construye y expone sus valores")
        void construccion() {
            Instant ahora = Instant.now();
            // Cómo debería quedar tu test:
            PartidaResponse response = new PartidaResponse(
                "1", "BOLA_8", "EN_JUEGO",
                List.of(new JugadorResponse("1", "Ana")),
                List.of(new JugadorResponse("2", "Beto")),
                Map.of("Ana", 0, "Beto", 0),
                Map.of("Ana", "LISAS", "Beto", "RAYADAS"),
                Map.of("A", 0, "B", 0), // <--- AGREGAR ESTO (El total por equipo)
                "Ana",
                null,
                0,
                Instant.now()
            );

            assertEquals("1", response.id());
            assertEquals("BOLA_8", response.modalidad());
            assertEquals("EN_JUEGO", response.fase());
            assertEquals("Ana", response.jugadorActual());
            assertNull(response.ganador());
            assertEquals(0, response.cantidadTiros());
            assertEquals(ahora, response.fechaCreacion());
        }
    }

    @Nested
    @DisplayName("TiroEventRequest")
    class TiroEventRequestTest {

        @Test
        @DisplayName("Se construye con tiro completo")
        void construccion() {
            TiroEventRequest dto = new TiroEventRequest(
                "j1",
                3,
                List.of(3),
                List.of("TOP_LEFT"),
                5);

            assertEquals("j1", dto.jugadorId());
            assertEquals(3, dto.primeraBolaTocada());
            assertEquals(1, dto.bolasEmbolsadas().size());
            assertEquals(1, dto.troneras().size());
            assertEquals(5, dto.duracionSegundos());
        }

        @Test
        @DisplayName("Acepta nulls en campos opcionales")
        void camposOpcionalesNulos() {
            TiroEventRequest dto = new TiroEventRequest(
                "j1", null, null, null, null);

            assertNull(dto.primeraBolaTocada());
            assertNull(dto.bolasEmbolsadas());
            assertNull(dto.troneras());
            assertNull(dto.duracionSegundos());
        }
    }

    @Nested
    @DisplayName("ResultadoTiroResponse")
    class ResultadoTiroResponseTest {

        @Test
        @DisplayName("Representa un tiro legal")
        void tiroLegal() {
            ResultadoTiroResponse dto = new ResultadoTiroResponse(
                false, null, 3, false, false, "Tiro legal, 3 punto(s)");

            assertFalse(dto.esFalta());
            assertNull(dto.motivo());
            assertEquals(3, dto.puntos());
            assertFalse(dto.cambiaTurno());
            assertFalse(dto.esVictoria());
        }

        @Test
        @DisplayName("Representa una falta")
        void falta() {
            ResultadoTiroResponse dto = new ResultadoTiroResponse(
                true, "NO_TOCAR_BOLA", 0, true, false, "Falta: no toco bola");

            assertTrue(dto.esFalta());
            assertEquals("NO_TOCAR_BOLA", dto.motivo());
            assertEquals(0, dto.puntos());
            assertTrue(dto.cambiaTurno());
        }
    }

    @Nested
    @DisplayName("ErrorResponse")
    class ErrorResponseTest {

        @Test
        @DisplayName("Se construye y expone sus valores")
        void construccion() {
            Instant ahora = Instant.now();
            ErrorResponse dto = new ErrorResponse(
                ahora, 404, "Not Found", "Partida no encontrada", "/api/v1/matches/x");

            assertEquals(ahora, dto.timestamp());
            assertEquals(404, dto.status());
            assertEquals("Not Found", dto.error());
            assertEquals("Partida no encontrada", dto.message());
            assertEquals("/api/v1/matches/x", dto.path());
        }
    }
}