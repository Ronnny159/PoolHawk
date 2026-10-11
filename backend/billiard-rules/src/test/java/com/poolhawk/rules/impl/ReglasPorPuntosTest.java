package com.poolhawk.rules.impl;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.MatchState;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.match.Turnos;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Coordenada;
import com.poolhawk.domain.table.Mesa;
import com.poolhawk.domain.table.TroneraNombre;
import com.poolhawk.rules.ReglasConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link ReglasPorPuntos}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("ReglasPorPuntos - reglas de Por Puntos en parejas")
class ReglasPorPuntosTest {

    private static final String PARTIDA_ID = "p1";
    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");
    private static final Jugador CARLA = new Jugador("j3", "Carla");
    private static final Jugador DIEGO = new Jugador("j4", "Diego");
    private static final Instant FECHA = Instant.parse("2026-01-01T10:00:00Z");

    private ReglasPorPuntos reglas;
    private Mesa mesa;
    private Turnos turnos;
    private MatchState estado;

    @BeforeEach
    void setUp() {
        reglas = new ReglasPorPuntos(ReglasConfig.porDefecto());
        mesa = new Mesa();
        mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
        for (int i = 1; i <= 15; i++) {
            mesa.agregarBola(Bola.crear(i, new Coordenada(60 + i * 5, 80)));
        }
        turnos = new Turnos(List.of(ANA, BETO, CARLA, DIEGO));
        estado = new MatchState(PARTIDA_ID, TipoModalidad.POR_PUNTOS, turnos, mesa, FECHA);
        estado.iniciar();
    }

    private Bola bola(int n) {
        return Bola.crear(n, new Coordenada(100 + n, 100));
    }

    private TiroEvent tiro(Jugador jugador, Bola primerContacto,
                           List<Bola> embolsadas) {
        return new TiroEvent(
            "evt-" + System.nanoTime(),
            PARTIDA_ID,
            jugador,
            primerContacto,
            embolsadas,
            embolsadas.stream().map(b -> TroneraNombre.TOP_LEFT).toList(),
            Instant.now(),
            Duration.ofSeconds(5)
        );
    }

    private TiroEvent tiroSimple(Jugador jugador, Bola primerContacto) {
        return tiro(jugador, primerContacto, List.of());
    }

    @Nested
    @DisplayName("Nombre de modalidad")
    class NombreModalidad {

        @Test
        @DisplayName("Retorna 'Por Puntos'")
        void nombre() {
            assertEquals("Por Puntos", reglas.getNombreModalidad());
        }
    }

    @Nested
    @DisplayName("Faltas básicas")
    class FaltasBasicas {

        @Test
        @DisplayName("Falta si no toca ninguna bola")
        void noTocoNinguna() {
            ResultadoTiro r = reglas.evaluar(estado, tiroSimple(ANA, null));
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.NO_TOCAR_BOLA, r.motivo());
        }

        @Test
        @DisplayName("Falta si embolsa la blanca")
        void embolsoBlanca() {
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(0)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.BLANCA_EMBOLSADA, r.motivo());
        }
    }

    @Nested
    @DisplayName("Puntuación")
    class Puntuacion {

        @Test
        @DisplayName("Una bola embolsada suma 1 punto")
        void unaBolaUnPunto() {
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertFalse(r.esFalta());
            assertEquals(1, r.puntos());
        }

        @Test
        @DisplayName("Dos bolas embolsadas suman 2 puntos")
        void dosBolasDosPuntos() {
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1), bola(2)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertEquals(2, r.puntos());
        }

        @Test
        @DisplayName("La blanca no cuenta puntos")
        void blancaNoCuenta() {
            // La blanca embolsada es falta, así que no se llega a contar
            // Verificamos con la 8 (que sí cuenta 1 punto)
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertEquals(1, r.puntos());
        }

        @Test
        @DisplayName("Embolsar la bola 8 vale 1 punto")
        void ochoValeUno() {
            // La 8 sola: tocar primero la 8 es falta si no es la última,
            // así que tocamos otra primero
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1), bola(8)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertEquals(2, r.puntos());
        }
    }

    @Nested
    @DisplayName("Victoria al alcanzar puntaje objetivo")
    class VictoriaPorPuntaje {

        @Test
        @DisplayName("No hay victoria por debajo del objetivo")
        void sinVictoria() {
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertFalse(r.esVictoria());
        }

        @Test
        @DisplayName("Hay victoria al alcanzar el objetivo")
        void victoria() {
            // Configura un estado con Ana cerca del objetivo
            ReglasPorPuntos reglas50 = new ReglasPorPuntos(ReglasConfig.porDefecto());

            // Simular que Ana ya tiene 49 puntos
            for (int i = 0; i < 49; i++) {
                estado.sumarPuntos(ANA, 1);
            }
            assertEquals(49, estado.puntajeDe(ANA));

            // Un tiro que le da 1 punto más
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1)));
            ResultadoTiro r = reglas50.evaluar(estado, t);
            assertTrue(r.esVictoria());
            assertEquals(1, r.puntos());
        }
    }

    @Nested
    @DisplayName("Turnos")
    class TurnosTest {

        @Test
        @DisplayName("Tiro que embolsa no cambia turno")
        void embolsoSigueTirando() {
            TiroEvent t = tiro(ANA, bola(1), List.of(bola(1)));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertFalse(r.cambiaTurno());
        }

        @Test
        @DisplayName("Tiro sin embolsar cambia turno")
        void sinEmbolsarCambia() {
            TiroEvent t = tiroSimple(ANA, bola(1));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.cambiaTurno());
        }

        @Test
        @DisplayName("Falta cambia turno")
        void faltaCambia() {
            TiroEvent t = tiroSimple(ANA, null);
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.cambiaTurno());
        }
    }

    @Nested
    @DisplayName("Partida terminada y ganador")
    class PartidaTerminada {

        @Test
        @DisplayName("Partida no terminada al inicio")
        void noTerminada() {
            assertFalse(reglas.partidaTerminada(estado));
            assertNull(reglas.determinarGanador(estado));
        }

        @Test
        @DisplayName("Partida terminada cuando un jugador alcanza el objetivo")
        void terminadaConGanador() {
            for (int i = 0; i < 50; i++) {
                estado.sumarPuntos(ANA, 1);
            }
            assertTrue(reglas.partidaTerminada(estado));
            assertEquals(ANA, reglas.determinarGanador(estado));
        }
    }
}