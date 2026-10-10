package com.poolhawk.domain.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link FaltaMotivo}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("FaltaMotivo - motivos de falta en billar")
class FaltaMotivoTest {

    @Nested
    @DisplayName("Propiedades")
    class Propiedades {

        @Test
        @DisplayName("El enum tiene al menos 10 valores")
        void alMenosDiez() {
            assertTrue(FaltaMotivo.values().length >= 10);
        }

        @ParameterizedTest
        @EnumSource(FaltaMotivo.class)
        @DisplayName("Todos los motivos tienen descripción no vacía")
        void todosConDescripcion(FaltaMotivo motivo) {
            assertNotNull(motivo.getDescripcion());
            assertFalse(motivo.getDescripcion().isBlank());
        }

        @Test
        @DisplayName("Descripciones específicas correctas")
        void descripcionesCorrectas() {
            assertEquals("La bola blanca no tocó ninguna bola",
                FaltaMotivo.NO_TOCAR_BOLA.getDescripcion());
            assertEquals("La bola blanca fue embolsada",
                FaltaMotivo.BLANCA_EMBOLSADA.getDescripcion());
        }
    }

    @Nested
    @DisplayName("Clasificación por modalidad")
    class Clasificacion {

        @Test
        @DisplayName("BOLA_8_ANTES_DE_TIEMPO es exclusiva de Bola 8")
        void bola8AntesEsExclusiva() {
            assertTrue(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO.esExclusivaDeBola8());
            assertFalse(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO.esExclusivaDePorPuntos());
            assertFalse(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO.esComun());
        }

        @Test
        @DisplayName("BOLA_8_TRONERA_INCORRECTA es exclusiva de Bola 8")
        void bola8TroneraEsExclusiva() {
            assertTrue(FaltaMotivo.BOLA_8_TRONERA_INCORRECTA.esExclusivaDeBola8());
        }

        @Test
        @DisplayName("APERTURA_INVALIDA es exclusiva de Bola 8")
        void aperturaEsExclusiva() {
            assertTrue(FaltaMotivo.APERTURA_INVALIDA.esExclusivaDeBola8());
        }

        @Test
        @DisplayName("ORDEN_INCORRECTO es exclusiva de Por Puntos")
        void ordenEsExclusiva() {
            assertTrue(FaltaMotivo.ORDEN_INCORRECTO.esExclusivaDePorPuntos());
            assertFalse(FaltaMotivo.ORDEN_INCORRECTO.esExclusivaDeBola8());
            assertFalse(FaltaMotivo.ORDEN_INCORRECTO.esComun());
        }

        @Test
        @DisplayName("NO_TOCAR_BOLA es común")
        void noTocarComun() {
            assertTrue(FaltaMotivo.NO_TOCAR_BOLA.esComun());
            assertFalse(FaltaMotivo.NO_TOCAR_BOLA.esExclusivaDeBola8());
            assertFalse(FaltaMotivo.NO_TOCAR_BOLA.esExclusivaDePorPuntos());
        }

        @Test
        @DisplayName("BLANCA_EMBOLSADA es común")
        void blancaComun() {
            assertTrue(FaltaMotivo.BLANCA_EMBOLSADA.esComun());
        }

        @ParameterizedTest
        @EnumSource(FaltaMotivo.class)
        @DisplayName("Cada motivo pertenece a una y solo una categoría")
        void categoriaUnica(FaltaMotivo motivo) {
            int categorias = 0;
            if (motivo.esExclusivaDeBola8()) categorias++;
            if (motivo.esExclusivaDePorPuntos()) categorias++;
            if (motivo.esComun()) categorias++;

            assertEquals(1, categorias,
                "El motivo " + motivo + " debe pertenecer a exactamente una categoría");
        }
    }

    @Nested
    @DisplayName("Cobertura de categorías")
    class Cobertura {

        @Test
        @DisplayName("Existen motivos exclusivos de Bola 8")
        void hayExclusivosBola8() {
            long cantidad = java.util.Arrays.stream(FaltaMotivo.values())
                .filter(FaltaMotivo::esExclusivaDeBola8)
                .count();
            assertTrue(cantidad >= 3);
        }

        @Test
        @DisplayName("Existe al menos un motivo exclusivo de Por Puntos")
        void hayExclusivosPorPuntos() {
            long cantidad = java.util.Arrays.stream(FaltaMotivo.values())
                .filter(FaltaMotivo::esExclusivaDePorPuntos)
                .count();
            assertTrue(cantidad >= 1);
        }

        @Test
        @DisplayName("Existen motivos comunes")
        void hayComunes() {
            long cantidad = java.util.Arrays.stream(FaltaMotivo.values())
                .filter(FaltaMotivo::esComun)
                .count();
            assertTrue(cantidad >= 5);
        }
    }
}