package com.poolhawk.domain.match;

import com.poolhawk.domain.table.TipoBola;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Grupo}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Grupo - asignación de bolas en Bola 8")
class GrupoTest {

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("El enum tiene exactamente 3 valores")
        void tresValores() {
            assertEquals(3, Grupo.values().length);
        }

        @Test
        @DisplayName("Nombres visibles correctos")
        void nombresVisibles() {
            assertEquals("Lisas", Grupo.LISAS.getNombreVisible());
            assertEquals("Rayadas", Grupo.RAYADAS.getNombreVisible());
            assertEquals("Sin asignar", Grupo.SIN_ASIGNAR.getNombreVisible());
        }

        @Test
        @DisplayName("Lisas y Rayadas tienen tipo de bola asociado")
        void tiposAsociados() {
            assertEquals(TipoBola.LISA, Grupo.LISAS.getTipoBolaAsociado());
            assertEquals(TipoBola.RAYADA, Grupo.RAYADAS.getTipoBolaAsociado());
        }

        @Test
        @DisplayName("SIN_ASIGNAR no tiene tipo de bola asociado")
        void sinAsignarSinTipo() {
            assertNull(Grupo.SIN_ASIGNAR.getTipoBolaAsociado());
        }
    }

    @Nested
    @DisplayName("Estado de asignación")
    class EstadoAsignacion {

        @Test
        @DisplayName("LISAS y RAYADAS están asignados")
        void asignados() {
            assertTrue(Grupo.LISAS.estaAsignado());
            assertTrue(Grupo.RAYADAS.estaAsignado());
        }

        @Test
        @DisplayName("SIN_ASIGNAR no está asignado")
        void sinAsignar() {
            assertFalse(Grupo.SIN_ASIGNAR.estaAsignado());
        }
    }

    @Nested
    @DisplayName("Grupo opuesto")
    class Opuesto {

        @Test
        @DisplayName("El opuesto de LISAS es RAYADAS")
        void opuestoDeLisas() {
            assertEquals(Grupo.RAYADAS, Grupo.LISAS.opuesto());
        }

        @Test
        @DisplayName("El opuesto de RAYADAS es LISAS")
        void opuestoDeRayadas() {
            assertEquals(Grupo.LISAS, Grupo.RAYADAS.opuesto());
        }

        @Test
        @DisplayName("El opuesto de SIN_ASIGNAR lanza excepción")
        void opuestoDeSinAsignar() {
            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> Grupo.SIN_ASIGNAR.opuesto()
            );
            assertTrue(ex.getMessage().contains("sin asignar"));
        }

        @Test
        @DisplayName("El opuesto del opuesto es el original")
        void dobleOpuesto() {
            assertEquals(Grupo.LISAS, Grupo.LISAS.opuesto().opuesto());
            assertEquals(Grupo.RAYADAS, Grupo.RAYADAS.opuesto().opuesto());
        }
    }

    @Nested
    @DisplayName("Derivación desde tipo de bola")
    class DerivacionDesdeTipo {

        @Test
        @DisplayName("LISA deriva a LISAS")
        void lisaDeriva() {
            assertEquals(Grupo.LISAS, Grupo.desdeTipoBola(TipoBola.LISA));
        }

        @Test
        @DisplayName("RAYADA deriva a RAYADAS")
        void rayadaDeriva() {
            assertEquals(Grupo.RAYADAS, Grupo.desdeTipoBola(TipoBola.RAYADA));
        }

        @Test
        @DisplayName("BOLA OCHO no tiene grupo y lanza excepción")
        void ochoSinGrupo() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> Grupo.desdeTipoBola(TipoBola.OCHO)
            );
            assertTrue(ex.getMessage().contains("LISA o RAYADA"));
        }

        @Test
        @DisplayName("BLANCA no tiene grupo y lanza excepción")
        void blancaSinGrupo() {
            assertThrows(IllegalArgumentException.class,
                () -> Grupo.desdeTipoBola(TipoBola.BLANCA));
        }

        @Test
        @DisplayName("Nulo lanza NullPointerException o IllegalArgumentException")
        void nulo() {
            assertThrows(Exception.class,
                () -> Grupo.desdeTipoBola(null));
        }
    }

    @Nested
    @DisplayName("Coherencia")
    class Coherencia {

        @ParameterizedTest
        @EnumSource(value = Grupo.class, names = {"LISAS", "RAYADAS"})
        @DisplayName("Grupos asignados tienen tipo de bola no nulo")
        void gruposAsignadosConTipo(Grupo grupo) {
            assertNotNull(grupo.getTipoBolaAsociado());
            assertTrue(grupo.estaAsignado());
        }
    }
}