package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.api.repository.PartidaRepository;
import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.match.FasePartida;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.poolhawk.api.mapper.PartidaMapper;


import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService - orquestacion (Unit Test)")
class MatchServiceTest {

    @Mock
    private PartidaRepository partidaRepository;

    private MatchStorage storage;
    private MatchService service;

    @BeforeEach
    void setUp() {
        storage = new MatchStorage(partidaRepository);

        MatchLiveHandler liveHandler = new MatchLiveHandler();
        PartidaMapper mapper = new PartidaMapper();

        service = new MatchService(storage, liveHandler, mapper);
    }

    private Partida crearPartidaBola8() {
        // AHORA: Simulamos el guardado de PartidaEntity, no de Partida
        when(partidaRepository.save(any(PartidaEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        return service.crear(new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            List.of("Ana"),
            List.of("Beto")));
    }

    private Partida crearPartidaPorPuntos() {
        when(partidaRepository.save(any(PartidaEntity.class))).thenAnswer(i -> i.getArguments()[0]);
        return service.crear(new CrearPartidaRequest(
            TipoModalidad.POR_PUNTOS,
            List.of("Ana", "Carla"),
            List.of("Beto", "Diego")));
    }

    @Nested
    @DisplayName("Creacion de partidas")
    class Creacion {
        @Test
        @DisplayName("Crea partida de Bola 8 correctamente")
        void crearBola8() {
            Partida partida = crearPartidaBola8();
            assertNotNull(partida);
            assertEquals(TipoModalidad.BOLA_8, partida.getModalidad());
            assertEquals(FasePartida.EN_CURSO, partida.getFase());
        }

        @Test
        @DisplayName("Crea partida de Por Puntos correctamente")
        void crearPorPuntos() {
            Partida partida = crearPartidaPorPuntos();
            assertEquals(TipoModalidad.POR_PUNTOS, partida.getModalidad());
            assertEquals(2, partida.getEquipoA().cantidadJugadores());
        }
    }

    @Nested
    @DisplayName("Busqueda y listado")
    class BusquedaListado {
        @Test
        @DisplayName("Busca una partida existente (desde memoria)")
        void buscarExistente() {
            // Al crearla, se guarda en partidasEnMemoria del servicio
            Partida creada = crearPartidaBola8();

            // Buscamos por ID, el servicio la saca de la caché
            Partida encontrada = service.buscar(creada.getId());
            assertEquals(creada.getId(), encontrada.getId());
        }

        @Test
        @DisplayName("Buscar partida inexistente lanza excepcion")
        void buscarInexistente() {
            // Simulamos que no está ni en memoria ni en la base de datos
            when(partidaRepository.findById("no-existe")).thenReturn(Optional.empty());
            assertThrows(PartidaNoEncontradaException.class, () -> service.buscar("no-existe"));
        }
    }

    @Nested
    @DisplayName("Registro de tiros")
    class RegistroTiros {
        @Test
        @DisplayName("Falta por no tocar bola cambia el turno")
        void faltaPorNoTocar() {
            Partida partida = crearPartidaBola8();
            String jugadorId = partida.getEquipoA().getJugadores().get(0).id();

            TiroEventRequest request = new TiroEventRequest(jugadorId, null, List.of(), List.of(), 5);
            
            ResultadoTiro resultado = service.registrarTiro(partida.getId(), request);

            assertTrue(resultado.esFalta());
            assertEquals(FaltaMotivo.NO_TOCAR_BOLA, resultado.motivo());
            assertTrue(resultado.cambiaTurno());
        }
    }
}