package com.poolhawk.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.BilliardApplication;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica idempotencia durable tras un <b>reinicio real del ApplicationContext</b>.
 */
@SpringBootTest(classes = BilliardApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API REST - Reinicio real del ApplicationContext (H-05)")
class MatchControllerRestartIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String partidaId;
    private static String jugadorId;
    private static TiroEventRequest payloadOriginal;
    private static String bodyPrimero;

    @Test
    @Order(1)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @DisplayName("Fase 1: crear partida y registrar tiro (contexto inicial)")
    void fase1_crearYRegistrarTiro() throws Exception {
        CrearPartidaRequest crear = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), null, null);
        String res = mockMvc.perform(post("/api/v1/matches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crear)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        partidaId = objectMapper.readTree(res).get("id").asText();
        jugadorId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();
        String eventId = "evento-reinicio-real-1";

        payloadOriginal = new TiroEventRequest(eventId, jugadorId, 1, List.of(), List.of(), 5);

        bodyPrimero = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payloadOriginal)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertNotNull(bodyPrimero);
    }

    @Test
    @Order(2)
    @DisplayName("Fase 2: reintento tras reinicio real devuelve el mismo resultado (contexto nuevo)")
    void fase2_reintentarTrasReinicioReal() throws Exception {
        assertNotNull(partidaId, "fase1 debió ejecutarse primero");

        String bodySegundo = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payloadOriginal)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode primero = objectMapper.readTree(bodyPrimero);
        JsonNode segundo = objectMapper.readTree(bodySegundo);
        assertEquals(primero, segundo, "El reintento tras reinicio real debe devolver el ResultadoTiro original");

        String partidaRes = mockMvc.perform(get("/api/v1/matches/" + partidaId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        int cantidadTiros = objectMapper.readTree(partidaRes).get("cantidadTiros").asInt();
        assertEquals(1, cantidadTiros, "Tras el reintento debe haber exactamente 1 tiro persistido");
    }
}