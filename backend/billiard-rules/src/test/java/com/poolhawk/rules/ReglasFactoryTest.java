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
        @DisplayName("No se puede instanciar la fábrica por reflexión")
        void noInstanciable() throws NoSuchMethodException {
            var constructor = ReglasFactory.class.getDeclaredConstructor();
            assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
                "El constructor de ReglasFactory debe ser privado");
        }
    }

    @Nested
    @DisplayName("Modalidades")
    class Modalidades {

        @Test
        @DisplayName("Bola 8 se crea correctamente")
        void bola8Implementada() {
            ReglasBillar reglas = ReglasFactory.crear(TipoModalidad.BOLA_8);
            assertNotNull(reglas);
            assertEquals("Bola 8", reglas.getNombreModalidad());
        }

        @Test
        @DisplayName("Por Puntos se crea correctamente")
        void porPuntosImplementada() {
            ReglasBillar reglas = ReglasFactory.crear(TipoModalidad.POR_PUNTOS);
            assertNotNull(reglas);
            assertEquals("Por Puntos", reglas.getNombreModalidad());
        }
    }
}