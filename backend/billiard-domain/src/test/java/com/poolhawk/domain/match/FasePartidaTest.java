package com.poolhawk.domain.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link FasePartida}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("FasePartida - ciclo de vida de una partida")
class FasePartidaTest {

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("El enum tiene exactamente 5 valores")
        void cincoValores() {
            assertEquals(5, FasePartida.values().length);
        }

        @ParameterizedTest
        @EnumSource(FasePartida.class)
        @DisplayName("Todas las fases tienen nombre visible")
        void todosConNombre(FasePartida fase) {
            assertNotNull(fase.getNombreVisible());
            assertFalse(fase.getNombreVisible().isBlank());
        }

        @Test
        @DisplayName("Nombres visibles correctos")
        void nombresVisibles() {
            assertEquals("Creada", FasePartida.CREADA.getNombreVisible());
            assertEquals("En curso", FasePartida.EN_CURSO.getNombreVisible());
            assertEquals("Pausada", FasePartida.PAUSADA.getNombreVisible());
            assertEquals("Finalizada", FasePartida.FINALIZADA.getNombreVisible());
            assertEquals("Abandonada", FasePartida.ABANDONADA.getNombreVisible());
        }
    }

    @Nested
    @DisplayName("Clasificación de fases")
    class Clasificacion {

        @Test
        @DisplayName("FINALIZADA y ABANDONADA son terminales")
        void terminales() {
            assertTrue(FasePartida.FINALIZADA.esTerminal());
            assertTrue(FasePartida.ABANDONADA.esTerminal());
        }

        @Test
        @DisplayName("CREADA, EN_CURSO y PAUSADA no son terminales")
        void noTerminales() {
            assertFalse(FasePartida.CREADA.esTerminal());
            assertFalse(FasePartida.EN_CURSO.esTerminal());
            assertFalse(FasePartida.PAUSADA.esTerminal());
        }

        @Test
        @DisplayName("EN_CURSO y PAUSADA están activas")
        void activas() {
            assertTrue(FasePartida.EN_CURSO.estaActiva());
            assertTrue(FasePartida.PAUSADA.estaActiva());
        }

        @Test
        @DisplayName("CREADA, FINALIZADA y ABANDONADA no están activas")
        void noActivas() {
            assertFalse(FasePartida.CREADA.estaActiva());
            assertFalse(FasePartida.FINALIZADA.estaActiva());
            assertFalse(FasePartida.ABANDONADA.estaActiva());
        }

        @ParameterizedTest
        @EnumSource(FasePartida.class)
        @DisplayName("esTerminal y estaActiva son mutuamente excluyentes")
        void mutuamenteExcluyentes(FasePartida fase) {
            assertFalse(fase.esTerminal() && fase.estaActiva());
        }
    }

    @Nested
    @DisplayName("Transiciones válidas")
    class Transiciones {

        @Test
        @DisplayName("Desde CREADA solo se puede ir a EN_CURSO o ABANDONADA")
        void desdeCreada() {
            Set<FasePartida> validas = FasePartida.CREADA.transicionesValidas();
            assertEquals(2, validas.size());
            assertTrue(validas.contains(FasePartida.EN_CURSO));
            assertTrue(validas.contains(FasePartida.ABANDONADA));
        }

        @Test
        @DisplayName("Desde EN_CURSO se puede ir a PAUSADA, FINALIZADA o ABANDONADA")
        void desdeEnCurso() {
            Set<FasePartida> validas = FasePartida.EN_CURSO.transicionesValidas();
            assertEquals(3, validas.size());
            assertTrue(validas.contains(FasePartida.PAUSADA));
            assertTrue(validas.contains(FasePartida.FINALIZADA));
            assertTrue(validas.contains(FasePartida.ABANDONADA));
        }

        @Test
        @DisplayName("Desde PAUSADA se puede volver a EN_CURSO o abandonar")
        void desdePausada() {
            Set<FasePartida> validas = FasePartida.PAUSADA.transicionesValidas();
            assertEquals(2, validas.size());
            assertTrue(validas.contains(FasePartida.EN_CURSO));
            assertTrue(validas.contains(FasePartida.ABANDONADA));
        }

        @Test
        @DisplayName("Desde FINALIZADA no hay transiciones")
        void desdeFinalizada() {
            assertTrue(FasePartida.FINALIZADA.transicionesValidas().isEmpty());
        }

        @Test
        @DisplayName("Desde ABANDONADA no hay transiciones")
        void desdeAbandonada() {
            assertTrue(FasePartida.ABANDONADA.transicionesValidas().isEmpty());
        }
    }

    @Nested
    @DisplayName("Método puedeTransicionarA")
    class PuedeTransicionar {

        @Test
        @DisplayName("CREADA → EN_CURSO es válido")
        void creadaAEnCurso() {
            assertTrue(FasePartida.CREADA.puedeTransicionarA(FasePartida.EN_CURSO));
        }

        @Test
        @DisplayName("CREADA → FINALIZADA no es válido")
        void creadaAFinalizada() {
            assertFalse(FasePartida.CREADA.puedeTransicionarA(FasePartida.FINALIZADA));
        }

        @Test
        @DisplayName("EN_CURSO → PAUSADA es válido")
        void enCursoAPausada() {
            assertTrue(FasePartida.EN_CURSO.puedeTransicionarA(FasePartida.PAUSADA));
        }

        @Test
        @DisplayName("PAUSADA → EN_CURSO es válido")
        void pausadaAEnCurso() {
            assertTrue(FasePartida.PAUSADA.puedeTransicionarA(FasePartida.EN_CURSO));
        }

        @Test
        @DisplayName("FINALIZADA no puede transicionar a ninguna fase")
        void finalizadaSinSalida() {
            for (FasePartida destino : FasePartida.values()) {
                assertFalse(FasePartida.FINALIZADA.puedeTransicionarA(destino));
            }
        }

        @Test
        @DisplayName("ABANDONADA no puede transicionar a ninguna fase")
        void abandonadaSinSalida() {
            for (FasePartida destino : FasePartida.values()) {
                assertFalse(FasePartida.ABANDONADA.puedeTransicionarA(destino));
            }
        }

        @Test
        @DisplayName("Una fase no puede transicionar a sí misma")
        void sinAutoTransicion() {
            for (FasePartida fase : FasePartida.values()) {
                assertFalse(fase.puedeTransicionarA(fase),
                    "La fase " + fase + " no debería poder transicionar a sí misma");
            }
        }
    }
}