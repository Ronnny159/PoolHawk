package com.poolhawk.domain.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Coordenada}.
 *
 * <p>Cubre creación válida, validación de entradas, cálculo de distancias,
 * proximidad y desplazamiento. Todos los casos borde identificados en el
 * diseño están representados aquí.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Coordenada - posición bidimensional en la mesa")
class CoordenadaTest {

    @Nested
    @DisplayName("Creación y validación")
    class CreacionYValidacion {

        @Test
        @DisplayName("Crea una coordenada válida con valores positivos")
        void creaCoordenadaValida() {
            Coordenada c = new Coordenada(10.0, 20.0);
            assertEquals(10.0, c.x());
            assertEquals(20.0, c.y());
        }

        @Test
        @DisplayName("Crea una coordenada válida con valores negativos")
        void creaCoordenadaConNegativos() {
            Coordenada c = new Coordenada(-5.0, -3.5);
            assertEquals(-5.0, c.x());
            assertEquals(-3.5, c.y());
        }

        @Test
        @DisplayName("Crea una coordenada en el origen")
        void creaCoordenadaEnOrigen() {
            Coordenada c = new Coordenada(0, 0);
            assertEquals(0.0, c.x());
            assertEquals(0.0, c.y());
        }

        @Test
        @DisplayName("Rechaza coordenada con NaN en X")
        void rechazaNaNEnX() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Coordenada(Double.NaN, 5.0)
            );
            assertTrue(ex.getMessage().contains("finitos"));
        }

        @Test
        @DisplayName("Rechaza coordenada con NaN en Y")
        void rechazaNaNEnY() {
            assertThrows(IllegalArgumentException.class,
                () -> new Coordenada(5.0, Double.NaN));
        }

        @Test
        @DisplayName("Rechaza coordenada con infinito positivo")
        void rechazaInfinitoPositivo() {
            assertThrows(IllegalArgumentException.class,
                () -> new Coordenada(5.0, Double.POSITIVE_INFINITY));
        }

        @Test
        @DisplayName("Rechaza coordenada con infinito negativo")
        void rechazaInfinitoNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> new Coordenada(Double.NEGATIVE_INFINITY, 5.0));
        }
    }

    @Nested
    @DisplayName("Cálculo de distancias")
    class CalculoDeDistancias {

        @Test
        @DisplayName("Calcula distancia entre dos puntos con triángulo 3-4-5")
        void calculaDistanciaPitagorica() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(3, 4);
            assertEquals(5.0, a.distanciaA(b), 0.0001);
        }

        @Test
        @DisplayName("Distancia a sí misma es cero")
        void distanciaASiMismaEsCero() {
            Coordenada c = new Coordenada(7, 7);
            assertEquals(0.0, c.distanciaA(c), 0.0001);
        }

        @Test
        @DisplayName("Distancia es simétrica")
        void distanciaEsSimetrica() {
            Coordenada a = new Coordenada(1, 2);
            Coordenada b = new Coordenada(5, 8);
            assertEquals(a.distanciaA(b), b.distanciaA(a), 0.0001);
        }

        @Test
        @DisplayName("Distancia horizontal pura")
        void distanciaHorizontal() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(10, 0);
            assertEquals(10.0, a.distanciaA(b), 0.0001);
        }

        @Test
        @DisplayName("Distancia vertical pura")
        void distanciaVertical() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(0, 10);
            assertEquals(10.0, a.distanciaA(b), 0.0001);
        }

        @Test
        @DisplayName("Lanza NullPointerException si la coordenada destino es nula")
        void distanciaConNull() {
            Coordenada c = new Coordenada(1, 1);
            NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> c.distanciaA(null)
            );
            assertTrue(ex.getMessage().contains("nula"));
        }
    }

    @Nested
    @DisplayName("Proximidad dentro de un radio")
    class Proximidad {

        @Test
        @DisplayName("Retorna true cuando está dentro del radio")
        void dentroDelRadio() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(3, 4);
            assertTrue(a.estaCercaDe(b, 5.5));
        }

        @Test
        @DisplayName("Retorna true cuando está exactamente en el borde del radio")
        void enElBordeDelRadio() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(3, 4);
            assertTrue(a.estaCercaDe(b, 5.0));
        }

        @Test
        @DisplayName("Retorna false cuando está fuera del radio")
        void fueraDelRadio() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(3, 4);
            assertFalse(a.estaCercaDe(b, 4.5));
        }

        @Test
        @DisplayName("Detecta proximidad entre dos bolas estándar")
        void detectaColisionDeBolas() {
            // Una bola estándar de billar tiene radio ~2.85 cm
            Coordenada bolaA = new Coordenada(0, 0);
            Coordenada bolaB = new Coordenada(5.0, 0); // 5 cm de distancia
            // Las bolas colisionan si están a menos de 5.7 cm (suma de radios)
            assertTrue(bolaA.estaCercaDe(bolaB, 5.7));
        }

        @Test
        @DisplayName("Rechaza radio negativo")
        void rechazaRadioNegativo() {
            Coordenada a = new Coordenada(0, 0);
            Coordenada b = new Coordenada(3, 4);
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> a.estaCercaDe(b, -1.0)
            );
            assertTrue(ex.getMessage().contains("negativo"));
        }

        @Test
        @DisplayName("Rechaza coordenada centro nula")
        void rechazaCentroNulo() {
            Coordenada a = new Coordenada(0, 0);
            assertThrows(NullPointerException.class,
                () -> a.estaCercaDe(null, 5.0));
        }
    }

    @Nested
    @DisplayName("Desplazamiento")
    class Desplazamiento {

        @Test
        @DisplayName("Desplaza correctamente en X y Y")
        void desplazaCorrectamente() {
            Coordenada original = new Coordenada(1, 2);
            Coordenada desplazada = original.desplazar(3, 4);
            assertEquals(4.0, desplazada.x());
            assertEquals(6.0, desplazada.y());
        }

        @Test
        @DisplayName("El desplazamiento no modifica la coordenada original")
        void desplazamientoNoModificaOriginal() {
            Coordenada original = new Coordenada(1, 2);
            original.desplazar(10, 10);
            assertEquals(1.0, original.x());
            assertEquals(2.0, original.y());
        }

        @Test
        @DisplayName("Desplazamiento con valores negativos")
        void desplazamientoNegativo() {
            Coordenada original = new Coordenada(5, 5);
            Coordenada desplazada = original.desplazar(-2, -3);
            assertEquals(3.0, desplazada.x());
            assertEquals(2.0, desplazada.y());
        }
    }

    @Nested
    @DisplayName("Igualdad por valor")
    class Igualdad {

        @Test
        @DisplayName("Dos coordenadas con mismos valores son iguales")
        void igualdadPorValor() {
            Coordenada a = new Coordenada(5, 10);
            Coordenada b = new Coordenada(5, 10);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Dos coordenadas con valores distintos no son iguales")
        void desigualdadPorValor() {
            Coordenada a = new Coordenada(5, 10);
            Coordenada b = new Coordenada(10, 5);
            assertNotEquals(a, b);
        }
    }
}