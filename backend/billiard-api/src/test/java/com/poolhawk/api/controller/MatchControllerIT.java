package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.api.BilliardApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BilliardApplication.class)
@AutoConfigureMockMvc
@DisplayName("API REST - Integración Dinámica")
class MatchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String partidaId;
    private String jugadorId;

    @BeforeEach
    void setUp() throws Exception {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null
        );
        String response = mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        partidaId = objectMapper.readTree(response).get("id").asText();
        jugadorId = objectMapper.readTree(response).get("equipoA").get(0).get("id").asText();
    }

    @Test
    @DisplayName("POST /matches rechaza Bola 8 con 3 jugadores (400)")
    void crearPartidaDinamicaInvalida() throws Exception {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Luis", "Pedro"), null, null
        );
        mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id} retorna 404 si no existe")
    void obtenerPartidaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/matches/no-existe-123"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /matches/{id}/events rechaza un tiro duplicado (409)")
    void registrarTiroDuplicadoRechazado() throws Exception {
        String eventId = "evento-test-123";
        TiroEventRequest tiroRequest = new TiroEventRequest(eventId, jugadorId, 1, List.of(), List.of(), 5);

        // Primer envío exitoso
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isOk());

        // Segundo envío con el mismo eventId
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("El tiro ya fue registrado."));
    }

    @Test
    @DisplayName("POST /matches/{id}/events rechaza bola inexistente (400)")
    void registrarTiroBolaInexistente() throws Exception {
        TiroEventRequest tiroRequest = new TiroEventRequest("event-1", jugadorId, 99, List.of(), List.of(), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matches/{id}/events rechaza bola repetida (400)")
    void registrarTiroBolaRepetida() throws Exception {
        TiroEventRequest tiroRequest = new TiroEventRequest("event-2", jugadorId, 1, List.of(1, 1), List.of("TOP_LEFT", "BOTTOM_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matches/{id}/events rechaza bola ya embolsada (400)")
    void registrarTiroBolaYaEmbolsada() throws Exception {
        // Embolsar la bola 1
        TiroEventRequest tiro1 = new TiroEventRequest("event-3", jugadorId, 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiro1)))
            .andExpect(status().isOk());

        // Reintentar embolsar la misma bola 1
        TiroEventRequest tiro2 = new TiroEventRequest("event-4", jugadorId, 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiro2)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matches/{id}/events falta ordinaria con blanca embolsada")
    void registrarTiroFaltaBlanca() throws Exception {
        // Falta metiendo la blanca (id 0)
        TiroEventRequest tiro = new TiroEventRequest("event-5", jugadorId, 1, List.of(0), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isOk());
        
        // Verificar que la partida sigue en curso (la blanca no eliminó la partida)
        mockMvc.perform(get("/api/v1/matches/" + partidaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fase").value("EN_CURSO"));
    }
}