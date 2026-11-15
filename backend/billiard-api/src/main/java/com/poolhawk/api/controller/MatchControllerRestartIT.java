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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de integración que verifica idempotencia durable tras un <b>reinicio
 * real del ApplicationContext de Spring</b>, no solo vaciando la caché en
 * memoria (H-05).
 *
 * <p>Estrategia:</p>
 * <ol>
 *   <li>Método {@code fase1}: crea partida, registra tiro, guarda el snapshot
 *       de la respuesta y el payload original en campos estáticos, y luego
 *       cierra el ApplicationContext con {@code @DirtiesContext(AFTER_METHOD)}.</li>
 *   <li>Método {@code fase2}: el contexto de Spring se recrea. Todos los beans
 *       (incluido {@code MatchService}, su caché en memoria y {@code MockMvc})
 *       son nuevos. Se reenvía el mismo {@code eventId} con el mismo payload y
 *       se comprueba que el resultado original se devuelve desde PostgreSQL.</li>
 * </ol>
 *
 * <p>Los campos {@code static} sobreviven al cierre del contexto porque la
 * clase de test no se recrea; solo el contexto de Spring.</p>
 *
 * <p><b>Requiere</b> una instancia real de PostgreSQL accesible (no H2), porque
 * la persistencia entre contextos ocurre en la base de datos configurada en
 * {@code application.yml}.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
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

    // Estado compartido entre los dos métodos, sobrevive al cierre del contexto
    private static String partidaId;
    private static String jugadorId;
    private static String eventId;
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
        eventId = "evento-reinicio-real-1";

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
        assertNotNull(bodyPrimero, "fase1 debió guardar el cuerpo original");

        String bodySegundo = mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payloadOriginal)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode primero = objectMapper.readTree(bodyPrimero);
        JsonNode segundo = objectMapper.readTree(bodySegundo);
        assertEquals(primero, segundo,
            "El reintento tras reinicio real del contexto debe devolver el ResultadoTiro original");

        // Verificar que el historial no se duplicó
        String partidaRes = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/matches/" + partidaId))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        int cantidadTiros = objectMapper.readTree(partidaRes).get("cantidadTiros").asInt();
        assertEquals(1, cantidadTiros,
            "Tras el reintento tras reinicio debe haber exactamente 1 tiro persistido");
    }
}