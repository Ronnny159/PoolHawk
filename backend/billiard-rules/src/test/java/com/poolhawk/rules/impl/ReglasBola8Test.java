package com.poolhawk.rules.impl;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.Grupo;
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
 * Pruebas unitarias de {@link ReglasBola8}.
 *
 * <p>Cubre el orden de validación completo, incluyendo prioridad de
 * reglas (bola 8 antes de tiempo tiene prioridad sobre bola equivocada).</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("ReglasBola8 - reglas de Bola 8")
class ReglasBola8Test {

    private static final String PARTIDA_ID = "p1";
    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");
    private static final Instant FECHA = Instant.parse("2026-01-01T10:00:00Z");

    private ReglasBola8 reglas;
    private Mesa mesa;
    private Turnos turnos;
    private MatchState estado;

    @BeforeEach
    void setUp() {
        reglas = new ReglasBola8(ReglasConfig.porDefecto());
        mesa = new Mesa();
        // Blanca
        mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
        // Lisas 1-7
        for (int i = 1; i <= 7; i++) {
            mesa.agregarBola(Bola.crear(i, new Coordenada(100 + i * 5, 100)));
        }
        // Ocho
        mesa.agregarBola(Bola.crear(8, new Coordenada(200, 100)));
        // Rayadas 9-15
        for (int i = 9; i <= 15; i++) {
            mesa.agregarBola(Bola.crear(i, new Coordenada(100 + i * 5, 120)));
        }

        turnos = new Turnos(List.of(ANA, BETO));
        estado = new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, turnos, mesa, FECHA);
        estado.iniciar();
    }

    private Bola bola(int n) {
        return Bola.crear(n, new Coordenada(100 + n, 100));
    }

    private TiroEvent tiro(Jugador jugador, Bola primerContacto,
                           List<Bola> embolsadas, List<TroneraNombre> troneras) {
        return new TiroEvent(
            "evt-" + System.nanoTime(),
            PARTIDA_ID,
            jugador,
            primerContacto,
            embolsadas,
            troneras,
            Instant.now(),
            Duration.ofSeconds(5)
        );
    }

    private TiroEvent tiroSimple(Jugador jugador, Bola primerContacto) {
        return tiro(jugador, primerContacto, List.of(), List.of());
    }

    private TiroEvent tiroConEmbolsada(Jugador jugador, Bola primerContacto,
                                       Bola embolsada) {
        return tiro(jugador, primerContacto, List.of(embolsada),
            List.of(TroneraNombre.TOP_LEFT));
    }

    @Nested
    @DisplayName("Nombre de modalidad")
    class NombreModalidad {

        @Test
        @DisplayName("Retorna 'Bola 8'")
        void nombre() {
            assertEquals("Bola 8", reglas.getNombreModalidad());
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
            TiroEvent t = tiroConEmbolsada(ANA, bola(1), bola(0));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.BLANCA_EMBOLSADA, r.motivo());
        }

        @Test
        @DisplayName("Falta si toca primero la bola 8 sin grupo asignado")
        void tocaOchoSinGrupo() {
            TiroEvent t = tiroSimple(ANA, bola(8));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.BOLA_EQUIVOCADA, r.motivo());
        }
    }

    @Nested
    @DisplayName("Primer contacto con grupo asignado")
    class PrimerContactoConGrupo {

        @BeforeEach
        void asignarGrupos() {
            estado.asignarGrupo(ANA, Grupo.LISAS);
            estado.asignarGrupo(BETO, Grupo.RAYADAS);
        }

        @Test
        @DisplayName("Legal si toca primero una bola de su grupo")
        void tocaSuGrupo() {
            TiroEvent t = tiroSimple(ANA, bola(3));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertFalse(r.esFalta());
        }

        @Test
        @DisplayName("Falta si toca primero una bola del grupo opuesto")
        void tocaGrupoOpuesto() {
            TiroEvent t = tiroSimple(ANA, bola(11));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.BOLA_EQUIVOCADA, r.motivo());
        }

        @Test
        @DisplayName("Falta si toca primero la bola 8")
        void tocaOcho() {
            TiroEvent t = tiroSimple(ANA, bola(8));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.BOLA_EQUIVOCADA, r.motivo());
        }
    }

    @Nested
    @DisplayName("Victoria")
    class Victoria {

        @Test
        @DisplayName("Victoria al embolsar la 8 con grupo limpio")
        void victoriaConGrupoLimpio() {
            estado.asignarGrupo(ANA, Grupo.LISAS);
            estado.asignarGrupo(BETO, Grupo.RAYADAS);

            // Embolsar todas las lisas
            for (int i = 1; i <= 7; i++) {
                mesa.embolsarBola(i);
            }

            TiroEvent t = tiroConEmbolsada(ANA, bola(8), bola(8));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esVictoria());
            assertFalse(r.esFalta());
        }
    }

    @Nested
    @DisplayName("Derrota por bola 8 antes de tiempo")
    class Derrota {

        @Test
        @DisplayName("Falta si embolsa la 8 con grupo sucio")
        void faltaOchoConGrupoSucio() {
            estado.asignarGrupo(ANA, Grupo.LISAS);
            estado.asignarGrupo(BETO, Grupo.RAYADAS);

            // NO embolsar las lisas
            TiroEvent t = tiroConEmbolsada(ANA, bola(8), bola(8));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO, r.motivo());
        }
    }

    @Nested
    @DisplayName("Asignación de grupos")
    class AsignacionGrupos {

        @Test
        @DisplayName("No asigna grupo si no embolsó nada")
        void noAsignaSinEmbolsar() {
            TiroEvent t = tiroSimple(ANA, bola(3));
            reglas.evaluar(estado, t);
            assertEquals(Grupo.SIN_ASIGNAR, estado.grupoDe(ANA));
            assertEquals(Grupo.SIN_ASIGNAR, estado.grupoDe(BETO));
        }
    }

    @Nested
    @DisplayName("Turnos")
    class TurnosTest {

        @BeforeEach
        void asignarGrupos() {
            estado.asignarGrupo(ANA, Grupo.LISAS);
            estado.asignarGrupo(BETO, Grupo.RAYADAS);
        }

        @Test
        @DisplayName("Tiro legal sin embolsar cambia turno")
        void tiroLegalCambiaTurno() {
            TiroEvent t = tiroSimple(ANA, bola(3));
            ResultadoTiro r = reglas.evaluar(estado, t);
            assertTrue(r.cambiaTurno());
        }

        @Test
        @DisplayName("Falta cambia turno")
        void faltaCambiaTurno() {
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
        @DisplayName("Partida terminada tras finalizar")
        void terminadaTrasFinalizar() {
            estado.finalizar(ANA);
            assertTrue(reglas.partidaTerminada(estado));
            assertEquals(ANA, reglas.determinarGanador(estado));
        }
    }

    @Nested
    @DisplayName("Casos borde adicionales")
    class CasosBorde {

        @Test
        @DisplayName("Falta por tiro fuera de shot clock")
        void faltaPorShotClock() {
            ReglasConfig config = new ReglasConfig(
                false, Duration.ofSeconds(10), false, 50);
            ReglasBola8 reglasConClock = new ReglasBola8(config);

            TiroEvent tiroLargo = new TiroEvent(
                "evt-largo", PARTIDA_ID, ANA, bola(1),
                List.of(), List.of(), Instant.now(), Duration.ofSeconds(20));

            ResultadoTiro r = reglasConClock.evaluar(estado, tiroLargo);
            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.TIEMPO_EXCEDIDO, r.motivo());
        }

        @Test
        @DisplayName("Tiro legal con múltiples bolas embolsadas")
        void tiroConMultiplesEmbolsadas() {
            estado.asignarGrupo(ANA, Grupo.LISAS);
            estado.asignarGrupo(BETO, Grupo.RAYADAS);

            TiroEvent t = tiro(ANA, bola(1),
                List.of(bola(1), bola(2), bola(3)),
                List.of(TroneraNombre.TOP_LEFT, TroneraNombre.TOP_RIGHT,
                    TroneraNombre.MIDDLE_LEFT));

            ResultadoTiro r = reglas.evaluar(estado, t);
            assertFalse(r.esFalta());
            assertFalse(r.cambiaTurno());  // sigue tirando
        }

        @Test
        @DisplayName("Partida terminada sin ganador (abandonada)")
        void partidaAbandonada() {
            estado.abandonar();
            assertTrue(reglas.partidaTerminada(estado));
            assertNull(reglas.determinarGanador(estado));
        }

        @Test
        @DisplayName("Victoria se detecta correctamente")
        void victoriaDetectada() {
            estado.asignarGrupo(ANA, Grupo.LISAS);
            estado.asignarGrupo(BETO, Grupo.RAYADAS);
            for (int i = 1; i <= 7; i++) {
                mesa.embolsarBola(i);
            }

            TiroEvent t = tiroConEmbolsada(ANA, bola(8), bola(8));
            ResultadoTiro r = reglas.evaluar(estado, t);

            assertTrue(r.esVictoria());
            assertFalse(r.esFalta());
            assertFalse(r.fueExitoso());  // victoria no es "tiro exitoso normal"
        }
    }
}