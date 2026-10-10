package com.poolhawk.domain.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link TipoBola}.
 *
 * <p>Verifica la derivación del tipo a partir del número de bola, las
 * propiedades de cada tipo y la clasificación de grupos.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("TipoBola - clasificación de bolas por patrón")
class TipoBolaTest {

    @Nested
    @DisplayName("Derivación desde número")
    class DerivacionDesdeNumero {

        @ParameterizedTest(name = "Número {0} corresponde a {1}")
        @CsvSource({
            "0, BLANCA",
            "1, LISA",
            "2, LISA",
            "3, LISA",
            "4, LISA",
            "5, LISA",
            "6, LISA",
            "7, LISA",
            "8, OCHO",
            "9, RAYADA",
            "10, RAYADA",
            "11, RAYADA",
            "12, RAYADA",
            "13, RAYADA",
            "14, RAYADA",
            "15, RAYADA"
        })
        @DisplayName("Deriva el tipo correcto para cada número del 0 al 15")
        void derivaTipoCorrecto(int numero, TipoBola esperado) {
            assertEquals(esperado, TipoBola.desdeNumero(numero));
        }

        @Test
        @DisplayName("Rechaza número negativo")
        void rechazaNumeroNegativo() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> TipoBola.desdeNumero(-1)
            );
            assertTrue(ex.getMessage().contains("entre 0 y 15"));
        }

        @Test
        @DisplayName("Rechaza número mayor a 15")
        void rechazaNumeroMayorA15() {
            assertThrows(IllegalArgumentException.class,
                () -> TipoBola.desdeNumero(16));
        }
    }

    @Nested
    @DisplayName("Clasificación de grupos")
    class ClasificacionDeGrupos {

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
        @DisplayName("Las bolas del 1 al 7 son de grupo (lisas)")
        void lisasSonDeGrupo(int numero) {
            assertTrue(TipoBola.desdeNumero(numero).esDeGrupo());
        }

        @ParameterizedTest
        @ValueSource(ints = {9, 10, 11, 12, 13, 14, 15})
        @DisplayName("Las bolas del 9 al 15 son de grupo (rayadas)")
        void rayadasSonDeGrupo(int numero) {
            assertTrue(TipoBola.desdeNumero(numero).esDeGrupo());
        }

        @Test
        @DisplayName("La bola 8 no es de grupo")
        void ochoNoEsDeGrupo() {
            assertFalse(TipoBola.OCHO.esDeGrupo());
        }

        @Test
        @DisplayName("La bola blanca no es de grupo")
        void blancaNoEsDeGrupo() {
            assertFalse(TipoBola.BLANCA.esDeGrupo());
        }
    }

    @Nested
    @DisplayName("Bola final")
    class BolaFinal {

        @Test
        @DisplayName("Solo la bola OCHO es bola final")
        void soloOchoEsFinal() {
            assertTrue(TipoBola.OCHO.esBolaFinal());
            assertFalse(TipoBola.LISA.esBolaFinal());
            assertFalse(TipoBola.RAYADA.esBolaFinal());
            assertFalse(TipoBola.BLANCA.esBolaFinal());
        }
    }

    @Nested
    @DisplayName("Propiedades de cada tipo")
    class Propiedades {

        @Test
        @DisplayName("Cada tipo tiene nombre visible en español")
        void nombresVisibles() {
            assertEquals("Blanca", TipoBola.BLANCA.getNombreVisible());
            assertEquals("Lisa", TipoBola.LISA.getNombreVisible());
            assertEquals("Ocho", TipoBola.OCHO.getNombreVisible());
            assertEquals("Rayada", TipoBola.RAYADA.getNombreVisible());
        }

        @Test
        @DisplayName("Cada tipo tiene su primer número correcto")
        void primerosNumeros() {
            assertEquals(0, TipoBola.BLANCA.getPrimerNumero());
            assertEquals(1, TipoBola.LISA.getPrimerNumero());
            assertEquals(8, TipoBola.OCHO.getPrimerNumero());
            assertEquals(9, TipoBola.RAYADA.getPrimerNumero());
        }
    }
}