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
 * Verifica idempotencia durable tras un reinicio real del ApplicationContext.
 *
 * <p><b>Estrategia:</b> La clase se divide en dos métodos ordenados. El primero
 * crea la partida y registra el tiro. El segundo se ejecuta DESPUÉS de que Spring
 * haya cerrado y recreado el contexto (gracias a {@code @DirtiesContext}).
 * Los datos se comparten mediante variables estáticas porque el contexto se
 * destruye entre métodos, pero la base de datos PostgreSQL persiste los datos.</p>
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

    // Variables estáticas para compartir estado entre contextos de Spring.
    // No se limpian entre métodos, es intencional.
    private static String partidaId;
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
        String jugadorId = objectMapper.readTree(res).get("equipoA").get(0).get("id").asText();
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
        // Aseguramos que la fase 1 se ejecutó correctamente
        assertNotNull(partidaId, "La fase 1 debió ejecutarse primero y persistir la partida");
        assertNotNull(payloadOriginal, "El payload original debe estar disponible");

        // Reintento del mismo eventId con el mismo payload
        String bodySegundo = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payloadOriginal)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        // Verificamos que el resultado es EXACTAMENTE el mismo
        JsonNode primero = objectMapper.readTree(bodyPrimero);
        JsonNode segundo = objectMapper.readTree(bodySegundo);
        assertEquals(primero, segundo, "El reintento tras reinicio real debe devolver el ResultadoTiro original");

        // Verificamos que el historial no se duplicó
        String partidaRes = mockMvc.perform(get("/api/v1/matches/" + partidaId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        int cantidadTiros = objectMapper.readTree(partidaRes).get("cantidadTiros").asInt();
        assertEquals(1, cantidadTiros, "Tras el reintento debe haber exactamente 1 tiro persistido");
    }
}