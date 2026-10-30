package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.mapper.PartidaMapper;
import com.poolhawk.api.repository.PartidaRepository;
import com.poolhawk.api.websocket.MatchLiveHandler;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchService - Orquestación dinámica")
class MatchServiceTest {

    @Mock
    private PartidaRepository partidaRepository;

    private MatchService service;

    @BeforeEach
    void setUp() {
        MatchStorage storage = new MatchStorage(partidaRepository);
        MatchLiveHandler liveHandler = new MatchLiveHandler();
        PartidaMapper mapper = new PartidaMapper();
        
        // Simulamos que la BD guarda correctamente
        when(partidaRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        
        service = new MatchService(storage, liveHandler, mapper);
    }

    @Test
    @DisplayName("Crea partida de Bola 8 con 4 jugadores individuales")
    void crearPartida4Jugadores() {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of("Ana", "Luis", "Carlos", "Pedro")
        );

        Partida partida = service.crear(request);

        assertNotNull(partida);
        assertEquals(4, partida.getEstado().getTurnos().cantidadJugadores());
        assertEquals("Ana", partida.jugadorActual().nombre());
    }

    @Test
    @DisplayName("Rechaza partida en parejas con número impar de jugadores")
    void rechazaParejasImpares() {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.POR_PUNTOS,
            FormatoJuego.PAREJAS,
            List.of("Ana", "Luis", "Carlos") // 3 jugadores en parejas = error
        );

        assertThrows(IllegalArgumentException.class, () -> service.crear(request));
    }
}