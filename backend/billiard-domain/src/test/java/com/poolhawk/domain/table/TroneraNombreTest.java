package com.poolhawk.domain.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link TroneraNombre}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("TroneraNombre - nombres de las troneras")
class TroneraNombreTest {

    @Nested
    @DisplayName("Clasificación de troneras")
    class Clasificacion {

        @ParameterizedTest
        @EnumSource(value = TroneraNombre.class,
                    names = {"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"})
        @DisplayName("Las cuatro esquinas se reconocen como de esquina")
        void esquinasSonDeEsquina(TroneraNombre nombre) {
            assertTrue(nombre.esDeEsquina());
            assertFalse(nombre.esLateral());
        }

        @ParameterizedTest
        @EnumSource(value = TroneraNombre.class,
                    names = {"MIDDLE_LEFT", "MIDDLE_RIGHT"})
        @DisplayName("Las dos laterales se reconocen como laterales")
        void lateralesSonLaterales(TroneraNombre nombre) {
            assertTrue(nombre.esLateral());
            assertFalse(nombre.esDeEsquina());
        }

        @ParameterizedTest
        @EnumSource(value = TroneraNombre.class,
                    names = {"TOP_LEFT", "TOP_RIGHT"})
        @DisplayName("Las superiores se reconocen como superiores")
        void superiores(TroneraNombre nombre) {
            assertTrue(nombre.esSuperior());
            assertFalse(nombre.esInferior());
        }

        @ParameterizedTest
        @EnumSource(value = TroneraNombre.class,
                    names = {"BOTTOM_LEFT", "BOTTOM_RIGHT"})
        @DisplayName("Las inferiores se reconocen como inferiores")
        void inferiores(TroneraNombre nombre) {
            assertTrue(nombre.esInferior());
            assertFalse(nombre.esSuperior());
        }
    }

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("El enum tiene exactamente 6 valores")
        void seisValores() {
            assertEquals(6, TroneraNombre.values().length);
        }

        @ParameterizedTest
        @EnumSource(TroneraNombre.class)
        @DisplayName("Cada valor tiene nombre visible en español")
        void todosTienenNombreVisible(TroneraNombre nombre) {
            assertNotNull(nombre.getNombreVisible());
            assertFalse(nombre.getNombreVisible().isBlank());
        }
    }
}

