package com.poolhawk.domain.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link TipoModalidad}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("TipoModalidad - modalidades de billar soportadas")
class TipoModalidadTest {

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("El enum tiene exactamente 2 valores")
        void dosValores() {
            assertEquals(2, TipoModalidad.values().length);
        }

        @ParameterizedTest
        @EnumSource(TipoModalidad.class)
        @DisplayName("Todos tienen nombre visible no vacío")
        void todosConNombre(TipoModalidad modalidad) {
            assertNotNull(modalidad.getNombreVisible());
            assertFalse(modalidad.getNombreVisible().isBlank());
        }

        @Test
        @DisplayName("Bola 8 tiene 2 jugadores y es individual")
        void bola8Individual() {
            assertEquals(2, TipoModalidad.BOLA_8.getCantidadJugadores());
            assertTrue(TipoModalidad.BOLA_8.esIndividual());
            assertFalse(TipoModalidad.BOLA_8.esPorEquipos());
        }

        @Test
        @DisplayName("Por Puntos tiene 4 jugadores y es por equipos")
        void porPuntosEquipos() {
            assertEquals(4, TipoModalidad.POR_PUNTOS.getCantidadJugadores());
            assertTrue(TipoModalidad.POR_PUNTOS.esPorEquipos());
            assertFalse(TipoModalidad.POR_PUNTOS.esIndividual());
        }

        @Test
        @DisplayName("Nombres visibles correctos")
        void nombresVisibles() {
            assertEquals("Bola 8", TipoModalidad.BOLA_8.getNombreVisible());
            assertEquals("Por Puntos", TipoModalidad.POR_PUNTOS.getNombreVisible());
        }
    }

    @Nested
    @DisplayName("Coherencia interna")
    class Coherencia {

        @ParameterizedTest
        @EnumSource(TipoModalidad.class)
        @DisplayName("esIndividual y esPorEquipos son mutuamente excluyentes")
        void mutuamenteExcluyentes(TipoModalidad modalidad) {
            assertNotEquals(modalidad.esIndividual(), modalidad.esPorEquipos());
        }

        @ParameterizedTest
        @EnumSource(TipoModalidad.class)
        @DisplayName("Cantidad de jugadores siempre par (2 o 4)")
        void cantidadPar(TipoModalidad modalidad) {
            assertEquals(0, modalidad.getCantidadJugadores() % 2);
        }
    }
}