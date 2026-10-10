package com.poolhawk.domain.event;

import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Coordenada;
import com.poolhawk.domain.table.TroneraNombre;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link TiroEvent}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("TiroEvent - evento de un tiro")
class TiroEventTest {

    private static final Jugador JUGADOR = new Jugador("j1", "Ana");
    private static final Coordenada POS = new Coordenada(50, 50);
    private static final Instant AHORA = Instant.now();
    private static final Duration DURACION = Duration.ofSeconds(5);

    private static Bola bola(int numero) {
        return Bola.crear(numero, POS);
    }

    @Nested
    @DisplayName("Constructor y validaciones")
    class Constructor {

        @Test
        @DisplayName("Crea un evento válido con todo")
        void creaValido() {
            Bola tocada = bola(3);
            Bola embolsada = bola(3);

            TiroEvent evento = new TiroEvent(
                "e1", "p1", JUGADOR,
                tocada,
                List.of(embolsada),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA,
                DURACION
            );

            assertEquals("e1", evento.id());
            assertEquals("p1", evento.partidaId());
            assertEquals(JUGADOR, evento.jugador());
            assertEquals(tocada, evento.primeraBolaTocada());
            assertEquals(1, evento.cantidadBolasEmbolsadas());
            assertEquals(AHORA, evento.timestamp());
            assertEquals(DURACION, evento.duracion());
        }

        @Test
        @DisplayName("Crea evento sin primera bola tocada")
        void sinPrimeraBola() {
            TiroEvent evento = new TiroEvent(
                "e1", "p1", JUGADOR,
                null,
                List.of(),
                List.of(),
                AHORA,
                DURACION
            );

            assertNull(evento.primeraBolaTocada());
            assertFalse(evento.tocoAlgunaBola());
        }

        @Test
        @DisplayName("Rechaza id nulo")
        void rechazaIdNulo() {
            assertThrows(NullPointerException.class,
                () -> new TiroEvent(null, "p1", JUGADOR, null,
                    List.of(), List.of(), AHORA, DURACION));
        }

        @Test
        @DisplayName("Rechaza id en blanco")
        void rechazaIdBlanco() {
            assertThrows(IllegalArgumentException.class,
                () -> new TiroEvent("", "p1", JUGADOR, null,
                    List.of(), List.of(), AHORA, DURACION));
        }

        @Test
        @DisplayName("Rechaza partidaId nulo")
        void rechazaPartidaNula() {
            assertThrows(NullPointerException.class,
                () -> new TiroEvent("e1", null, JUGADOR, null,
                    List.of(), List.of(), AHORA, DURACION));
        }

        @Test
        @DisplayName("Rechaza jugador nulo")
        void rechazaJugadorNulo() {
            assertThrows(NullPointerException.class,
                () -> new TiroEvent("e1", "p1", null, null,
                    List.of(), List.of(), AHORA, DURACION));
        }

        @Test
        @DisplayName("Rechaza listas nulas")
        void rechazaListasNulas() {
            assertThrows(NullPointerException.class,
                () -> new TiroEvent("e1", "p1", JUGADOR, null,
                    null, List.of(), AHORA, DURACION));
            assertThrows(NullPointerException.class,
                () -> new TiroEvent("e1", "p1", JUGADOR, null,
                    List.of(), null, AHORA, DURACION));
        }

        @Test
        @DisplayName("Rechaza timestamp nulo")
        void rechazaTimestampNulo() {
            assertThrows(NullPointerException.class,
                () -> new TiroEvent("e1", "p1", JUGADOR, null,
                    List.of(), List.of(), null, DURACION));
        }

        @Test
        @DisplayName("Rechaza duración nula")
        void rechazaDuracionNula() {
            assertThrows(NullPointerException.class,
                () -> new TiroEvent("e1", "p1", JUGADOR, null,
                    List.of(), List.of(), AHORA, null));
        }

