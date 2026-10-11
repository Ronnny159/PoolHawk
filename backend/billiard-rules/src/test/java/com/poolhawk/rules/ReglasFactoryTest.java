package com.poolhawk.rules;

import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link ReglasFactory}.
 *
 * <p>Como las implementaciones concretas aún no existen, esta prueba
 * valida el contrato de la fábrica: que rechace entradas nulas y que
 * informe claramente qué modalidades aún no están disponibles.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("ReglasFactory - creación de reglas")
class ReglasFactoryTest {

    @Nested
    @DisplayName("Validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("Rechaza modalidad nula")
        void rechazaModalidadNula() {
            assertThrows(NullPointerException.class,
                () -> ReglasFactory.crear(null, ReglasConfig.porDefecto()));
        }

        @Test
        @DisplayName("Rechaza configuración nula")
        void rechazaConfigNula() {
            assertThrows(NullPointerException.class,
                () -> ReglasFactory.crear(TipoModalidad.BOLA_8, null));
        }

        @Test
        @DisplayName("No se puede instanciar la fábrica")
        void noInstanciable() {
            assertThrows(Exception.class, ReglasFactory::new);
        }
    }

    @Nested
    @DisplayName("Modalidades no implementadas aún")
    class NoImplementadas {

        @Test
        @DisplayName("Bola 8 aún no está implementada")
        void bola8NoImplementada() { 
            UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> ReglasFactory.crear(TipoModalidad.BOLA_8)
            );
            assertTrue(ex.getMessage().contains("feature/rules-bola8"));
        }

        @Test
        @DisplayName("Por Puntos aún no está implementada")
        void porPuntosNoImplementada() {
            UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> ReglasFactory.crear(TipoModalidad.POR_PUNTOS)
            );
            assertTrue(ex.getMessage().contains("feature/rules-porpuntos"));
        }
    }
}