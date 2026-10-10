package com.poolhawk.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link ResultadoTiro}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("ResultadoTiro - veredicto del motor de reglas")
class ResultadoTiroTest {

    @Nested
    @DisplayName("Constructor y validaciones")
    class Constructor {

        @Test
        @DisplayName("Crea un resultado de tiro legal sin falta")
        void creaLegal() {
            ResultadoTiro resultado = new ResultadoTiro(
                false, null, 5, false, false, "Tiro legal");

            assertFalse(resultado.esFalta());
            assertNull(resultado.motivo());
            assertEquals(5, resultado.puntos());
            assertFalse(resultado.cambiaTurno());
            assertFalse(resultado.esVictoria());
        }

        @Test
        @DisplayName("Rechaza descripción nula")
        void rechazaDescripcionNula() {
            assertThrows(IllegalArgumentException.class,
                () -> new ResultadoTiro(false, null, 0, false, false, null));
        }

        @Test
        @DisplayName("Rechaza descripción vacía")
        void rechazaDescripcionVacia() {
            assertThrows(IllegalArgumentException.class,
                () -> new ResultadoTiro(false, null, 0, false, false, ""));
        }

        @Test
        @DisplayName("Rechaza falta sin motivo")
        void rechazaFaltaSinMotivo() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new ResultadoTiro(true, null, 0, true, false, "Falta")
            );
            assertTrue(ex.getMessage().contains("motivo"));
        }

        @Test
        @DisplayName("Rechaza tiro legal con motivo")
        void rechazaLegalConMotivo() {
            assertThrows(IllegalArgumentException.class,
                () -> new ResultadoTiro(
                    false, FaltaMotivo.NO_TOCAR_BOLA, 0, false, false, "Legal"));
        }

        @Test
        @DisplayName("Rechaza falta con puntos distintos de cero")
        void rechazaFaltaConPuntos() {
            assertThrows(IllegalArgumentException.class,
                () -> new ResultadoTiro(
                    true, FaltaMotivo.NO_TOCAR_BOLA, 5, true, false, "Falta"));
        }

        @Test
        @DisplayName("Rechaza puntos negativos")
        void rechazaPuntosNegativos() {
            assertThrows(IllegalArgumentException.class,
                () -> new ResultadoTiro(false, null, -1, false, false, "Negativo"));
        }
    }

    @Nested
    @DisplayName("Fábrica tiroLegalSinPuntos")
    class FabricaTiroLegalSinPuntos {

        @Test
        @DisplayName("Crea un resultado legal sin puntos")
        void creaResultado() {
            ResultadoTiro resultado = ResultadoTiro.tiroLegalSinPuntos();

            assertFalse(resultado.esFalta());
            assertNull(resultado.motivo());
            assertEquals(0, resultado.puntos());
            assertFalse(resultado.cambiaTurno());
            assertFalse(resultado.esVictoria());
            assertTrue(resultado.fueNeutro());
        }
    }

    @Nested
    @DisplayName("Fábrica tiroLegalConPuntos")
    class FabricaTiroLegalConPuntos {

        @Test
        @DisplayName("Crea resultado con puntos válidos")
        void creaResultado() {
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            assertFalse(resultado.esFalta());
            assertEquals(3, resultado.puntos());
            assertTrue(resultado.fueExitoso());
        }

        @Test
        @DisplayName("Rechaza 0 puntos")
        void rechazaCero() {
            assertThrows(IllegalArgumentException.class,
                () -> ResultadoTiro.tiroLegalConPuntos(0));
        }

        @Test
        @DisplayName("Rechaza puntos negativos")
        void rechazaNegativos() {
            assertThrows(IllegalArgumentException.class,
                () -> ResultadoTiro.tiroLegalConPuntos(-1));
        }

        @Test
        @DisplayName("Acepta 1 punto como mínimo")
        void aceptaUno() {
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(1);
            assertEquals(1, resultado.puntos());
        }
    }

    @Nested
    @DisplayName("Fábrica falta")
    class FabricaFalta {

        @Test
        @DisplayName("Crea falta con motivo")
        void creaFalta() {
            ResultadoTiro resultado = ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);

            assertTrue(resultado.esFalta());
            assertEquals(FaltaMotivo.BLANCA_EMBOLSADA, resultado.motivo());
            assertEquals(0, resultado.puntos());
            assertTrue(resultado.cambiaTurno());
            assertFalse(resultado.esVictoria());
        }

        @Test
        @DisplayName("La descripción incluye el motivo")
        void descripcionConMotivo() {
            ResultadoTiro resultado = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
            assertTrue(resultado.descripcion().contains("no tocó"));
        }

        @Test
        @DisplayName("Rechaza motivo nulo")
        void rechazaMotivoNulo() {
            assertThrows(NullPointerException.class,
                () -> ResultadoTiro.falta(null));
        }
    }

    @Nested
    @DisplayName("Fábrica victoria")
    class FabricaVictoria {

        @Test
        @DisplayName("Crea victoria con puntos")
        void creaVictoria() {
            ResultadoTiro resultado = ResultadoTiro.victoria(5);

            assertFalse(resultado.esFalta());
            assertEquals(5, resultado.puntos());
            assertTrue(resultado.cambiaTurno());
            assertTrue(resultado.esVictoria());
        }

        @Test
        @DisplayName("Crea victoria con 0 puntos")
        void victoriaSinPuntos() {
            ResultadoTiro resultado = ResultadoTiro.victoria(0);
            assertTrue(resultado.esVictoria());
            assertEquals(0, resultado.puntos());
        }

        @Test
        @DisplayName("Rechaza puntos negativos")
        void rechazaNegativos() {
            assertThrows(IllegalArgumentException.class,
                () -> ResultadoTiro.victoria(-1));
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("fueExitoso con tiro legal con puntos")
        void exitoso() {
            assertTrue(ResultadoTiro.tiroLegalConPuntos(2).fueExitoso());
            assertFalse(ResultadoTiro.tiroLegalConPuntos(2).fueNeutro());
        }

        @Test
        @DisplayName("fueNeutro con tiro legal sin puntos")
        void neutro() {
            assertTrue(ResultadoTiro.tiroLegalSinPuntos().fueNeutro());
            assertFalse(ResultadoTiro.tiroLegalSinPuntos().fueExitoso());
        }

        @Test
        @DisplayName("Falta no es exitosa ni neutra")
        void faltaNoEsNinguna() {
            ResultadoTiro falta = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
            assertFalse(falta.fueExitoso());
            assertFalse(falta.fueNeutro());
        }

        @Test
        @DisplayName("Victoria no es neutra")
        void victoriaNoEsNeutra() {
            ResultadoTiro victoria = ResultadoTiro.victoria(0);
            assertFalse(victoria.fueNeutro());
        }
    }

    @Test
    @DisplayName("Las cuatro categorías son mutuamente excluyentes")
    void categoriasExcluyentes() {
        ResultadoTiro neutro = ResultadoTiro.tiroLegalSinPuntos();
        ResultadoTiro exitoso = ResultadoTiro.tiroLegalConPuntos(3);
        ResultadoTiro falta = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
        ResultadoTiro victoria = ResultadoTiro.victoria(2);

        // Neutro: solo fueNeutro
        assertTrue(neutro.fueNeutro());
        assertFalse(neutro.fueExitoso());
        assertFalse(neutro.esFalta());
        assertFalse(neutro.esVictoria());

        // Exitoso: solo fueExitoso
        assertFalse(exitoso.fueNeutro());
        assertTrue(exitoso.fueExitoso());
        assertFalse(exitoso.esFalta());
        assertFalse(exitoso.esVictoria());

        // Falta: solo esFalta
        assertFalse(falta.fueNeutro());
        assertFalse(falta.fueExitoso());
        assertTrue(falta.esFalta());
        assertFalse(falta.esVictoria());

        // Victoria: solo esVictoria
        assertFalse(victoria.fueNeutro());
        assertFalse(victoria.fueExitoso());
        assertFalse(victoria.esFalta());
        assertTrue(victoria.esVictoria());
    }
}