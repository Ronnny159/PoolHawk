package com.poolhawk.domain.table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Mesa}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Mesa - contenedor de bolas y troneras")
class MesaTest {

    private Mesa mesa;

    @BeforeEach
    void setUp() {
        mesa = new Mesa();
    }

    @Nested
    @DisplayName("Construcción")
    class Construccion {

        @Test
        @DisplayName("Crea mesa con dimensiones por defecto")
        void dimensionesPorDefecto() {
            assertEquals(Mesa.ANCHO_DEFECTO, mesa.getAncho());
            assertEquals(Mesa.ALTO_DEFECTO, mesa.getAlto());
        }

        @Test
        @DisplayName("Crea mesa con dimensiones personalizadas")
        void dimensionesPersonalizadas() {
            Mesa pequeña = new Mesa(198, 99);
            assertEquals(198, pequeña.getAncho());
            assertEquals(99, pequeña.getAlto());
        }

        @Test
        @DisplayName("Rechaza ancho no positivo")
        void rechazaAnchoCero() {
            assertThrows(IllegalArgumentException.class,
                () -> new Mesa(0, 100));
        }

        @Test
        @DisplayName("Rechaza alto negativo")
        void rechazaAltoNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> new Mesa(100, -1));
        }

        @Test
        @DisplayName("Siempre construye 6 troneras")
        void seisTroneras() {
            assertEquals(6, mesa.getTroneras().size());
        }

        @Test
        @DisplayName("La lista de troneras es inmutable")
        void tronerasInmutables() {
            assertThrows(UnsupportedOperationException.class,
                () -> mesa.getTroneras().clear());
        }
    }

    @Nested
    @DisplayName("Gestión de bolas")
    class GestionDeBolas {

        @Test
        @DisplayName("Agrega una bola correctamente")
        void agregaBola() {
            mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
            assertEquals(1, mesa.contarBolasEnMesa());
        }

        @Test
        @DisplayName("Rechaza bola con número duplicado")
        void rechazaDuplicado() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            assertThrows(IllegalArgumentException.class,
                () -> mesa.agregarBola(Bola.crear(3, new Coordenada(60, 60))));
        }

        @Test
        @DisplayName("Rechaza bola fuera de límites")
        void rechazaFueraDeLimites() {
            assertThrows(IllegalArgumentException.class,
                () -> mesa.agregarBola(Bola.crear(1, new Coordenada(300, 200))));
        }

        @Test
        @DisplayName("Rechaza bola nula")
        void rechazaBolaNula() {
            assertThrows(NullPointerException.class,
                () -> mesa.agregarBola(null));
        }

        @Test
        @DisplayName("Quita una bola correctamente")
        void quitarBola() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            mesa.quitarBola(3);
            assertEquals(0, mesa.contarBolasEnMesa());
        }

        @Test
        @DisplayName("Quitar bola inexistente lanza excepción")
        void quitarInexistente() {
            assertThrows(IllegalArgumentException.class,
                () -> mesa.quitarBola(99));
        }

        @Test
        @DisplayName("Limpiar elimina todas las bolas")
        void limpiarTodas() {
            mesa.agregarBola(Bola.crear(1, new Coordenada(50, 50)));
            mesa.agregarBola(Bola.crear(2, new Coordenada(60, 60)));
            mesa.limpiar();
            assertEquals(0, mesa.contarBolasEnMesa());
        }
    }

    @Nested
    @DisplayName("Movimiento de bolas")
    class Movimiento {

        @Test
        @DisplayName("Mueve una bola a nueva posición")
        void mueveBola() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            mesa.moverBola(3, new Coordenada(100, 100));

            Bola movida = mesa.buscarBola(3).orElseThrow();
            assertEquals(new Coordenada(100, 100), movida.posicion());
        }

        @Test
        @DisplayName("Mover bola inexistente lanza excepción")
        void moverInexistente() {
            assertThrows(IllegalArgumentException.class,
                () -> mesa.moverBola(99, new Coordenada(50, 50)));
        }

        @Test
        @DisplayName("Mover a posición fuera de límites lanza excepción")
        void moverFueraDeLimites() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            assertThrows(IllegalArgumentException.class,
                () -> mesa.moverBola(3, new Coordenada(500, 500)));
        }

        @Test
        @DisplayName("Mover con posición nula lanza excepción")
        void moverConNulo() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            assertThrows(NullPointerException.class,
                () -> mesa.moverBola(3, null));
        }
    }

    @Nested
    @DisplayName("Embolsado")
    class Embolsado {

        @Test
        @DisplayName("Embolsar marca la bola como fuera de mesa")
        void embolsarBola() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            mesa.embolsarBola(3);

            Bola embolsada = mesa.buscarBola(3).orElseThrow();
            assertFalse(embolsada.enMesa());
            assertEquals(0, mesa.contarBolasEnMesa());
            assertEquals(1, mesa.getBolasEmbolsadas().size());
        }

        @Test
        @DisplayName("Embolsar bola inexistente lanza excepción")
        void embolsarInexistente() {
            assertThrows(IllegalArgumentException.class,
                () -> mesa.embolsarBola(99));
        }
    }

    @Nested
    @DisplayName("Consulta de bolas")
    class Consulta {

        @Test
        @DisplayName("Buscar bola existente retorna Optional con la bola")
        void buscarExistente() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            Optional<Bola> resultado = mesa.buscarBola(3);
            assertTrue(resultado.isPresent());
            assertEquals(3, resultado.get().numero());
        }

        @Test
        @DisplayName("Buscar bola inexistente retorna Optional vacío")
        void buscarInexistente() {
            Optional<Bola> resultado = mesa.buscarBola(99);
            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("getBolaBlanca retorna la blanca")
        void obtenerBlanca() {
            mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
            Bola blanca = mesa.getBolaBlanca();
            assertEquals(0, blanca.numero());
        }

        @Test
        @DisplayName("getBolaBlanca lanza excepción si no está")
        void blancaNoPresente() {
            assertThrows(IllegalStateException.class,
                () -> mesa.getBolaBlanca());
        }

        @Test
        @DisplayName("getBolas retorna lista inmutable")
        void bolasInmutables() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            assertThrows(UnsupportedOperationException.class,
                () -> mesa.getBolas().clear());
        }
    }

    @Nested
    @DisplayName("Consulta de grupos")
    class Grupos {

        @Test
        @DisplayName("Quedan bolas lisas en la mesa")
        void quedanLisas() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            assertTrue(mesa.quedanBolasDeGrupo(TipoBola.LISA));
            assertFalse(mesa.quedanBolasDeGrupo(TipoBola.RAYADA));
        }

        @Test
        @DisplayName("No quedan bolas de grupo si todas están embolsadas")
        void noQuedanGrupo() {
            mesa.agregarBola(Bola.crear(3, new Coordenada(50, 50)));
            mesa.embolsarBola(3);
            assertFalse(mesa.quedanBolasDeGrupo(TipoBola.LISA));
        }

        @Test
        @DisplayName("Rechaza tipo no de grupo")
        void rechazaTipoNoGrupo() {
            assertThrows(IllegalArgumentException.class,
                () -> mesa.quedanBolasDeGrupo(TipoBola.OCHO));
            assertThrows(IllegalArgumentException.class,
                () -> mesa.quedanBolasDeGrupo(TipoBola.BLANCA));
        }
    }

    @Nested
    @DisplayName("Límites y troneras")
    class LimitesYTroneras {

        @Test
        @DisplayName("Posición dentro de límites")
        void dentroDeLimites() {
            assertTrue(mesa.estaDentroDeLimites(new Coordenada(100, 50)));
            assertTrue(mesa.estaDentroDeLimites(new Coordenada(0, 0)));
            assertTrue(mesa.estaDentroDeLimites(new Coordenada(254, 127)));
        }

        @Test
        @DisplayName("Posición fuera de límites")
        void fueraDeLimites() {
            assertFalse(mesa.estaDentroDeLimites(new Coordenada(-1, 50)));
            assertFalse(mesa.estaDentroDeLimites(new Coordenada(300, 50)));
        }

        @Test
        @DisplayName("Bola en alguna tronera")
        void bolaEnTronera() {
            Coordenada posicionTronera = mesa.getTroneras().get(0).posicion();
            Bola bola = Bola.crear(3, posicionTronera);
            assertTrue(mesa.bolaEnAlgunaTronera(bola));
        }

        @Test
        @DisplayName("Bola fuera de troneras")
        void bolaFueraDeTroneras() {
            Bola bola = Bola.crear(3, new Coordenada(127, 63));
            assertFalse(mesa.bolaEnAlgunaTronera(bola));
        }

        @Test
        @DisplayName("Tronera más cercana a una posición")
        void troneraMasCercana() {
            Coordenada cercaEsquinaInferior = new Coordenada(5, 5);
            Tronera cercana = mesa.getTroneraMasCercana(cercaEsquinaInferior);
            assertEquals(TroneraNombre.BOTTOM_LEFT, cercana.nombre());
        }
    }

    @Nested
    @DisplayName("Bolas en mesa vs embolsadas")
    class BolasEnMesaVsEmbolsadas {

        @Test
        @DisplayName("getBolasEnMesa solo retorna las que están en juego")
        void bolasEnMesa() {
            mesa.agregarBola(Bola.crear(1, new Coordenada(50, 50)));
            mesa.agregarBola(Bola.crear(2, new Coordenada(60, 60)));
            mesa.embolsarBola(1);

            List<Bola> enMesa = mesa.getBolasEnMesa();
            assertEquals(1, enMesa.size());
            assertEquals(2, enMesa.get(0).numero());
        }

        @Test
        @DisplayName("getBolasEmbolsadas retorna solo las fuera de juego")
        void bolasEmbolsadas() {
            mesa.agregarBola(Bola.crear(1, new Coordenada(50, 50)));
            mesa.agregarBola(Bola.crear(2, new Coordenada(60, 60)));
            mesa.embolsarBola(1);

            List<Bola> embolsadas = mesa.getBolasEmbolsadas();
            assertEquals(1, embolsadas.size());
            assertEquals(1, embolsadas.get(0).numero());
        }
    }
}