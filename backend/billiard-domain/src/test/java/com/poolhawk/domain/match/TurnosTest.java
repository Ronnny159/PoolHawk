package com.poolhawk.domain.match;

import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Turnos}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Turnos - rotación de jugadores")
class TurnosTest {

    private static final Jugador J1 = new Jugador("j1", "Ana");
    private static final Jugador J2 = new Jugador("j2", "Beto");
    private static final Jugador J3 = new Jugador("j3", "Carla");
    private static final Jugador J4 = new Jugador("j4", "Diego");

    @Nested
    @DisplayName("Construcción con lista")
    class ConstruccionLista {

        @Test
        @DisplayName("Crea turnos con 2 jugadores")
        void dosJugadores() {
            Turnos turnos = new Turnos(List.of(J1, J2));
            assertEquals(2, turnos.cantidadJugadores());
            assertEquals(J1, turnos.jugadorActual());
            assertEquals(0, turnos.indiceActual());
        }

        @Test
        @DisplayName("Crea turnos con 4 jugadores (Por Puntos)")
        void cuatroJugadores() {
            Turnos turnos = new Turnos(List.of(J1, J2, J3, J4));
            assertEquals(4, turnos.cantidadJugadores());
            assertEquals(J1, turnos.jugadorActual());
        }

        @Test
        @DisplayName("Crea turnos con índice inicial")
        void indiceInicial() {
            Turnos turnos = new Turnos(List.of(J1, J2, J3, J4), 2);
            assertEquals(J3, turnos.jugadorActual());
            assertEquals(2, turnos.indiceActual());
        }

        @Test
        @DisplayName("Rechaza lista nula")
        void rechazaListaNula() {
            assertThrows(NullPointerException.class,
                () -> new Turnos(null));
        }

        @Test
        @DisplayName("Rechaza lista vacía")
        void rechazaListaVacia() {
            assertThrows(IllegalArgumentException.class,
                () -> new Turnos(List.of()));
        }

        @Test
        @DisplayName("Rechaza lista con un solo jugador")
        void rechazaUnJugador() {
            assertThrows(IllegalArgumentException.class,
                () -> new Turnos(List.of(J1)));
        }

        @Test
        @DisplayName("Rechaza índice inicial fuera de rango")
        void rechazaIndiceInvalido() {
            assertThrows(IllegalArgumentException.class,
                () -> new Turnos(List.of(J1, J2), 5));
            assertThrows(IllegalArgumentException.class,
                () -> new Turnos(List.of(J1, J2), -1));
        }

        @Test
        @DisplayName("Rechaza jugadores duplicados")
        void rechazaDuplicados() {
            assertThrows(IllegalArgumentException.class,
                () -> new Turnos(List.of(J1, J2, J1)));
        }

        @Test
        @DisplayName("La lista de jugadores es inmutable")
        void listaInmutable() {
            Turnos turnos = new Turnos(List.of(J1, J2));
            assertThrows(UnsupportedOperationException.class,
                () -> turnos.jugadores().clear());
        }
    }

    @Nested
    @DisplayName("Fábrica desde equipos")
    class FabricaDesdeEquipos {

        @Test
        @DisplayName("Crea turnos intercalados para parejas")
        void parejasIntercaladas() {
            Equipo equipoA = Equipo.pareja("eA", "A", J1, J3);
            Equipo equipoB = Equipo.pareja("eB", "B", J2, J4);

            Turnos turnos = Turnos.desdeEquipos(equipoA, equipoB);

            assertEquals(4, turnos.cantidadJugadores());
            assertEquals(List.of(J1, J2, J3, J4), turnos.jugadores());
        }

        @Test
        @DisplayName("Crea turnos para equipos individuales")
        void individuales() {
            Equipo equipoA = Equipo.individual("eA", "A", J1);
            Equipo equipoB = Equipo.individual("eB", "B", J2);

            Turnos turnos = Turnos.desdeEquipos(equipoA, equipoB);

            assertEquals(2, turnos.cantidadJugadores());
            assertEquals(List.of(J1, J2), turnos.jugadores());
        }

        @Test
        @DisplayName("Rechaza equipos con distinta cantidad de jugadores")
        void equiposDesiguales() {
            Equipo equipoA = Equipo.individual("eA", "A", J1);
            Equipo equipoB = Equipo.pareja("eB", "B", J2, J4);

            assertThrows(IllegalArgumentException.class,
                () -> Turnos.desdeEquipos(equipoA, equipoB));
        }

        @Test
        @DisplayName("Rechaza equipos vacíos")
        void equiposVacios() {
            Equipo vacioA = new Equipo("eA", "A");
            Equipo vacioB = new Equipo("eB", "B");

            assertThrows(IllegalArgumentException.class,
                () -> Turnos.desdeEquipos(vacioA, vacioB));
        }

        @Test
        @DisplayName("Rechaza equipos nulos")
        void equiposNulos() {
            Equipo equipo = new Equipo("e", "Equipo");
            assertThrows(NullPointerException.class,
                () -> Turnos.desdeEquipos(null, equipo));
            assertThrows(NullPointerException.class,
                () -> Turnos.desdeEquipos(equipo, null));
        }
    }