        @Test
        @DisplayName("Rechaza duración negativa")
        void rechazaDuracionNegativa() {
            assertThrows(IllegalArgumentException.class,
                () -> new TiroEvent("e1", "p1", JUGADOR, null,
                    List.of(), List.of(), AHORA, Duration.ofSeconds(-1)));
        }

        @Test
        @DisplayName("Acepta duración cero")
        void aceptaDuracionCero() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(), List.of(), AHORA, Duration.ZERO);
            assertEquals(Duration.ZERO, evento.duracion());
        }

        @Test
        @DisplayName("Rechaza listas de distinta longitud")
        void rechazaListasDesiguales() {
            IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new TiroEvent("e1", "p1", JUGADOR, null,
                    List.of(bola(1), bola(2)),
                    List.of(TroneraNombre.TOP_LEFT),
                    AHORA, DURACION)
            );
            assertTrue(ex.getMessage().contains("debe coincidir"));
        }
    }

    @Nested
    @DisplayName("Inmutabilidad de listas")
    class Inmutabilidad {

        @Test
        @DisplayName("La lista de bolas embolsadas es inmutable")
        void bolasInmutables() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(1)),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);

            assertThrows(UnsupportedOperationException.class,
                () -> evento.bolasEmbolsadas().clear());
        }

        @Test
        @DisplayName("La lista de troneras es inmutable")
        void tronerasInmutables() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(1)),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);

            assertThrows(UnsupportedOperationException.class,
                () -> evento.troneras().clear());
        }

        @Test
        @DisplayName("Modificar la lista original no afecta al evento")
        void copiaDefensiva() {
            java.util.List<Bola> mutable = new java.util.ArrayList<>();
            mutable.add(bola(1));

            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                mutable,
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);

            mutable.clear();

            assertEquals(1, evento.cantidadBolasEmbolsadas());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("tocoAlgunaBola retorna true cuando hay primera bola")
        void tocoAlgunaBola() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, bola(3),
                List.of(), List.of(), AHORA, DURACION);
            assertTrue(evento.tocoAlgunaBola());
        }

        @Test
        @DisplayName("embolsoAlgunaBola retorna true con bolas")
        void embolsoAlguna() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(3)),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);
            assertTrue(evento.embolsoAlgunaBola());
        }

        @Test
        @DisplayName("embolsoAlgunaBola retorna false sin bolas")
        void noEmbolso() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(), List.of(), AHORA, DURACION);
            assertFalse(evento.embolsoAlgunaBola());
            assertEquals(0, evento.cantidadBolasEmbolsadas());
        }

        @Test
        @DisplayName("embolsoLaBlanca detecta la blanca")
        void embolsoBlanca() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(0)),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);
            assertTrue(evento.embolsoLaBlanca());
        }

        @Test
        @DisplayName("embolsoLaOcho detecta la 8")
        void embolsoOcho() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(8)),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);
            assertTrue(evento.embolsoLaOcho());
            assertFalse(evento.embolsoLaBlanca());
        }

        @Test
        @DisplayName("troneraDeBola retorna la tronera correcta")
        void troneraDeBola() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(1), bola(3), bola(5)),
                List.of(TroneraNombre.TOP_LEFT,
                        TroneraNombre.MIDDLE_RIGHT,
                        TroneraNombre.BOTTOM_LEFT),
                AHORA, DURACION);

            assertEquals(TroneraNombre.TOP_LEFT, evento.troneraDeBola(1));
            assertEquals(TroneraNombre.MIDDLE_RIGHT, evento.troneraDeBola(3));
            assertEquals(TroneraNombre.BOTTOM_LEFT, evento.troneraDeBola(5));
        }

        @Test
        @DisplayName("troneraDeBola retorna null si la bola no fue embolsada")
        void troneraDeBolaNoEmbolsada() {
            TiroEvent evento = new TiroEvent("e1", "p1", JUGADOR, null,
                List.of(bola(1)),
                List.of(TroneraNombre.TOP_LEFT),
                AHORA, DURACION);

            assertNull(evento.troneraDeBola(99));
        }
    }
}