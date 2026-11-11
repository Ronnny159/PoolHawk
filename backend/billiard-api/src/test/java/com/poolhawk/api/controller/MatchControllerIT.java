package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
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

@SpringBootTest
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
        // El servicio rechaza Bola 8 con más de 2 jugadores
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of("Ana", "Luis", "Pedro"), 
            null, null
        );

        mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()); // Ahora espera 400, no 201
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
}