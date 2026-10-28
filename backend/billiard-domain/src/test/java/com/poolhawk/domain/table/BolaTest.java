package com.poolhawk.domain.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Bola}.
 *
 * <p>Cubre creación, validación de invariantes, derivación de tipo,
 * movimiento, embolsado y clasificación por tipo.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Bola - entidad de bola de billar")
class BolaTest {

    private static final Coordenada POSICION = new Coordenada(50.0, 100.0);

    @Nested
    @DisplayName("Creación y validación")
    class CreacionYValidacion {

        @Test
        @DisplayName("Crea la bola blanca correctamente")
        void creaBolaBlanca() {
            Bola blanca = Bola.crear(0, POSICION);
            assertEquals(0, blanca.numero());
            assertEquals(TipoBola.BLANCA, blanca.tipo());
            assertTrue(blanca.enMesa());
            assertTrue(blanca.esBlanca());
        }

        @Test
        @DisplayName("Crea una bola lisa correctamente")
        void creaBolaLisa() {
            Bola bola3 = Bola.crear(3, POSICION);
            assertEquals(3, bola3.numero());
            assertEquals(TipoBola.LISA, bola3.tipo());
            assertTrue(bola3.esLisa());
        }

        @Test
        @DisplayName("Crea la bola 8 correctamente")
        void creaBolaOcho() {
            Bola ocho = Bola.crear(8, POSICION);
            assertEquals(8, ocho.numero());
            assertEquals(TipoBola.OCHO, ocho.tipo());
            assertTrue(ocho.esOcho());
        }

        @Test
        @DisplayName("Crea una bola rayada correctamente")
        void creaBolaRayada() {
            Bola bola11 = Bola.crear(11, POSICION);
            assertEquals(11, bola11.numero());
            assertEquals(TipoBola.RAYADA, bola11.tipo());
            assertTrue(bola11.esRayada());
        }

        @Test
        @DisplayName("Rechaza número negativo")
        void rechazaNumeroNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> Bola.crear(-1, POSICION));
        }

        @Test
        @DisplayName("Rechaza número mayor a 15")
        void rechazaNumeroMayor() {
            assertThrows(IllegalArgumentException.class,
                () -> Bola.crear(16, POSICION));
        }

        @Test
        @DisplayName("Rechaza posición nula")
        void rechazaPosicionNula() {
            assertThrows(NullPointerException.class,
                () -> Bola.crear(3, null));
        }

        @Test
        @DisplayName("Rechaza tipo inconsistente con el número")
        void rechazaTipoInconsistente() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Bola(3, TipoBola.RAYADA, POSICION, true)
            );
            assertTrue(ex.getMessage().contains("no corresponde"));
        }
    }

    @Nested
    @DisplayName("Movimiento")
    class Movimiento {

        @Test
        @DisplayName("Mueve la bola a una nueva posición")
        void mueveBola() {
            Bola original = Bola.crear(3, POSICION);
            Coordenada nuevaPos = new Coordenada(75.0, 150.0);
            Bola movida = original.moverA(nuevaPos);

            assertEquals(nuevaPos, movida.posicion());
            assertEquals(original.numero(), movida.numero());
            assertEquals(original.tipo(), movida.tipo());
        }

        @Test
        @DisplayName("El movimiento no modifica la bola original")
        void movimientoNoModificaOriginal() {
            Bola original = Bola.crear(3, POSICION);
            original.moverA(new Coordenada(75.0, 150.0));

            assertEquals(POSICION, original.posicion());
        }

        @Test
        @DisplayName("Mover con posición nula lanza excepción")
        void moverConNulo() {
            Bola original = Bola.crear(3, POSICION);
            assertThrows(NullPointerException.class,
                () -> original.moverA(null));
        }
    }

    @Nested
    @DisplayName("Embolsado")
    class Embolsado {

        @Test
        @DisplayName("Embolsar marca la bola fuera de la mesa")
        void embolsarMarcaFueraDeMesa() {
            Bola original = Bola.crear(3, POSICION);
            Bola embolsada = original.embolsar();

            assertFalse(embolsada.enMesa());
            assertEquals(original.numero(), embolsada.numero());
        }

        @Test
        @DisplayName("Embolsar no modifica la bola original")
        void embolsarNoModificaOriginal() {
            Bola original = Bola.crear(3, POSICION);
            original.embolsar();

            assertTrue(original.enMesa());
        }
    }

    @Nested
    @DisplayName("Clasificación por tipo")
    class Clasificacion {

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
        @DisplayName("Las bolas del 1 al 7 son lisas y de grupo")
        void lisasSonDeGrupo(int numero) {
            Bola bola = Bola.crear(numero, POSICION);
            assertTrue(bola.esLisa());
            assertTrue(bola.esDeGrupo());
        }

        @ParameterizedTest
        @ValueSource(ints = {9, 10, 11, 12, 13, 14, 15})
        @DisplayName("Las bolas del 9 al 15 son rayadas y de grupo")
        void rayadasSonDeGrupo(int numero) {
            Bola bola = Bola.crear(numero, POSICION);
            assertTrue(bola.esRayada());
            assertTrue(bola.esDeGrupo());
        }

        @Test
        @DisplayName("La bola 8 no es de grupo")
        void ochoNoEsDeGrupo() {
            Bola ocho = Bola.crear(8, POSICION);
            assertFalse(ocho.esDeGrupo());
        }

        @Test
        @DisplayName("La blanca no es de grupo")
        void blancaNoEsDeGrupo() {
            Bola blanca = Bola.crear(0, POSICION);
            assertFalse(blanca.esDeGrupo());
        }
    }

    @Nested
    @DisplayName("Igualdad por valor")
    class Igualdad {

        @Test
        @DisplayName("Dos bolas con mismos valores son iguales")
        void bolasIguales() {
            Bola a = Bola.crear(3, POSICION);
            Bola b = Bola.crear(3, POSICION);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("Dos bolas con distinto número no son iguales")
        void bolasDistintoNumero() {
            Bola a = Bola.crear(3, POSICION);
            Bola b = Bola.crear(4, POSICION);
            assertNotEquals(a, b);
        }
    }
}