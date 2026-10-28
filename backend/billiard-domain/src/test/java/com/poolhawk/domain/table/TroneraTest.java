package com.poolhawk.domain.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Tronera}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Tronera - abertura de la mesa")
class TroneraTest {

    private static final Coordenada POSICION = new Coordenada(10.0, 10.0);

    @Nested
    @DisplayName("Creación y validación")
    class Creacion {

        @Test
        @DisplayName("Crea una tronera válida")
        void creaTroneraValida() {
            Tronera tronera = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            assertEquals(TroneraNombre.TOP_LEFT, tronera.nombre());
            assertEquals(POSICION, tronera.posicion());
            assertEquals(11.0, tronera.radio());
        }

        @Test
        @DisplayName("Rechaza nombre nulo")
        void rechazaNombreNulo() {
            assertThrows(NullPointerException.class,
                () -> new Tronera(null, POSICION, 11.0));
        }

        @Test
        @DisplayName("Rechaza posición nula")
        void rechazaPosicionNula() {
            assertThrows(NullPointerException.class,
                () -> new Tronera(TroneraNombre.TOP_LEFT, null, 11.0));
        }

        @Test
        @DisplayName("Rechaza radio cero")
        void rechazaRadioCero() {
            assertThrows(IllegalArgumentException.class,
                () -> new Tronera(TroneraNombre.TOP_LEFT, POSICION, 0));
        }

        @Test
        @DisplayName("Rechaza radio negativo")
        void rechazaRadioNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> new Tronera(TroneraNombre.TOP_LEFT, POSICION, -5));
        }
    }

    @Nested
    @DisplayName("Radio por defecto")
    class RadioPorDefecto {

        @ParameterizedTest
        @EnumSource(value = TroneraNombre.class,
                    names = {"TOP_LEFT", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT"})
        @DisplayName("Esquinas usan radio de 11 cm")
        void esquinasRadioOnce(TroneraNombre nombre) {
            Tronera t = Tronera.conRadioPorDefecto(nombre, POSICION);
            assertEquals(Tronera.RADIO_ESQUINA, t.radio());
        }

        @ParameterizedTest
        @EnumSource(value = TroneraNombre.class,
                    names = {"MIDDLE_LEFT", "MIDDLE_RIGHT"})
        @DisplayName("Laterales usan radio de 13 cm")
        void lateralesRadioTrece(TroneraNombre nombre) {
            Tronera t = Tronera.conRadioPorDefecto(nombre, POSICION);
            assertEquals(Tronera.RADIO_LATERAL, t.radio());
        }
    }

    @Nested
    @DisplayName("Captura de bolas")
    class Captura {

        @Test
        @DisplayName("Bola en el centro de la tronera es capturada")
        void bolaEnCentro() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            assertTrue(t.captura(POSICION));
        }

        @Test
        @DisplayName("Bola dentro del radio es capturada")
        void bolaDentroDelRadio() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            Coordenada cerca = new Coordenada(15.0, 10.0); // 5 cm de distancia
            assertTrue(t.captura(cerca));
        }

        @Test
        @DisplayName("Bola justo en el borde es capturada")
        void bolaEnElBorde() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            Coordenada borde = new Coordenada(21.0, 10.0); // exactamente 11 cm
            assertTrue(t.captura(borde));
        }

        @Test
        @DisplayName("Bola fuera del radio no es capturada")
        void bolaFuera() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            Coordenada lejos = new Coordenada(30.0, 10.0); // 20 cm de distancia
            assertFalse(t.captura(lejos));
        }

        @Test
        @DisplayName("Captura de una Bola completa")
        void capturaDeBola() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            Bola bola = Bola.crear(3, POSICION);
            assertTrue(t.captura(bola));
        }

        @Test
        @DisplayName("Captura rechaza posición nula")
        void capturaRechazaNulo() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            assertThrows(NullPointerException.class,
                () -> t.captura((Coordenada) null));
        }

        @Test
        @DisplayName("Captura rechaza bola nula")
        void capturaRechazaBolaNula() {
            Tronera t = new Tronera(TroneraNombre.TOP_LEFT, POSICION, 11.0);
            assertThrows(NullPointerException.class,
                () -> t.captura((Bola) null));
        }
    }
}