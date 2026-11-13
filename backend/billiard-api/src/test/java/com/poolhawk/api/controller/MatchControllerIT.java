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
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Luis", "Pedro"), null, null
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
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Luis"), null, null
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
    @DisplayName("POST /events rechaza un tiro duplicado (409)")
    void registrarTiroDuplicadoRechazado() throws Exception {
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

        String eventId = "evento-test-123";
        TiroEventRequest tiroRequest = new TiroEventRequest(
            eventId, jugadorId, 1, List.of(), List.of(), 5
        );

        // 1. Primer intento (OK)
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isOk());

        // 2. Segundo intento (Mismo ID -> 409 Conflict)
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("El tiro ya fue registrado."));
    }

    @Test
    @DisplayName("POST /events rechaza bola inexistente (400)")
    void registrarTiroBolaInexistente() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String pId = objectMapper.readTree(res).get("id").asText();
        String jId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();

        TiroEventRequest tiro = new TiroEventRequest("event-1", jId, 99, List.of(), List.of(), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events rechaza bola repetida (400)")
    void registrarTiroBolaRepetida() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String pId = objectMapper.readTree(res).get("id").asText();
        String jId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();

        TiroEventRequest tiro = new TiroEventRequest("event-2", jId, 1, List.of(1, 1), List.of("TOP_LEFT", "BOTTOM_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events rechaza bola ya embolsada (400)")
    void registrarTiroBolaYaEmbolsada() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String pId = objectMapper.readTree(res).get("id").asText();
        String jId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();

        // 1. Embolsar la bola 1
        TiroEventRequest tiro1 = new TiroEventRequest("event-3", jId, 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro1)))
            .andExpect(status().isOk());

        // 2. Reintentar embolsar la 1 (con otro ID)
        TiroEventRequest tiro2 = new TiroEventRequest("event-4", jId, 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro2)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events falta ordinaria con blanca embolsada mantiene partida en curso")
    void registrarTiroFaltaBlanca() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String pId = objectMapper.readTree(res).get("id").asText();
        String jId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();

        // Enviamos falta metiendo la blanca (id 0)
        TiroEventRequest tiro = new TiroEventRequest("event-blanca", jId, 1, List.of(0), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isOk());

        // Verificamos que la partida sigue EN_CURSO y no hay ganador
        String partidaRes = mockMvc.perform(get("/api/v1/matches/" + pId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fase").value("EN_CURSO"))
            .andExpect(jsonPath("$.ganador").isEmpty())
            .andReturn().getResponse().getContentAsString();

        // NOTA: Para verificar que la blanca está en mesa, necesitaríamos exponer ese dato en el PartidaResponse.
        // Como no está, confiamos en que el test de integración no devolvió error y la fase sigue en curso.
    }
}