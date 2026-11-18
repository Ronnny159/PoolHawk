package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.BilliardApplication;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.service.MatchService;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BilliardApplication.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test") // <-- CRÍTICO: Usa H2 y desactiva Flyway
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // <-- Aísla el contexto
@DisplayName("API REST - Integración Dinámica y Persistencia")
class MatchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchService matchService;

    // --- PRUEBAS DE CREACIÓN Y CONSULTA (FASE 3 y 4) ---

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
            .andExpect(jsonPath("$.jugadorActual").value("Ana"))
            .andExpect(jsonPath("$.bolasEnMesa").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id} retorna 404 si no existe")
    void obtenerPartidaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/matches/no-existe-123"))
            .andExpect(status().isNotFound());
    }

    // --- PRUEBAS DE IDEMPOTENCIA Y PERSISTENCIA (FASE 5) ---

    @Test
    @DisplayName("Idempotencia durable: reintento idéntico tras vaciar caché (Simulación de reinicio)")
    void registrarTiroIdempotenteSobreviveReinicioDeCache() throws Exception {
        String[] ctx = crearPartida();
        String partidaId = ctx[0];
        String jugadorId = ctx[1];

        String eventId = "evento-durable-identico";
        TiroEventRequest tiro = new TiroEventRequest(eventId, jugadorId, 1, List.of(), List.of(), 5);

        String bodyPrimero = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        vaciarCacheEnMemoria();

        String bodySegundo = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(bodyPrimero), objectMapper.readTree(bodySegundo),
            "El reintento idéntico debe devolver el mismo ResultadoTiro");

        mockMvc.perform(get("/api/v1/matches/" + partidaId))
            .andExpect(jsonPath("$.cantidadTiros").value(1));
    }

    @Test
    @DisplayName("Idempotencia durable: reintento con payload distinto tras vaciar caché (400)")
    void registrarTiroPayloadDistintoTrasReinicioDeCacheDevuelve400() throws Exception {
        String[] ctx = crearPartida();
        String partidaId = ctx[0];
        String jugadorId = ctx[1];

        String eventId = "evento-durable-distinto";
        TiroEventRequest original = new TiroEventRequest(eventId, jugadorId, 1, List.of(), List.of(), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(original)))
            .andExpect(status().isOk());

        vaciarCacheEnMemoria();

        TiroEventRequest distinto = new TiroEventRequest(eventId, jugadorId, 1, List.of(2), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(distinto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Dos partidas distintas pueden usar el mismo eventId sin colisión global")
    void registrarTiroMismoEventIdDiferentesPartidas() throws Exception {
        String[] ctx1 = crearPartida();
        String partida1Id = ctx1[0];
        String jugador1Id = ctx1[1];

        String[] ctx2 = crearPartida();
        String partida2Id = ctx2[0];
        String jugador2Id = ctx2[1];

        String eventIdCompartido = "evento-global-123";

        TiroEventRequest tiro1 = new TiroEventRequest(eventIdCompartido, jugador1Id, 1, List.of(), List.of(), 5);
        TiroEventRequest tiro2 = new TiroEventRequest(eventIdCompartido, jugador2Id, 1, List.of(), List.of(), 5);

        mockMvc.perform(post("/api/v1/matches/" + partida1Id + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro1)))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/matches/" + partida2Id + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro2)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/matches/" + partida1Id)).andExpect(jsonPath("$.cantidadTiros").value(1));
        mockMvc.perform(get("/api/v1/matches/" + partida2Id)).andExpect(jsonPath("$.cantidadTiros").value(1));
    }

    // --- PRUEBAS DE VALIDACIÓN (FASE 4) ---

    @Test
    @DisplayName("POST /events reintento con null en troneras -> 400")
    void registrarTiroReintentoConTroneraNulaDevuelve400() throws Exception {
        String[] ctx = crearPartida();
        String partidaId = ctx[0];
        String jugadorId = ctx[1];

        String eventId = "evento-tronera-nula";
        TiroEventRequest original = new TiroEventRequest(eventId, jugadorId, 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(original)))
            .andExpect(status().isOk());

        TiroEventRequest conNulo = new TiroEventRequest(eventId, jugadorId, 1, List.of(1), Arrays.asList("TOP_LEFT", null), 5);
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(conNulo)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events rechaza eventId nulo (400)")
    void registrarTiroEventIdNulo() throws Exception {
        String[] ctx = crearPartida();
        TiroEventRequest tiro = new TiroEventRequest(null, ctx[1], 1, List.of(), List.of(), 5);
        mockMvc.perform(post("/api/v1/matches/" + ctx[0] + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events rechaza bola inexistente (400)")
    void registrarTiroBolaInexistente() throws Exception {
        String[] ctx = crearPartida();
        TiroEventRequest tiro = new TiroEventRequest("event-1", ctx[1], 99, List.of(), List.of(), 5);
        mockMvc.perform(post("/api/v1/matches/" + ctx[0] + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events rechaza bola repetida (400)")
    void registrarTiroBolaRepetida() throws Exception {
        String[] ctx = crearPartida();
        TiroEventRequest tiro = new TiroEventRequest("event-2", ctx[1], 1, List.of(1, 1), List.of("TOP_LEFT", "BOTTOM_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + ctx[0] + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events rechaza bola ya embolsada (400)")
    void registrarTiroBolaYaEmbolsada() throws Exception {
        String[] ctx = crearPartida();
        TiroEventRequest tiro1 = new TiroEventRequest("event-3", ctx[1], 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + ctx[0] + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro1)))
            .andExpect(status().isOk());

        TiroEventRequest tiro2 = new TiroEventRequest("event-4", ctx[1], 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + ctx[0] + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro2)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events falta con blanca embolsada mantiene blanca en mesa y cambia turno")
    void registrarTiroFaltaBlanca() throws Exception {
        String[] ctx = crearPartida();
        mockMvc.perform(get("/api/v1/matches/" + ctx[0])).andExpect(jsonPath("$.jugadorActual").value("Ana"));

        TiroEventRequest tiro = new TiroEventRequest("event-blanca", ctx[1], 1, List.of(0), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + ctx[0] + "/events")
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/matches/" + ctx[0]))
            .andExpect(jsonPath("$.fase").value("EN_CURSO"))
            .andExpect(jsonPath("$.ganador").isEmpty())
            .andExpect(jsonPath("$.bolasEnMesa[?(@ == 0)]").exists())
            .andExpect(jsonPath("$.jugadorActual").value("Beto"));
    }

    // --- MÉTODOS AUXILIARES ---

    private String[] crearPartida() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String pId = objectMapper.readTree(res).get("id").asText();
        String jId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();
        return new String[]{pId, jId};
    }

    @SuppressWarnings("unchecked")
    private void vaciarCacheEnMemoria() {
        Map<String, Object> cache = (Map<String, Object>) ReflectionTestUtils.getField(matchService, "partidasEnMemoria");
        if (cache == null) {
            fail("No se pudo acceder al campo 'partidasEnMemoria' de MatchService.");
        }
        cache.clear();
    }
}