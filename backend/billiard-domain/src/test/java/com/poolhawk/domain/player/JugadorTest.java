package com.poolhawk.domain.player;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Jugador}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Jugador - persona que juega billar")
class JugadorTest {

    @Nested
    @DisplayName("Creación y validación")
    class Creacion {

        @Test
        @DisplayName("Crea un jugador válido")
        void creaJugadorValido() {
            Jugador jugador = new Jugador("j1", "Ana Pérez");
            assertEquals("j1", jugador.id());
            assertEquals("Ana Pérez", jugador.nombre());
        }

        @Test
        @DisplayName("Rechaza id nulo")
        void rechazaIdNulo() {
            assertThrows(NullPointerException.class,
                () -> new Jugador(null, "Ana"));
        }

        @Test
        @DisplayName("Rechaza nombre nulo")
        void rechazaNombreNulo() {
            assertThrows(NullPointerException.class,
                () -> new Jugador("j1", null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t"})
        @DisplayName("Rechaza id en blanco")
        void rechazaIdEnBlanco(String idInvalido) {
            assertThrows(IllegalArgumentException.class,
                () -> new Jugador(idInvalido, "Ana"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t"})
        @DisplayName("Rechaza nombre en blanco")
        void rechazaNombreEnBlanco(String nombreInvalido) {
            assertThrows(IllegalArgumentException.class,
                () -> new Jugador("j1", nombreInvalido));
        }

        @Test
        @DisplayName("Rechaza nombre mayor a 80 caracteres")
        void rechazaNombreLargo() {
            String nombreLargo = "A".repeat(81);
            assertThrows(IllegalArgumentException.class,
                () -> new Jugador("j1", nombreLargo));
        }

        @Test
        @DisplayName("Acepta nombre de exactamente 80 caracteres")
        void aceptaNombreLimite() {
            String nombreLimite = "A".repeat(80);
            Jugador jugador = new Jugador("j1", nombreLimite);
            assertEquals(80, jugador.nombre().length());
        }

        @Test
        @DisplayName("Normaliza espacios al inicio y fin del nombre")
        void normalizaEspacios() {
            Jugador jugador = new Jugador("j1", "  Ana Pérez  ");
            assertEquals("Ana Pérez", jugador.nombre());
        }

        @Test
        @DisplayName("Permite nombres con acentos y caracteres especiales")
        void permiteAcentos() {
            Jugador jugador = new Jugador("j1", "José Muñoz Gómez");
            assertEquals("José Muñoz Gómez", jugador.nombre());
        }
    }

    @Nested
    @DisplayName("Renombrar")
    class Renombrar {

        @Test
        @DisplayName("Renombrar retorna nuevo jugador con el nuevo nombre")
        void renombrarCambiaNombre() {
            Jugador original = new Jugador("j1", "Ana");
            Jugador renombrado = original.renombrar("Ana Pérez");

            assertEquals("Ana Pérez", renombrado.nombre());
            assertEquals("j1", renombrado.id());
        }

        @Test
        @DisplayName("Renombrar no modifica el jugador original")
        void renombrarNoModificaOriginal() {
            Jugador original = new Jugador("j1", "Ana");
            original.renombrar("Ana Pérez");

            assertEquals("Ana", original.nombre());
        }

        @Test
        @DisplayName("Renombrar con nombre inválido lanza excepción")
        void renombrarInvalido() {
            Jugador original = new Jugador("j1", "Ana");
            assertThrows(IllegalArgumentException.class,
                () -> original.renombrar(""));
        }
    }

    @Nested
    @DisplayName("Igualdad por valor")
    class Igualdad {

        @Test
        @DisplayName("Dos jugadores con mismo id y nombre son iguales")
        void igualdadPorValor() {
            Jugador a = new Jugador("j1", "Ana");
            Jugador b = new Jugador("j1", "Ana");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Dos jugadores con distinto id no son iguales")
        void distintoId() {
            Jugador a = new Jugador("j1", "Ana");
            Jugador b = new Jugador("j2", "Ana");
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("Dos jugadores con distinto nombre no son iguales")
        void distintoNombre() {
            Jugador a = new Jugador("j1", "Ana");
            Jugador b = new Jugador("j1", "Beatriz");
            assertNotEquals(a, b);
        }
    }
}