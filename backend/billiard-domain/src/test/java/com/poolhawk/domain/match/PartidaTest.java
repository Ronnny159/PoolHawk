package com.poolhawk.domain.match;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Coordenada;
import com.poolhawk.domain.table.Mesa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Partida}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Partida - agregado raíz del dominio")
class PartidaTest {

    private static final String ID = "partida-1";

    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");
    private static final Jugador CARLA = new Jugador("j3", "Carla");
    private static final Jugador DIEGO = new Jugador("j4", "Diego");

    private Mesa mesa;
    private Partida partidaIndividual;

    @BeforeEach
    void setUp() {
        mesa = new Mesa();
        mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
        mesa.agregarBola(Bola.crear(1, new Coordenada(60, 60)));
        partidaIndividual = Partida.individual(ID, ANA, BETO, mesa);
    }

    private TiroEvent tiroDe(Jugador jugador) {
        return new TiroEvent(
            "evt-" + System.nanoTime(),
            ID,
            jugador,
            null,
            List.of(),
            List.of(),
            Instant.now(),
            Duration.ofSeconds(5)
        );
    }

    @Nested
    @DisplayName("Fábricas y construcción")
    class Fabricas {

        @Test
        @DisplayName("Fábrica individual crea una partida de Bola 8")
        void fabricaIndividual() {
            assertEquals(ID, partidaIndividual.getId());
            assertEquals(TipoModalidad.BOLA_8, partidaIndividual.getModalidad());
            assertEquals(1, partidaIndividual.getEquipoA().cantidadJugadores());
            assertEquals(1, partidaIndividual.getEquipoB().cantidadJugadores());
            assertTrue(partidaIndividual.getEquipoA().contieneJugador("j1"));
            assertTrue(partidaIndividual.getEquipoB().contieneJugador("j2"));
        }

        @Test
        @DisplayName("Fábrica parejas crea una partida de Por Puntos")
        void fabricaParejas() {
            Equipo eqA = Equipo.pareja("eA", "Tigres", ANA, CARLA);
            Equipo eqB = Equipo.pareja("eB", "Leones", BETO, DIEGO);

            Partida partida = Partida.parejas(ID, eqA, eqB, mesa);

            assertEquals(TipoModalidad.POR_PUNTOS, partida.getModalidad());
            assertEquals(2, partida.getEquipoA().cantidadJugadores());
            assertEquals(2, partida.getEquipoB().cantidadJugadores());
        }

        @Test
        @DisplayName("Fábrica individual rechaza jugadores iguales")
        void rechazaJugadoresIguales() {
            assertThrows(IllegalArgumentException.class,
                () -> Partida.individual(ID, ANA, ANA, mesa));
        }

        @Test
        @DisplayName("Constructor rechaza modalidad Bola 8 con equipos de 2")
        void modalidadIncoherenteBola8() {
            Equipo eqA = Equipo.pareja("eA", "A", ANA, CARLA);
            Equipo eqB = Equipo.pareja("eB", "B", BETO, DIEGO);

            assertThrows(IllegalArgumentException.class,
                () -> new Partida(ID, TipoModalidad.BOLA_8, eqA, eqB,
                    mesa, Instant.now()));
        }

        @Test
        @DisplayName("Constructor rechaza modalidad Por Puntos con equipos de 1")
        void modalidadIncoherentePorPuntos() {
            Equipo eqA = Equipo.individual("eA", "A", ANA);
            Equipo eqB = Equipo.individual("eB", "B", BETO);

            assertThrows(IllegalArgumentException.class,
                () -> new Partida(ID, TipoModalidad.POR_PUNTOS, eqA, eqB,
                    mesa, Instant.now()));
        }

        @Test
        @DisplayName("Constructor rechaza equipos de distinta cantidad")
        void equiposDesiguales() {
            Equipo eqA = Equipo.individual("eA", "A", ANA);
            Equipo eqB = Equipo.pareja("eB", "B", BETO, DIEGO);

            assertThrows(IllegalArgumentException.class,
                () -> new Partida(ID, TipoModalidad.POR_PUNTOS, eqA, eqB,
                    mesa, Instant.now()));
        }

        @Test
        @DisplayName("Constructor rechaza id nulo")
        void rechazaIdNulo() {
            Equipo eqA = Equipo.individual("eA", "A", ANA);
            Equipo eqB = Equipo.individual("eB", "B", BETO);
            assertThrows(NullPointerException.class,
                () -> new Partida(null, TipoModalidad.BOLA_8, eqA, eqB,
                    mesa, Instant.now()));
        }

        @Test
        @DisplayName("Constructor rechaza id en blanco")
        void rechazaIdBlanco() {
            Equipo eqA = Equipo.individual("eA", "A", ANA);
            Equipo eqB = Equipo.individual("eB", "B", BETO);
            assertThrows(IllegalArgumentException.class,
                () -> new Partida("", TipoModalidad.BOLA_8, eqA, eqB,
                    mesa, Instant.now()));
        }

        @Test
        @DisplayName("Constructor rechaza equipos nulos")
        void rechazaEquiposNulos() {
            Equipo eqA = Equipo.individual("eA", "A", ANA);
            assertThrows(NullPointerException.class,
                () -> new Partida(ID, TipoModalidad.BOLA_8, null, eqA,
                    mesa, Instant.now()));
            assertThrows(NullPointerException.class,
                () -> new Partida(ID, TipoModalidad.BOLA_8, eqA, null,
                    mesa, Instant.now()));
        }

