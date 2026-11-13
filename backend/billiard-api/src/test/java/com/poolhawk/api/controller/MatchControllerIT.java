package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.api.BilliardApplication;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
            .andExpect(jsonPath("$.jugadorActual").value("Ana"))
            .andExpect(jsonPath("$.bolasEnMesa").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/matches/{id} retorna 404 si no existe")
    void obtenerPartidaInexistente() throws Exception {
        mockMvc.perform(get("/api/v1/matches/no-existe-123"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /events reintento idéntico devuelve 200 con el MISMO resultado (H-05)")
    void registrarTiroReintentoIdenticoDevuelve200() throws Exception {
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

        String eventId = "evento-idempotente-1";
        TiroEventRequest tiroRequest = new TiroEventRequest(
            eventId, jugadorId, 1, List.of(), List.of(), 5
        );

        // Primer intento -> 200 OK. Capturamos el cuerpo.
        String bodyPrimero = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // Segundo intento con MISMO eventId y MISMO payload -> 200 OK (no 409). Capturamos cuerpo.
        String bodySegundo = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiroRequest)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // H-05: el cuerpo del reintento debe ser EXACTAMENTE el mismo ResultadoTiro.
        JsonNode primero = objectMapper.readTree(bodyPrimero);
        JsonNode segundo = objectMapper.readTree(bodySegundo);
        assertEquals(primero, segundo,
            "El reintento idempotente debe devolver el mismo ResultadoTiro que el primer envío");

        // El estado no debe haberse duplicado: cantidadTiros == 1
        mockMvc.perform(get("/api/v1/matches/" + partidaId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cantidadTiros").value(1));
    }

    @Test
    @DisplayName("POST /events reintento con eventId repetido y payload distinto -> 400")
    void registrarTiroReintentoPayloadDistintoDevuelve400() throws Exception {
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

        String eventId = "evento-idempotente-2";
        TiroEventRequest primero = new TiroEventRequest(
            eventId, jugadorId, 1, List.of(), List.of(), 5
        );
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(primero)))
            .andExpect(status().isOk());

        // Mismo eventId, pero bolasEmbolsadas distintas -> payload distinto
        TiroEventRequest segundo = new TiroEventRequest(
            eventId, jugadorId, 1, List.of(2), List.of("TOP_LEFT"), 5
        );
        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(segundo)))
            .andExpect(status().isBadRequest());
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

        // 2. Reintentar embolsar la 1 (con otro eventId)
        TiroEventRequest tiro2 = new TiroEventRequest("event-4", jId, 1, List.of(1), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro2)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /events falta ordinaria con blanca embolsada mantiene la blanca en mesa Y cambia turno (H-04)")
    void registrarTiroFaltaBlanca() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String pId = objectMapper.readTree(res).get("id").asText();
        String jId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();

        // Estado ANTES del tiro: turno de Ana
        String jugadorAntes = mockMvc.perform(get("/api/v1/matches/" + pId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String nombreJugadorAntes = objectMapper.readTree(jugadorAntes).get("jugadorActual").asText();
        assertEquals("Ana", nombreJugadorAntes, "El primer turno debe ser de Ana");

        // Enviamos falta metiendo la blanca (id 0)
        TiroEventRequest tiro = new TiroEventRequest("event-blanca", jId, 1, List.of(0), List.of("TOP_LEFT"), 5);
        mockMvc.perform(post("/api/v1/matches/" + pId + "/events").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isOk());

        // Estado DESPUÉS del tiro
        String bodyDespues = mockMvc.perform(get("/api/v1/matches/" + pId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fase").value("EN_CURSO"))
            .andExpect(jsonPath("$.ganador").isEmpty())
            // H-04: la blanca (0) sigue sobre la mesa
            .andExpect(jsonPath("$.bolasEnMesa", Matchers.hasItem(0)))
            // H-04: el turno cambió al oponente
            .andExpect(jsonPath("$.jugadorActual").value("Beto"))
            .andReturn().getResponse().getContentAsString();

        String nombreJugadorDespues = objectMapper.readTree(bodyDespues).get("jugadorActual").asText();
        assertNotEquals(nombreJugadorAntes, nombreJugadorDespues,
            "Tras una falta, el turno debe cambiar al oponente");
    }
}