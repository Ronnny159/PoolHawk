package com.poolhawk.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link ReglasConfig}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("ReglasConfig - configuración de reglas")
class ReglasConfigTest {

    @Nested
    @DisplayName("Fábricas")
    class Fabricas {

        @Test
        @DisplayName("porDefecto crea configuración estándar")
        void porDefecto() {
            ReglasConfig config = ReglasConfig.porDefecto();
            assertFalse(config.callShot());
            assertNull(config.shotClock());
            assertFalse(config.threeFoulsPenalty());
            assertEquals(50, config.puntajeObjetivo());
        }

        @Test
        @DisplayName("conCallShot activa call shot")
        void conCallShot() {
            ReglasConfig config = ReglasConfig.conCallShot();
            assertTrue(config.callShot());
            assertNull(config.shotClock());
            assertFalse(config.threeFoulsPenalty());
            assertEquals(50, config.puntajeObjetivo());
        }

        @Test
        @DisplayName("deTorneo activa call shot, shot clock y three fouls")
        void deTorneo() {
            ReglasConfig config = ReglasConfig.deTorneo();
            assertTrue(config.callShot());
            assertNotNull(config.shotClock());
            assertEquals(Duration.ofSeconds(45), config.shotClock());
            assertTrue(config.threeFoulsPenalty());
            assertEquals(50, config.puntajeObjetivo());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("Rechaza shot clock negativo")
        void rechazaShotClockNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> new ReglasConfig(false, Duration.ofSeconds(-1), false, 50));
        }

        @Test
        @DisplayName("Acepta shot clock cero")
        void aceptaShotClockCero() {
            ReglasConfig config = new ReglasConfig(false, Duration.ZERO, false, 50);
            assertEquals(Duration.ZERO, config.shotClock());
        }

        @Test
        @DisplayName("Rechaza puntaje objetivo cero")
        void rechazaPuntajeCero() {
            assertThrows(IllegalArgumentException.class,
                () -> new ReglasConfig(false, null, false, 0));
        }

        @Test
        @DisplayName("Rechaza puntaje objetivo negativo")
        void rechazaPuntajeNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> new ReglasConfig(false, null, false, -10));
        }

        @Test
        @DisplayName("Acepta puntaje objetivo mínimo de 1")
        void aceptaPuntajeUno() {
            ReglasConfig config = new ReglasConfig(false, null, false, 1);
            assertEquals(1, config.puntajeObjetivo());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("tieneShotClock false cuando es nulo")
        void sinShotClock() {
            assertFalse(ReglasConfig.porDefecto().tieneShotClock());
        }

        @Test
        @DisplayName("tieneShotClock true cuando está configurado")
        void conShotClock() {
            assertTrue(ReglasConfig.deTorneo().tieneShotClock());
        }
    }

    @Nested
    @DisplayName("Constante de puntaje por defecto")
    class ConstanteDefault {

        @Test
        @DisplayName("PUNTAJE_OBJETIVO_DEFECTO es 50")
        void valorDefecto() {
            assertEquals(50, ReglasConfig.PUNTAJE_OBJETIVO_DEFECTO);
        }
    }
}