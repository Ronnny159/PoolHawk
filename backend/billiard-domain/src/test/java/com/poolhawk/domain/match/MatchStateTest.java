package com.poolhawk.domain.match;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link MatchState}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("MatchState - estado de una partida")
class MatchStateTest {

    private static final String PARTIDA_ID = "partida-1";
    private static final Instant FECHA = Instant.parse("2026-01-01T10:00:00Z");

    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");

    private Turnos turnos;
    private Mesa mesa;
    private MatchState state;

    @BeforeEach
    void setUp() {
        turnos = new Turnos(List.of(ANA, BETO));
        mesa = new Mesa();
        // Colocar la blanca y algunas bolas en la mesa
        mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
        mesa.agregarBola(Bola.crear(1, new Coordenada(60, 60)));
        mesa.agregarBola(Bola.crear(8, new Coordenada(70, 70)));
        state = new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, turnos, mesa, FECHA);
    }

    private TiroEvent tiroDe(Jugador jugador) {
        return new TiroEvent(
            "evt-" + System.nanoTime(),
            PARTIDA_ID,
            jugador,
            null,
            List.of(),
            List.of(),
            Instant.now(),
            Duration.ofSeconds(5)
        );
    }

    @Nested
    @DisplayName("Construcción")
    class Construccion {

        @Test
        @DisplayName("Crea un MatchState en fase CREADA")
        void faseInicial() {
            assertEquals(FasePartida.CREADA, state.getFase());
            assertEquals(PARTIDA_ID, state.getPartidaId());
            assertEquals(TipoModalidad.BOLA_8, state.getModalidad());
            assertNull(state.getGanador());
            assertEquals(0, state.cantidadTiros());
        }

        @Test
        @DisplayName("Inicializa puntajes en cero")
        void puntajesEnCero() {
            assertEquals(0, state.puntajeDe(ANA));
            assertEquals(0, state.puntajeDe(BETO));
        }

        @Test
        @DisplayName("Inicializa grupos en SIN_ASIGNAR")
        void gruposSinAsignar() {
            assertEquals(Grupo.SIN_ASIGNAR, state.grupoDe(ANA));
            assertEquals(Grupo.SIN_ASIGNAR, state.grupoDe(BETO));
        }

        @Test
        @DisplayName("Rechaza partidaId nulo")
        void rechazaIdNulo() {
            assertThrows(NullPointerException.class,
                () -> new MatchState(null, TipoModalidad.BOLA_8, turnos, mesa, FECHA));
        }

        @Test
        @DisplayName("Rechaza partidaId en blanco")
        void rechazaIdBlanco() {
            assertThrows(IllegalArgumentException.class,
                () -> new MatchState("", TipoModalidad.BOLA_8, turnos, mesa, FECHA));
        }

        @Test
        @DisplayName("Rechaza modalidad nula")
        void rechazaModalidadNula() {
            assertThrows(NullPointerException.class,
                () -> new MatchState(PARTIDA_ID, null, turnos, mesa, FECHA));
        }

        @Test
        @DisplayName("Rechaza turnos nulos")
        void rechazaTurnosNulos() {
            assertThrows(NullPointerException.class,
                () -> new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, null, mesa, FECHA));
        }

        @Test
        @DisplayName("Rechaza mesa nula")
        void rechazaMesaNula() {
            assertThrows(NullPointerException.class,
                () -> new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, turnos, null, FECHA));
        }

        @Test
        @DisplayName("Rechaza fechaInicio nula")
        void rechazaFechaNula() {
            assertThrows(NullPointerException.class,
                () -> new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, turnos, mesa, null));
        }
    }

    @Nested
    @DisplayName("Transiciones de fase")
    class Transiciones {

        @Test
        @DisplayName("Iniciar pasa de CREADA a EN_CURSO")
        void iniciar() {
            state.iniciar();
            assertEquals(FasePartida.EN_CURSO, state.getFase());
            assertTrue(state.estaActiva());
        }

        @Test
        @DisplayName("Iniciar dos veces lanza excepción")
        void iniciarDosVeces() {
            state.iniciar();
            assertThrows(IllegalStateException.class, () -> state.iniciar());
        }

        @Test
        @DisplayName("Pausar una partida en curso funciona")
        void pausar() {
            state.iniciar();
            state.pausar();
            assertEquals(FasePartida.PAUSADA, state.getFase());
        }

        @Test
        @DisplayName("Pausar una partida en CREADA lanza excepción")
        void pausarDesdeCreada() {
            assertThrows(IllegalStateException.class, () -> state.pausar());
        }

        @Test
        @DisplayName("Reanudar una partida pausada funciona")
        void reanudar() {
            state.iniciar();
            state.pausar();
            state.reanudar();
            assertEquals(FasePartida.EN_CURSO, state.getFase());
        }

        @Test
        @DisplayName("Reanudar una partida en curso lanza excepción")
        void reanudarDesdeEnCurso() {
            state.iniciar();
            assertThrows(IllegalStateException.class, () -> state.reanudar());
        }

        @Test
        @DisplayName("Finalizar declara ganador y pasa a FINALIZADA")
        void finalizar() {
            state.iniciar();
            state.finalizar(ANA);
            assertEquals(FasePartida.FINALIZADA, state.getFase());
            assertEquals(ANA, state.getGanador());
            assertTrue(state.estaTerminada());
        }

        @Test
        @DisplayName("Finalizar dos veces lanza excepción")
        void finalizarDosVeces() {
            state.iniciar();
            state.finalizar(ANA);
            assertThrows(IllegalStateException.class, () -> state.finalizar(BETO));
        }

        @Test
        @DisplayName("Finalizar con ganador ajeno lanza excepción")
        void ganadorAjeno() {
            state.iniciar();
            Jugador ajeno = new Jugador("j99", "Ajeno");
            assertThrows(IllegalArgumentException.class, () -> state.finalizar(ajeno));
        }

        @Test
        @DisplayName("Abandonar pasa a ABANDONADA")
        void abandonar() {
            state.iniciar();
            state.abandonar();
            assertEquals(FasePartida.ABANDONADA, state.getFase());
            assertTrue(state.estaTerminada());
            assertNull(state.getGanador());
        }

        @Test
        @DisplayName("Abandonar una partida terminada lanza excepción")
        void abandonarTerminada() {
            state.iniciar();
            state.finalizar(ANA);
            assertThrows(IllegalStateException.class, () -> state.abandonar());
        }
    }

    @Nested
    @DisplayName("Puntajes")
    class Puntajes {

        @Test
        @DisplayName("Sumar puntos incrementa el puntaje")
        void sumarPuntos() {
            state.sumarPuntos(ANA, 5);
            assertEquals(5, state.puntajeDe(ANA));
            assertEquals(0, state.puntajeDe(BETO));
        }

        @Test
        @DisplayName("Sumar puntos múltiples veces acumula")
        void sumarAcumulativo() {
            state.sumarPuntos(ANA, 5);
            state.sumarPuntos(ANA, 3);
            assertEquals(8, state.puntajeDe(ANA));
        }

        @Test
        @DisplayName("Sumar cero puntos no cambia nada")
        void sumarCero() {
            state.sumarPuntos(ANA, 0);
            assertEquals(0, state.puntajeDe(ANA));
        }

        @Test
        @DisplayName("Sumar puntos negativos lanza excepción")
        void sumarNegativo() {
            assertThrows(IllegalArgumentException.class,
                () -> state.sumarPuntos(ANA, -1));
        }

        @Test
        @DisplayName("Sumar puntos a jugador ajeno lanza excepción")
        void sumarAjeno() {
            Jugador ajeno = new Jugador("j99", "Ajeno");
            assertThrows(IllegalArgumentException.class,
                () -> state.sumarPuntos(ajeno, 5));
        }

        @Test
        @DisplayName("El mapa de puntajes es inmutable")
        void mapaInmutable() {
            Map<Jugador, Integer> puntajes = state.getPuntajes();
            assertThrows(UnsupportedOperationException.class,
                () -> puntajes.put(ANA, 100));
        }
    }

    @Nested
    @DisplayName("Asignación de grupos")
    class AsignacionGrupos {

        @Test
        @DisplayName("Asignar grupo LISAS a un jugador funciona")
        void asignarLisas() {
            state.asignarGrupo(ANA, Grupo.LISAS);
            assertEquals(Grupo.LISAS, state.grupoDe(ANA));
            assertEquals(Grupo.SIN_ASIGNAR, state.grupoDe(BETO));
        }

        @Test
        @DisplayName("Asignar grupo RAYADAS a un jugador funciona")
        void asignarRayadas() {
            state.asignarGrupo(BETO, Grupo.RAYADAS);
            assertEquals(Grupo.RAYADAS, state.grupoDe(BETO));
        }

        @Test
        @DisplayName("Asignar grupo SIN_ASIGNAR lanza excepción")
        void asignarSinAsignar() {
            assertThrows(IllegalArgumentException.class,
                () -> state.asignarGrupo(ANA, Grupo.SIN_ASIGNAR));
        }

        @Test
        @DisplayName("Reasignar grupo a jugador que ya tiene lanza excepción")
        void reasignar() {
            state.asignarGrupo(ANA, Grupo.LISAS);
            assertThrows(IllegalArgumentException.class,
                () -> state.asignarGrupo(ANA, Grupo.RAYADAS));
        }

        @Test
        @DisplayName("El mapa de grupos es inmutable")
        void mapaInmutable() {
            Map<Jugador, Grupo> grupos = state.getGruposAsignados();
            assertThrows(UnsupportedOperationException.class,
                () -> grupos.put(ANA, Grupo.LISAS));
        }
    }

    @Nested
    @DisplayName("Turnos")
    class TurnosTest {

        @Test
        @DisplayName("El jugador actual es el primero del orden")
        void jugadorActualInicial() {
            assertEquals(ANA, state.jugadorActual());
        }

        @Test
        @DisplayName("Avanzar turno cambia el jugador actual")
        void avanzarTurno() {
            state.avanzarTurno();
            assertEquals(BETO, state.jugadorActual());
        }

        @Test
        @DisplayName("Avanzar turno en círculo vuelve al primero")
        void avanzarCircular() {
            state.avanzarTurno();
            state.avanzarTurno();
            assertEquals(ANA, state.jugadorActual());
        }
    }

    @Nested
    @DisplayName("Historial de tiros")
    class Historial {

        @Test
        @DisplayName("Registrar un tiro lo agrega al historial")
        void registrarTiro() {
            TiroEvent evento = tiroDe(ANA);
            state.registrarTiro(evento);

            assertEquals(1, state.cantidadTiros());
            assertEquals(evento, state.getHistorial().get(0));
        }

        @Test
        @DisplayName("Registrar tiro nulo lanza excepción")
        void registrarNulo() {
            assertThrows(NullPointerException.class,
                () -> state.registrarTiro(null));
        }

        @Test
        @DisplayName("El historial es inmutable")
        void historialInmutable() {
            state.registrarTiro(tiroDe(ANA));
            assertThrows(UnsupportedOperationException.class,
                () -> state.getHistorial().clear());
        }
    }

    @Nested
    @DisplayName("Aplicar tiro")
    class AplicarTiro {

        @BeforeEach
        void iniciarPartida() {
            state.iniciar();
        }

        @Test
        @DisplayName("Aplicar tiro legal con puntos suma y no cambia turno")
        void tiroLegalConPuntos() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            state.aplicarTiro(evento, resultado);

            assertEquals(3, state.puntajeDe(ANA));
            assertEquals(ANA, state.jugadorActual()); // no cambió
            assertEquals(1, state.cantidadTiros());
        }

        @Test
        @DisplayName("Aplicar tiro legal sin puntos cambia turno")
        void tiroLegalSinPuntos() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalSinPuntos();

            state.aplicarTiro(evento, resultado);

            assertEquals(0, state.puntajeDe(ANA));
            assertEquals(BETO, state.jugadorActual()); // cambió
        }

        @Test
        @DisplayName("Aplicar falta no suma puntos y cambia turno")
        void aplicarFalta() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);

            state.aplicarTiro(evento, resultado);

            assertEquals(0, state.puntajeDe(ANA));
            assertEquals(BETO, state.jugadorActual());
            assertEquals(1, state.cantidadTiros());
        }

        @Test
        @DisplayName("Aplicar victoria cambia turno y es registrado")
        void aplicarVictoria() {
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.victoria(5);

            state.aplicarTiro(evento, resultado);

            assertEquals(5, state.puntajeDe(ANA));
            assertEquals(1, state.cantidadTiros());
        }

        @Test
        @DisplayName("Aplicar tiro sin partida activa lanza excepción")
        void aplicarSinActiva() {
            state.pausar(); // no está activa
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            assertThrows(IllegalStateException.class,
                () -> state.aplicarTiro(evento, resultado));
        }

        @Test
        @DisplayName("Aplicar tiro nulo lanza excepción")
        void aplicarTiroNulo() {
            assertThrows(NullPointerException.class,
                () -> state.aplicarTiro(null, ResultadoTiro.tiroLegalSinPuntos()));
        }

        @Test
        @DisplayName("Aplicar resultado nulo lanza excepción")
        void aplicarResultadoNulo() {
            assertThrows(NullPointerException.class,
                () -> state.aplicarTiro(tiroDe(ANA), null));
        }

        @Test
        @DisplayName("Aplicar tiro en partida pausada lanza excepción")
        void aplicarEnPausada() {
            state.pausar();
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            assertThrows(IllegalStateException.class,
                () -> state.aplicarTiro(evento, resultado));
        }

        @Test
        @DisplayName("Tras reanudar se pueden aplicar tiros")
        void aplicarTrasReanudar() {
            state.pausar();
            state.reanudar();
            TiroEvent evento = tiroDe(ANA);
            ResultadoTiro resultado = ResultadoTiro.tiroLegalConPuntos(3);

            assertDoesNotThrow(() -> state.aplicarTiro(evento, resultado));
            assertEquals(3, state.puntajeDe(ANA));
        }
    }

    @Nested
    @DisplayName("Consultas de grupos en mesa")
    class GruposEnMesa {

        @Test
        @DisplayName("quedanBolasDeGrupo LISAS cuando hay bolas lisas")
        void quedanLisas() {
            assertTrue(state.quedanBolasDeGrupo(Grupo.LISAS));
        }

        @Test
        @DisplayName("quedanBolasDeGrupo RAYADAS cuando no hay rayadas")
        void noQuedanRayadas() {
            // La mesa de prueba solo tiene blanca, lisa y la 8, sin rayadas
            assertFalse(state.quedanBolasDeGrupo(Grupo.RAYADAS));
        }

        @Test
        @DisplayName("quedanBolasDeGrupo con SIN_ASIGNAR lanza excepción")
        void sinAsignarLanza() {
            assertThrows(IllegalArgumentException.class,
                () -> state.quedanBolasDeGrupo(Grupo.SIN_ASIGNAR));
        }

        @Test
        @DisplayName("jugadorLimpioSuGrupo false cuando no tiene grupo asignado")
        void sinGrupoAsignado() {
            assertFalse(state.jugadorLimpioSuGrupo(ANA));
        }

        @Test
        @DisplayName("jugadorLimpioSuGrupo false cuando quedan bolas de su grupo")
        void conBolasDeGrupo() {
            state.asignarGrupo(ANA, Grupo.LISAS);
            // La mesa tiene bola 1 (lisa), entonces no está limpio
            assertFalse(state.jugadorLimpioSuGrupo(ANA));
        }
    }

    @Nested
    @DisplayName("Igualdad")
    class Igualdad {

        @Test
        @DisplayName("Dos MatchState con mismo partidaId son iguales")
        void igualdadPorPartidaId() {
            Turnos otrosTurnos = new Turnos(List.of(ANA, BETO));
            Mesa otraMesa = new Mesa();
            MatchState otro = new MatchState(
                PARTIDA_ID, TipoModalidad.BOLA_8, otrosTurnos, otraMesa, FECHA);

            assertEquals(state, otro);
            assertEquals(state.hashCode(), otro.hashCode());
        }

        @Test
        @DisplayName("Dos MatchState con distinto partidaId no son iguales")
        void distintoId() {
            Turnos otrosTurnos = new Turnos(List.of(ANA, BETO));
            Mesa otraMesa = new Mesa();
            MatchState otro = new MatchState(
                "otra-partida", TipoModalidad.BOLA_8, otrosTurnos, otraMesa, FECHA);

            assertNotEquals(state, otro);
        }
    }
}