    @Nested
    @DisplayName("Avance circular")
    class AvanceCircular {

        private Turnos turnos;

        @BeforeEach
        void setUp() {
            turnos = new Turnos(List.of(J1, J2, J3, J4));
        }

        @Test
        @DisplayName("Avanzar pasa al siguiente jugador")
        void avanzaUno() {
            turnos.avanzar();
            assertEquals(J2, turnos.jugadorActual());
            assertEquals(1, turnos.indiceActual());
        }

        @Test
        @DisplayName("Avanzar 4 veces vuelve al inicio")
        void vueltaCompleta() {
            turnos.avanzar();
            turnos.avanzar();
            turnos.avanzar();
            turnos.avanzar();
            assertEquals(J1, turnos.jugadorActual());
            assertEquals(0, turnos.indiceActual());
        }

        @Test
        @DisplayName("Avanzar con n grande hace módulo")
        void avanceGrande() {
            turnos.avanzar(7); // 7 % 4 = 3
            assertEquals(J4, turnos.jugadorActual());
        }

        @Test
        @DisplayName("Avanzar con n negativo retrocede")
        void avanceNegativo() {
            turnos.avanzar(-1); // 0 - 1 = -1 → 3
            assertEquals(J4, turnos.jugadorActual());
        }

        @Test
        @DisplayName("Avanzar con n=0 no cambia")
        void avanceCero() {
            turnos.avanzar(0);
            assertEquals(J1, turnos.jugadorActual());
        }
    }

    @Nested
    @DisplayName("Reinicio")
    class Reinicio {

        private Turnos turnos;

        @BeforeEach
        void setUp() {
            turnos = new Turnos(List.of(J1, J2, J3, J4));
        }

        @Test
        @DisplayName("Reiniciar vuelve al primer jugador")
        void reinicia() {
            turnos.avanzar(2);
            turnos.reiniciar();
            assertEquals(J1, turnos.jugadorActual());
            assertEquals(0, turnos.indiceActual());
        }

        @Test
        @DisplayName("Reiniciar desde un jugador específico")
        void reiniciaDesdeJugador() {
            turnos.reiniciarDesde(J3);
            assertEquals(J3, turnos.jugadorActual());
            assertEquals(2, turnos.indiceActual());
        }

        @Test
        @DisplayName("Reiniciar desde jugador que no está en el orden lanza excepción")
        void reiniciaDesdeAjeno() {
            Jugador ajeno = new Jugador("j99", "Ajeno");
            assertThrows(IllegalArgumentException.class,
                () -> turnos.reiniciarDesde(ajeno));
        }

        @Test
        @DisplayName("Reiniciar desde nulo lanza excepción")
        void reiniciaDesdeNulo() {
            assertThrows(NullPointerException.class,
                () -> turnos.reiniciarDesde(null));
        }
    }

    @Nested
    @DisplayName("Consulta de turnos")
    class Consulta {

        private Turnos turnos;

        @BeforeEach
        void setUp() {
            turnos = new Turnos(List.of(J1, J2, J3, J4));
        }

        @Test
        @DisplayName("esTurnoDe retorna true para el jugador actual")
        void esTurnoDelActual() {
            assertTrue(turnos.esTurnoDe(J1));
            assertFalse(turnos.esTurnoDe(J2));
        }

        @Test
        @DisplayName("posicionDe retorna el índice correcto")
        void posicionCorrecta() {
            assertEquals(0, turnos.posicionDe(J1));
            assertEquals(1, turnos.posicionDe(J2));
            assertEquals(2, turnos.posicionDe(J3));
            assertEquals(3, turnos.posicionDe(J4));
        }

        @Test
        @DisplayName("posicionDe retorna -1 para jugador ajeno")
        void posicionAjeno() {
            Jugador ajeno = new Jugador("j99", "Ajeno");
            assertEquals(-1, turnos.posicionDe(ajeno));
        }

        @Test
        @DisplayName("turnoSiguiente no modifica el estado")
        void siguienteNoModifica() {
            Jugador siguiente = turnos.turnoSiguiente();
            assertEquals(J2, siguiente);
            assertEquals(J1, turnos.jugadorActual()); // no cambió
        }

        @Test
        @DisplayName("turnoSiguiente desde el último vuelve al primero")
        void siguienteDesdeUltimo() {
            turnos.reiniciarDesde(J4);
            assertEquals(J1, turnos.turnoSiguiente());
        }

        @Test
        @DisplayName("turnoAnterior no modifica el estado")
        void anteriorNoModifica() {
            Jugador anterior = turnos.turnoAnterior();
            assertEquals(J4, anterior);
            assertEquals(J1, turnos.jugadorActual()); // no cambió
        }

        @Test
        @DisplayName("turnoAnterior desde el primero va al último")
        void anteriorDesdePrimero() {
            assertEquals(J4, turnos.turnoAnterior());
        }
    }
}