        @Test
        @DisplayName("Constructor rechaza mesa nula")
        void rechazaMesaNula() {
            Equipo eqA = Equipo.individual("eA", "A", ANA);
            Equipo eqB = Equipo.individual("eB", "B", BETO);
            assertThrows(NullPointerException.class,
                () -> new Partida(ID, TipoModalidad.BOLA_8, eqA, eqB,
                    null, Instant.now()));
        }
    }

    @Nested
    @DisplayName("Estado inicial")
    class EstadoInicial {

        @Test
        @DisplayName("Comienza en fase CREADA")
        void faseCreada() {
            assertEquals(FasePartida.CREADA, partidaIndividual.getFase());
            assertFalse(partidaIndividual.estaActiva());
            assertFalse(partidaIndividual.estaTerminada());
        }

        @Test
        @DisplayName("No tiene ganador")
        void sinGanador() {
            assertNull(partidaIndividual.getGanador());
        }

        @Test
        @DisplayName("El estado está inicializado")
        void estadoInicializado() {
            assertNotNull(partidaIndividual.getEstado());
            assertEquals(ID, partidaIndividual.getEstado().getPartidaId());
        }

        @Test
        @DisplayName("El jugador actual es el primero del turno")
        void jugadorActual() {
            assertEquals(ANA, partidaIndividual.jugadorActual());
        }

        @Test
        @DisplayName("getEquipos retorna los dos equipos")
        void equipos() {
            List<Equipo> equipos = partidaIndividual.getEquipos();
            assertEquals(2, equipos.size());
            assertTrue(equipos.contains(partidaIndividual.getEquipoA()));
            assertTrue(equipos.contains(partidaIndividual.getEquipoB()));
        }
    }

    @Nested
    @DisplayName("Transiciones de fase")
    class Transiciones {

        @Test
        @DisplayName("Iniciar cambia a EN_CURSO")
        void iniciar() {
            partidaIndividual.iniciar();
            assertEquals(FasePartida.EN_CURSO, partidaIndividual.getFase());
            assertTrue(partidaIndividual.estaActiva());
        }

        @Test
        @DisplayName("Pausar cambia a PAUSADA")
        void pausar() {
            partidaIndividual.iniciar();
            partidaIndividual.pausar();
            assertEquals(FasePartida.PAUSADA, partidaIndividual.getFase());
        }

        @Test
        @DisplayName("Reanudar vuelve a EN_CURSO")
        void reanudar() {
            partidaIndividual.iniciar();
            partidaIndividual.pausar();
            partidaIndividual.reanudar();
            assertEquals(FasePartida.EN_CURSO, partidaIndividual.getFase());
        }

        @Test
        @DisplayName("Finalizar declara ganador")
        void finalizar() {
            partidaIndividual.iniciar();
            partidaIndividual.finalizar(ANA);
            assertEquals(FasePartida.FINALIZADA, partidaIndividual.getFase());
            assertEquals(ANA, partidaIndividual.getGanador());
            assertTrue(partidaIndividual.estaTerminada());
        }

        @Test
        @DisplayName("Abandonar deja sin ganador")
        void abandonar() {
            partidaIndividual.iniciar();
            partidaIndividual.abandonar();
            assertEquals(FasePartida.ABANDONADA, partidaIndividual.getFase());
            assertNull(partidaIndividual.getGanador());
        }
    }

    @Nested
    @DisplayName("Aplicar tiro")
    class AplicarTiro {

        @BeforeEach
        void iniciar() {
            partidaIndividual.iniciar();
        }

        @Test
        @DisplayName("Aplicar tiro legal con puntos actualiza estado")
        void tiroLegalConPuntos() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            partidaIndividual.aplicarTiro(evento, resultado);

            assertEquals(3, partidaIndividual.getEstado().puntajeDe(ANA));
            assertEquals(ANA, partidaIndividual.jugadorActual()); // no cambió
        }

        @Test
        @DisplayName("Aplicar tiro legal sin puntos cambia turno")
        void tiroLegalSinPuntos() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalSinPuntos();

            partidaIndividual.aplicarTiro(evento, resultado);

            assertEquals(BETO, partidaIndividual.jugadorActual());
        }

        @Test
        @DisplayName("Aplicar falta cambia turno")
        void aplicarFalta() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);

            partidaIndividual.aplicarTiro(evento, resultado);

            assertEquals(BETO, partidaIndividual.jugadorActual());
        }

        @Test
        @DisplayName("Aplicar tiro en partida no activa lanza excepción")
        void noActiva() {
            partidaIndividual.pausar();
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            assertThrows(IllegalStateException.class,
                () -> partidaIndividual.aplicarTiro(evento, resultado));
        }
    }

    @Nested
    @DisplayName("Igualdad")
    class Igualdad {

        @Test
        @DisplayName("Dos partidas con mismo id son iguales")
        void igualdadPorId() {
            Mesa otraMesa = new Mesa();
            Partida otra = Partida.individual(ID, ANA, BETO, otraMesa);

            assertEquals(partidaIndividual, otra);
            assertEquals(partidaIndividual.hashCode(), otra.hashCode());
        }

        @Test
        @DisplayName("Dos partidas con distinto id no son iguales")
        void distintoId() {
            Mesa otraMesa = new Mesa();
            Partida otra = Partida.individual("otra-partida", ANA, BETO, otraMesa);

            assertNotEquals(partidaIndividual, otra);
        }
    }
}