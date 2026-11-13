package com.poolhawk.domain.event;

import com.poolhawk.domain.match.Grupo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResultadoTiro - veredicto del motor de reglas")
class ResultadoTiroTest {

    @Nested
    @DisplayName("Constructor y validaciones")
    class Constructor {

        @Test
        @DisplayName("Rechaza descripción nula")
        void rechazaDescripcionNula() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                false, null, 0, false, false, false, null, null));
        }

        @Test
        @DisplayName("Rechaza descripción en blanco")
        void rechazaDescripcionBlanca() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                false, null, 0, false, false, false, null, "   "));
        }

        @Test
        @DisplayName("Rechaza falta sin motivo")
        void rechazaFaltaSinMotivo() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                true, null, 0, true, false, false, null, "Falta"));
        }

        @Test
        @DisplayName("Rechaza motivo en tiro legal que no es derrota")
        void rechazaMotivoEnTiroLegal() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                false, FaltaMotivo.NO_TOCAR_BOLA, 0, false, false, false, null, "Tiro legal"));
        }

        @Test
        @DisplayName("Rechaza falta con puntos mayores a cero")
        void rechazaFaltaConPuntos() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                true, FaltaMotivo.NO_TOCAR_BOLA, 5, true, false, false, null, "Falta"));
        }

        @Test
        @DisplayName("Rechaza derrota con puntos mayores a cero")
        void rechazaDerrotaConPuntos() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                false, FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO, 5, false, false, true, null, "Derrota"));
        }

        @Test
        @DisplayName("Rechaza puntos negativos")
        void rechazaPuntosNegativos() {
            assertThrows(IllegalArgumentException.class, () -> new ResultadoTiro(
                false, null, -1, false, false, false, null, "Tiro"));
        }

        @Test
        @DisplayName("Acepta derrota con motivo")
        void aceptaDerrotaConMotivo() {
            assertDoesNotThrow(() -> new ResultadoTiro(
                false, FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO, 0, false, false, true, null, "Derrota"));
        }
    }

    @Nested
    @DisplayName("Fábrica: tiroLegalSinPuntos")
    class FabricaTiroLegalSinPuntos {

        @Test
        @DisplayName("Crea un resultado correcto")
        void creaResultadoCorrecto() {
            ResultadoTiro r = ResultadoTiro.tiroLegalSinPuntos(true);
            assertFalse(r.esFalta());
            assertNull(r.motivo());
            assertEquals(0, r.puntos());
            assertTrue(r.cambiaTurno());
            assertFalse(r.esVictoria());
            assertFalse(r.esDerrota());
            assertNull(r.grupoAsignado());
        }
    }

    @Nested
    @DisplayName("Fábrica: tiroLegalConPuntos")
    class FabricaTiroLegalConPuntos {

        @Test
        @DisplayName("Crea un resultado con puntos sin grupo")
        void creaResultadoConPuntos() {
            ResultadoTiro r = ResultadoTiro.tiroLegalConPuntos(3);
            assertFalse(r.esFalta());
            assertEquals(3, r.puntos());
            assertFalse(r.cambiaTurno());
            assertFalse(r.esVictoria());
            assertFalse(r.esDerrota());
            assertNull(r.grupoAsignado());
        }

        @Test
        @DisplayName("Crea un resultado con puntos y grupo asignado")
        void creaResultadoConPuntosYGrupo() {
            ResultadoTiro r = ResultadoTiro.tiroLegalConPuntos(2, Grupo.LISAS);
            assertFalse(r.esFalta());
            assertEquals(2, r.puntos());
            assertFalse(r.cambiaTurno());
            assertFalse(r.esVictoria());
            assertFalse(r.esDerrota());
            assertEquals(Grupo.LISAS, r.grupoAsignado());
        }
    }

    @Nested
    @DisplayName("Fábrica: falta")
    class FabricaFalta {

        @Test
        @DisplayName("Crea una falta válida")
        void creaFaltaValida() {
            ResultadoTiro r = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.NO_TOCAR_BOLA, r.motivo());
            assertEquals(0, r.puntos());
            assertTrue(r.cambiaTurno());
            assertFalse(r.esVictoria());
            assertFalse(r.esDerrota());
            assertNull(r.grupoAsignado());
        }

        @Test
        @DisplayName("Rechaza motivo nulo")
        void rechazaMotivoNulo() {
            assertThrows(NullPointerException.class, () -> ResultadoTiro.falta(null));
        }
    }

    @Nested
    @DisplayName("Fábrica: victoria")
    class FabricaVictoria {

        @Test
        @DisplayName("Crea una victoria con puntos")
        void creaVictoriaConPuntos() {
            ResultadoTiro r = ResultadoTiro.victoria(10);
            assertFalse(r.esFalta());
            assertNull(r.motivo());
            assertEquals(10, r.puntos());
            // CORREGIDO: En victoria el turno NO cambia
            assertFalse(r.cambiaTurno());
            assertTrue(r.esVictoria());
            assertFalse(r.esDerrota());
            assertNull(r.grupoAsignado());
        }

        @Test
        @DisplayName("Crea una victoria sin puntos")
        void creaVictoriaSinPuntos() {
            ResultadoTiro r = ResultadoTiro.victoria(0);
            assertEquals(0, r.puntos());
            assertTrue(r.esVictoria());
            assertFalse(r.cambiaTurno());
        }

        @Test
        @DisplayName("Rechaza puntos negativos")
        void rechazaPuntosNegativos() {
            assertThrows(IllegalArgumentException.class, () -> ResultadoTiro.victoria(-5));
        }
    }

    @Nested
    @DisplayName("Fábrica: derrota")
    class FabricaDerrota {

        @Test
        @DisplayName("Crea una derrota válida")
        void creaDerrotaValida() {
            ResultadoTiro r = ResultadoTiro.derrota(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO);
            assertFalse(r.esFalta());
            assertEquals(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO, r.motivo());
            assertEquals(0, r.puntos());
            // CORREGIDO: En derrota el turno NO cambia
            assertFalse(r.cambiaTurno());
            assertFalse(r.esVictoria());
            assertTrue(r.esDerrota());
            assertNull(r.grupoAsignado());
        }

        @Test
        @DisplayName("Rechaza motivo nulo")
        void rechazaMotivoNulo() {
            assertThrows(NullPointerException.class, () -> ResultadoTiro.derrota(null));
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("fueExitoso true para tiro legal con puntos")
        void fueExitosoConPuntos() {
            ResultadoTiro r = ResultadoTiro.tiroLegalConPuntos(3);
            assertTrue(r.fueExitoso());
        }

        @Test
        @DisplayName("fueExitoso false para tiro legal sin puntos")
        void fueExitosoSinPuntos() {
            ResultadoTiro r = ResultadoTiro.tiroLegalSinPuntos(true);
            assertFalse(r.fueExitoso());
        }

        @Test
        @DisplayName("fueExitoso false para falta")
        void fueExitosoFalta() {
            ResultadoTiro r = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
            assertFalse(r.fueExitoso());
        }

        @Test
        @DisplayName("fueExitoso false para victoria")
        void fueExitosoVictoria() {
            ResultadoTiro r = ResultadoTiro.victoria(5);
            assertFalse(r.fueExitoso());
        }

        @Test
        @DisplayName("fueExitoso false para derrota")
        void fueExitosoDerrota() {
            ResultadoTiro r = ResultadoTiro.derrota(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO);
            assertFalse(r.fueExitoso());
        }
    }
}