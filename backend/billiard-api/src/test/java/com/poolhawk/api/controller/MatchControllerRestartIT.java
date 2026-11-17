package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.BilliardApplication;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BilliardApplication.class)
@AutoConfigureMockMvc
@DisplayName("API REST - Integración y Reinicio Real")
class MatchControllerIT {

    @Test
    @DisplayName("POST /matches rechaza Bola 8 con 3 jugadores (400)")
    void crearPartidaDinamicaInvalida(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
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
    void crearPartidaValida(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
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
    void obtenerPartidaInexistente(MockMvc mockMvc) throws Exception {
        mockMvc.perform(get("/api/v1/matches/no-existe-123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("H-02: Idempotencia durable sobrevive a un REINICIO REAL del contexto de Spring")
    void registrarTiroIdempotenteSobreviveReinicioRealDelContexto() throws Exception {
        // 1. Iniciar primer contexto de aplicación
        ConfigurableApplicationContext context1 = SpringApplication.run(BilliardApplication.class, "--spring.profiles.active=test", "--server.port=0");
        MockMvc mockMvc1 = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context1).build();
        ObjectMapper mapper1 = context1.getBean(ObjectMapper.class);

        // 2. Crear partida y registrar el primer tiro
        String[] ctx = crearPartida(mockMvc1, mapper1);
        String partidaId = ctx[0];
        String jugadorId = ctx[1];
        String eventId = "evento-reinicio-real-123";

        TiroEventRequest tiro = new TiroEventRequest(eventId, jugadorId, 1, List.of(), List.of(), 5);
        String bodyPrimero = mockMvc1.perform(post("/api/v1/matches/" + partidaId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper1.writeValueAsString(tiro)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 3. Cerrar el primer contexto (simula el apagado del servidor)
        // La BD H2 en memoria persiste gracias a DB_CLOSE_DELAY=-1 en application-test.yml
        context1.close();

        // 4. Iniciar un SEGUNDO contexto nuevo (simula el reinicio del servidor)
        ConfigurableApplicationContext context2 = SpringApplication.run(BilliardApplication.class, "--spring.profiles.active=test", "--server.port=0");
        MockMvc mockMvc2 = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context2).build();
        ObjectMapper mapper2 = context2.getBean(ObjectMapper.class);

        // 5. Reintentar la misma petición con el mismo eventId en el nuevo contexto
        String bodySegundo = mockMvc2.perform(post("/api/v1/matches/" + partidaId + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper2.writeValueAsString(tiro)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 6. Verificar que el resultado es idéntico (idempotencia recuperada desde la BD, no de caché)
        JsonNode primero = mapper2.readTree(bodyPrimero);
        JsonNode segundo = mapper2.readTree(bodySegundo);
        assertEquals(primero, segundo, "El reintento tras un reinicio real del contexto debe devolver el ResultadoTiro original desde la BD");

        mockMvc2.perform(get("/api/v1/matches/" + partidaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadTiros").value(1));

        // Limpieza final
        context2.close();
    }

    @Test
    @DisplayName("H-05: Dos partidas distintas pueden usar el mismo eventId sin colisión global")
    void registrarTiroMismoEventIdDiferentesPartidas(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        String[] ctx1 = crearPartida(mockMvc, objectMapper);
        String partida1Id = ctx1[0];
        String jugador1Id = ctx1[1];

        String[] ctx2 = crearPartida(mockMvc, objectMapper);
        String partida2Id = ctx2[0];
        String jugador2Id = ctx2[1];

        String eventIdCompartido = "evento-global-123";
        TiroEventRequest tiro1 = new TiroEventRequest(eventIdCompartido, jugador1Id, 1, List.of(), List.of(), 5);
        TiroEventRequest tiro2 = new TiroEventRequest(eventIdCompartido, jugador2Id, 1, List.of(), List.of(), 5);

        mockMvc.perform(post("/api/v1/matches/" + partida1Id + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tiro1)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/matches/" + partida2Id + "/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tiro2)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/matches/" + partida1Id))
                .andExpect(jsonPath("$.cantidadTiros").value(1));
        mockMvc.perform(get("/api/v1/matches/" + partida2Id))
                .andExpect(jsonPath("$.cantidadTiros").value(1));
    }

    // --- Helpers ---

    private String[] crearPartida(MockMvc mockMvc, ObjectMapper mapper) throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(
                TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(crear)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String pId = mapper.readTree(res).get("id").asText();
        String jId = mapper.readTree(res).get("equipoA").get(0).get("id").asText();
        return new String[]{pId, jId};
    }
}