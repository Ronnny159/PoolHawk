package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

/**
 * Pruebas de integracion para los controladores REST.
 *
 * <p>Levanta un contexto completo de Spring Boot y simula peticiones
 * HTTP usando MockMvc para validar la capa web de punta a punta.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API REST - Integracion End-to-End")
class MatchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Creacion de partidas")
    class CreacionPartidas {

        @Test
        @DisplayName("POST /api/v1/matches crea una partida de Bola 8")
        void crearPartidaBola8() throws Exception {
            CrearPartidaRequest request = new CrearPartidaRequest(
                TipoModalidad.BOLA_8,
                List.of("Ana"),
                List.of("Beto"));

            mockMvc.perform(post("/api/v1/matches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.modalidad").value("BOLA_8"))
                .andExpect(jsonPath("$.fase").value("EN_CURSO"))
                .andExpect(jsonPath("$.jugadorActual").value("Ana"))
                .andExpect(jsonPath("$.cantidadTiros").value(0));
        }

        @Test
        @DisplayName("POST /api/v1/matches falla si la modalidad es nula")
        void crearPartidaSinModalidad() throws Exception {
            String jsonInvalido = """
                {"equipoA": ["Ana"], "equipoB": ["Beto"]}
                """;

            mockMvc.perform(post("/api/v1/matches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("GET /api/v1/matches/{id} retorna 404 si no existe")
        void obtenerPartidaInexistente() throws Exception {
            mockMvc.perform(get("/api/v1/matches/no-existe-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("Flujo completo")
    class FlujoCompleto {

        @Test
        @DisplayName("Crear partida -> Consultar -> Tirar -> Cambio de turno")
        void flujoCompleto() throws Exception {
            // 1. Crear partida
            CrearPartidaRequest crearRequest = new CrearPartidaRequest(
                TipoModalidad.BOLA_8,
                List.of("Ana"),
                List.of("Beto"));

            String responseJson = mockMvc.perform(post("/api/v1/matches")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(crearRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

            // 2. Extraer IDs
            String matchId = objectMapper.readTree(responseJson).get("id").asText();
            String jugadorId = objectMapper.readTree(responseJson)
                .get("equipoA").get(0).get("id").asText();

            // 3. Registrar un tiro (falta por no tocar bola)
            TiroEventRequest tiroRequest = new TiroEventRequest(
                jugadorId, null, List.of(), List.of(), 5);

            mockMvc.perform(post("/api/v1/matches/" + matchId + "/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(tiroRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esFalta").value(true))
                .andExpect(jsonPath("$.motivo").value("NO_TOCAR_BOLA"))
                .andExpect(jsonPath("$.cambiaTurno").value(true));

            // 4. Verificar que el turno cambio a Beto
            mockMvc.perform(get("/api/v1/matches/" + matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jugadorActual").value("Beto"))
                .andExpect(jsonPath("$.cantidadTiros").value(1));
        }
    }
}