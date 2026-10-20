package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.match.FasePartida;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link MatchService}.
 *
 * <p>Verifica la orquestacion de partidas: creacion, busqueda, listado
 * y registro de tiros con evaluacion de reglas.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("MatchService - orquestacion de partidas")
class MatchServiceTest {

    private MatchStorage storage;
    private MatchService service;

    @BeforeEach
    void setUp() {
        storage = new MatchStorage(null);
        service = new MatchService(storage);
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private Partida crearPartidaBola8() {
        return service.crear(new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            List.of("Ana"),
            List.of("Beto")));
    }

    private Partida crearPartidaPorPuntos() {
        return service.crear(new CrearPartidaRequest(
            TipoModalidad.POR_PUNTOS,
            List.of("Ana", "Carla"),
            List.of("Beto", "Diego")));
    }

    // ─────────────────────────────────────────────────────────────
    // Creacion de partidas
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Creacion de partidas")
    class Creacion {

        @Test
        @DisplayName("Crea partida de Bola 8 con 2 jugadores individuales")
        void crearBola8() {
            Partida partida = crearPartidaBola8();

            assertNotNull(partida);
            assertNotNull(partida.getId());
            assertEquals(TipoModalidad.BOLA_8, partida.getModalidad());
            assertEquals(FasePartida.EN_CURSO, partida.getFase());
            assertEquals(1, partida.getEquipoA().cantidadJugadores());
            assertEquals(1, partida.getEquipoB().cantidadJugadores());
            assertEquals(1, storage.cantidad());
        }

        @Test
        @DisplayName("Crea partida de Por Puntos con 2 equipos de 2 jugadores")
        void crearPorPuntos() {
            Partida partida = crearPartidaPorPuntos();

            assertEquals(TipoModalidad.POR_PUNTOS, partida.getModalidad());
            assertEquals(2, partida.getEquipoA().cantidadJugadores());
            assertEquals(2, partida.getEquipoB().cantidadJugadores());
            assertEquals(1, storage.cantidad());
        }

        @Test
        @DisplayName("Rechaza equipo con 3 jugadores")
        void rechazaEquipoCon3Jugadores() {
            CrearPartidaRequest request = new CrearPartidaRequest(
                TipoModalidad.POR_PUNTOS,
                List.of("A", "B", "C"),
                List.of("D", "E"));

            assertThrows(IllegalArgumentException.class,
                () -> service.crear(request));
        }

        @Test
        @DisplayName("Rechaza equipo vacio")
        void rechazaEquipoVacio() {
            CrearPartidaRequest request = new CrearPartidaRequest(
                TipoModalidad.BOLA_8,
                List.of(),
                List.of("Beto"));

            assertThrows(IllegalArgumentException.class,
                () -> service.crear(request));
        }

        @Test
        @DisplayName("Cada partida tiene un id unico")
        void idsUnicos() {
            Partida p1 = crearPartidaBola8();
            Partida p2 = crearPartidaBola8();

            assertNotEquals(p1.getId(), p2.getId());
            assertEquals(2, storage.cantidad());
        }

        @Test
        @DisplayName("La partida queda en fase EN_CURSO tras crearse")
        void faseEnCurso() {
            Partida partida = crearPartidaBola8();
            assertEquals(FasePartida.EN_CURSO, partida.getFase());
            assertTrue(partida.estaActiva());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Busqueda y listado
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Busqueda y listado")
    class BusquedaListado {

        @Test
        @DisplayName("Busca una partida existente por id")
        void buscarExistente() {
            Partida creada = crearPartidaBola8();
            Partida encontrada = service.buscar(creada.getId());

            assertEquals(creada.getId(), encontrada.getId());
        }

        @Test
        @DisplayName("Buscar partida inexistente lanza PartidaNoEncontradaException")
        void buscarInexistente() {
            assertThrows(PartidaNoEncontradaException.class,
                () -> service.buscar("no-existe"));
        }

        @Test
        @DisplayName("Listar partidas retorna todas las creadas")
        void listarTodas() {
            crearPartidaBola8();
            crearPartidaPorPuntos();

            List<Partida> todas = service.listarTodas();
            assertEquals(2, todas.size());
        }

        @Test
        @DisplayName("Listar sin partidas retorna lista vacia")
        void listarVacio() {
            assertTrue(service.listarTodas().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Registro de tiros
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Registro de tiros")
    class RegistroTiros {

        @Test
        @DisplayName("Tiro que no toca bola produce falta NO_TOCAR_BOLA")
        void faltaPorNoTocar() {
            Partida partida = crearPartidaBola8();
            String jugadorId = partida.getEquipoA().getJugadores().get(0).id();

            TiroEventRequest request = new TiroEventRequest(
                jugadorId, null, List.of(), List.of(), 5);

            ResultadoTiro resultado = service.registrarTiro(partida.getId(), request);

            assertTrue(resultado.esFalta());
            assertEquals(FaltaMotivo.NO_TOCAR_BOLA, resultado.motivo());
            assertTrue(resultado.cambiaTurno());
        }

        @Test
        @DisplayName("Tiro legal con primera bola tocada no produce falta")
        void tiroLegalSinEmbolsar() {
            Partida partida = crearPartidaBola8();
            String jugadorId = partida.getEquipoA().getJugadores().get(0).id();

            // La bola 1 es lisa, no hay grupo asignado aún.
            // Tocar primero la 1 es legal.
            TiroEventRequest request = new TiroEventRequest(
                jugadorId, 1, List.of(), List.of(), 5);

            ResultadoTiro resultado = service.registrarTiro(partida.getId(), request);

            assertFalse(resultado.esFalta());
            assertTrue(resultado.cambiaTurno());
        }

        @Test
        @DisplayName("Registrar tiro en partida inexistente lanza excepcion")
        void tiroEnPartidaInexistente() {
            TiroEventRequest request = new TiroEventRequest(
                "j1", 1, List.of(), List.of(), 5);

            assertThrows(PartidaNoEncontradaException.class,
                () -> service.registrarTiro("no-existe", request));
        }

        @Test
        @DisplayName("Registrar tiro con jugador ajeno lanza IllegalArgumentException")
        void tiroConJugadorAjeno() {
            Partida partida = crearPartidaBola8();

            TiroEventRequest request = new TiroEventRequest(
                "jugador-fantasma", 1, List.of(), List.of(), 5);

            assertThrows(IllegalArgumentException.class,
                () -> service.registrarTiro(partida.getId(), request));
        }

        @Test
        @DisplayName("El historial de la partida se incrementa tras cada tiro")
        void historialIncrementa() {
            Partida partida = crearPartidaBola8();
            String jugadorId = partida.getEquipoA().getJugadores().get(0).id();

            TiroEventRequest request = new TiroEventRequest(
                jugadorId, 1, List.of(), List.of(), 5);

            assertEquals(0, partida.getEstado().cantidadTiros());
            service.registrarTiro(partida.getId(), request);
            assertEquals(1, partida.getEstado().cantidadTiros());
        }

        @Test
        @DisplayName("Falta cambia el turno al siguiente jugador")
        void faltaCambiaTurno() {
            Partida partida = crearPartidaBola8();
            String anaId = partida.getEquipoA().getJugadores().get(0).id();

            assertEquals("Ana", partida.jugadorActual().nombre());

            TiroEventRequest request = new TiroEventRequest(
                anaId, null, List.of(), List.of(), 5);

            service.registrarTiro(partida.getId(), request);

            assertEquals("Beto", partida.jugadorActual().nombre());
        }
    }
}