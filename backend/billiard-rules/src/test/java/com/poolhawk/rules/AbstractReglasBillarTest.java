package com.poolhawk.rules;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link AbstractReglasBillar}.
 *
 * <p>Valida el template method: precondiciones, validación de shot clock
 * y delegación al método abstracto {@code evaluarTiroLegal}.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("AbstractReglasBillar - template method")
class AbstractReglasBillarTest {

    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");
    private static final String PARTIDA_ID = "p1";
    private static final Instant FECHA = Instant.parse("2026-01-01T10:00:00Z");

    private Mesa mesa;
    private Turnos turnos;
    private MatchState estado;

    @BeforeEach
    void setUp() {
        mesa = new Mesa();
        mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
        mesa.agregarBola(Bola.crear(1, new Coordenada(100, 100)));
        turnos = new Turnos(List.of(ANA, BETO));
        estado = new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, turnos, mesa, FECHA);
        estado.iniciar();
    }

    /**
     * Implementación concreta para tests que siempre devuelve un
     * resultado fijo, permitiendo testear el esqueleto del template.
     */
    private static class ReglasStub extends AbstractReglasBillar {
        private int vecesLlamadoEvaluarTiroLegal = 0;

        ReglasStub(ReglasConfig config) {
            super(config);
        }

        @Override
        public String getNombreModalidad() {
            return "Stub";
        }

        @Override
        protected ResultadoTiro evaluarTiroLegal(MatchState estado, TiroEvent tiro) {
            vecesLlamadoEvaluarTiroLegal++;
            return ResultadoTiro.tiroLegalSinPuntos(true);
        }

        @Override
        public boolean partidaTerminada(MatchState estado) {
            return false;
        }

        @Override
        public Jugador determinarGanador(MatchState estado) {
            return null;
        }

        public int getVecesLlamado() {
            return vecesLlamadoEvaluarTiroLegal;
        }
    }

    private TiroEvent tiroConDuracion(Duration duracion) {
        return new TiroEvent(
            "evt-" + System.nanoTime(),
            PARTIDA_ID,
            ANA,
            null,
            List.of(),
            List.of(),
            Instant.now(),
            duracion
        );
    }

    @Nested
    @DisplayName("Precondiciones")
    class Precondiciones {

        @Test
        @DisplayName("Rechaza estado nulo")
        void rechazaEstadoNulo() {
            ReglasStub reglas = new ReglasStub(ReglasConfig.porDefecto());
            assertThrows(NullPointerException.class,
                () -> reglas.evaluar(null, tiroConDuracion(Duration.ofSeconds(5))));
        }

        @Test
        @DisplayName("Rechaza tiro nulo")
        void rechazaTiroNulo() {
            ReglasStub reglas = new ReglasStub(ReglasConfig.porDefecto());
            assertThrows(NullPointerException.class,
                () -> reglas.evaluar(estado, null));
        }

        @Test
        @DisplayName("Rechaza configuración nula")
        void rechazaConfigNula() {
            assertThrows(NullPointerException.class,
                () -> new ReglasStub(null));
        }
    }

    @Nested
    @DisplayName("Template method sin shot clock")
    class SinShotClock {

        @Test
        @DisplayName("Delega al método abstracto evaluarTiroLegal")
        void delegaAlAbstracto() {
            ReglasStub reglas = new ReglasStub(ReglasConfig.porDefecto());
            TiroEvent tiro = tiroConDuracion(Duration.ofSeconds(5));

            ResultadoTiro r = reglas.evaluar(estado, tiro);

            assertNotNull(r);
            assertFalse(r.esFalta());
            assertEquals(1, reglas.getVecesLlamado());
        }

        @Test
        @DisplayName("No valida duración si no hay shot clock")
        void sinValidacionDeTiempo() {
            ReglasStub reglas = new ReglasStub(ReglasConfig.porDefecto());
            TiroEvent tiroLargo = tiroConDuracion(Duration.ofHours(1));

            ResultadoTiro r = reglas.evaluar(estado, tiroLargo);

            assertFalse(r.esFalta());
            assertEquals(1, reglas.getVecesLlamado());
        }
    }

    @Nested
    @DisplayName("Template method con shot clock")
    class ConShotClock {

        @Test
        @DisplayName("Tiro dentro del tiempo permitido delega al abstracto")
        void dentroDelTiempo() {
            ReglasConfig config = new ReglasConfig(
                false, Duration.ofSeconds(30), false, 50);
            ReglasStub reglas = new ReglasStub(config);
            TiroEvent tiro = tiroConDuracion(Duration.ofSeconds(10));

            ResultadoTiro r = reglas.evaluar(estado, tiro);

            assertFalse(r.esFalta());
            assertEquals(1, reglas.getVecesLlamado());
        }

        @Test
        @DisplayName("Tiro fuera del tiempo devuelve falta sin llamar al abstracto")
        void fueraDelTiempo() {
            ReglasConfig config = new ReglasConfig(
                false, Duration.ofSeconds(30), false, 50);
            ReglasStub reglas = new ReglasStub(config);
            TiroEvent tiro = tiroConDuracion(Duration.ofSeconds(45));

            ResultadoTiro r = reglas.evaluar(estado, tiro);

            assertTrue(r.esFalta());
            assertEquals(FaltaMotivo.TIEMPO_EXCEDIDO, r.motivo());
            assertEquals(0, reglas.getVecesLlamado());
        }

        @Test
        @DisplayName("Tiro justo en el límite es válido")
        void justoEnElLimite() {
            ReglasConfig config = new ReglasConfig(
                false, Duration.ofSeconds(30), false, 50);
            ReglasStub reglas = new ReglasStub(config);
            TiroEvent tiro = tiroConDuracion(Duration.ofSeconds(30));

            ResultadoTiro r = reglas.evaluar(estado, tiro);

            assertFalse(r.esFalta());
            assertEquals(1, reglas.getVecesLlamado());
        }
    }

    @Nested
    @DisplayName("Getter de configuración")
    class GetterConfig {

        @Test
        @DisplayName("getConfig devuelve la configuración recibida")
        void getConfig() {
            ReglasConfig config = ReglasConfig.deTorneo();
            ReglasStub reglas = new ReglasStub(config);

            assertEquals(config, reglas.getConfig());
        }
    }
}