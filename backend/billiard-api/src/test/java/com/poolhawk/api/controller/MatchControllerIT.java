package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.api.BilliardApplication;
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

    @Test
    @DisplayName("POST /matches rechaza Bola 8 con 3 jugadores (400)")
    void crearPartidaDinamicaInvalida() throws Exception {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of("Ana", "Luis", "Pedro"), 
            null, null
        );

        mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /matches crea una partida válida de Bola 8 (1v1)")
    void crearPartidaValida() throws Exception {
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of("Ana", "Luis"), 
            null, null
        );

        mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.modalidad").value("BOLA_8"))
            .andExpect(jsonPath("$.jugadorActual").value("Ana"));
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
        // 1. Crear partida
        CrearPartidaRequest crearRequest = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null
        );
        String response = mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crearRequest)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        
        String partidaId = objectMapper.readTree(response).get("id").asText();
        String jugadorId = objectMapper.readTree(response).get("equipoA").get(0).get("id").asText();

        // 2. Crear tiro con un eventId fijo
        String eventId = "evento-test-123";
        TiroEventRequest tiroRequest = new TiroEventRequest(
            eventId, jugadorId, 1, List.of(), List.of(), 5
        );

        // 3. Enviar el tiro (debe ser 200 OK)
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isOk());

        // 4. Enviar el mismo tiro otra vez (debe ser 409 Conflict)
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("El tiro ya fue registrado."));
    }
}