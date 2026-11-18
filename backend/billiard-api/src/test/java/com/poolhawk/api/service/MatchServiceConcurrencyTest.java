package com.poolhawk.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de consistencia transaccional y concurrencia (Fase 6).
 *
 * <p>Las pruebas son deterministas: la colisión de versiones se fuerza mediante
 * transacciones independientes coordinadas por {@link CountDownLatch}, o mediante
 * un fallo controlado del almacenamiento. No se depende del azar de planificación
 * de los hilos, por lo que no son pruebas intermitentes.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Fase 6 - Concurrencia optimista, 409 y consistencia de caché")
class MatchServiceConcurrencyTest {

    @Autowired
    private MatchService matchService;

    @SpyBean
    private MatchStorage storage;

    @Autowired
    private PlatformTransactionManager txManager;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Dos transacciones independientes leen la misma versión; la segunda confirma
     * primero y la primera debe fallar al escribir. Demuestra que {@code @Version}
     * detecta la colisión real de escrituras, sin bloqueos de aplicación.
     */
    @Test
    @DisplayName("@Version detecta la colisión entre dos transacciones independientes")
    void versionDetectaColisionEntreTransaccionesIndependientes() throws Exception {
        Partida partida = matchService.crear(new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0));
        String partidaId = partida.getId();
        Long versionInicial = storage.buscar(partidaId).orElseThrow().getVersion();

        CountDownLatch transaccionALeyo = new CountDownLatch(1);
        CountDownLatch transaccionBConfirmo = new CountDownLatch(1);
        AtomicReference<Throwable> falloA = new AtomicReference<>();
        AtomicReference<Throwable> falloB = new AtomicReference<>();

        Thread hiloA = new Thread(() -> {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            try {
                tx.executeWithoutResult(status -> {
                    PartidaEntity snapshotA = storage.buscar(partidaId).orElseThrow();
                    snapshotA.setEstadoPartidaJson("{\"puntajes\":{},\"grupos\":{},\"marca\":\"A\"}");
                    storage.guardar(snapshotA);
                    transaccionALeyo.countDown();
                    esperar(transaccionBConfirmo);
                    // Fuerza el UPDATE ... WHERE version = versionInicial
                    entityManager.flush();
                });
            } catch (Throwable t) {
                falloA.set(t);
            }
        }, "tx-A");

        Thread hiloB = new Thread(() -> {
            TransactionTemplate tx = new TransactionTemplate(txManager);
            try {
                esperar(transaccionALeyo);
                tx.executeWithoutResult(status -> {
                    PartidaEntity snapshotB = storage.buscar(partidaId).orElseThrow();
                    snapshotB.setEstadoPartidaJson("{\"puntajes\":{},\"grupos\":{},\"marca\":\"B\"}");
                    storage.guardar(snapshotB);
                    entityManager.flush();
                });
                transaccionBConfirmo.countDown();
            } catch (Throwable t) {
                transaccionBConfirmo.countDown();
                falloB.set(t);
            }
        }, "tx-B");

        hiloA.start();
        hiloB.start();
        hiloA.join(20_000);
        assertFalse(hiloA.isAlive(), "La transacción A no debe quedar colgada");
        hiloB.join(20_000);
        assertFalse(hiloB.isAlive(), "La transacción B no debe quedar colgada");

        assertNotNull(falloA.get(), "La transacción que escribíó con versión vieja debe fallar");
        assertTrue(esConflictoOptimista(falloA.get()),
            "Se esperaba un conflicto de bloqueo optimista, pero fue: " + falloA.get());
        assertTrue(falloB.get() == null, "La transacción que ganó la carrera no debe fallar");

        Long versionFinal = storage.buscar(partidaId).orElseThrow().getVersion();
        assertTrue(versionFinal > versionInicial,
            "La versión debe incrementarse tras la escritura confirmada");
    }

    /**
     * Un conflicto de versión en el servicio se traduce a HTTP 409, invalida la
     * caché y no altera el historial persistido.
     */
    @Test
    @DisplayName("El conflicto de versión produce 409, invalida caché y conserva un solo tiro")
    void conflictoProduce409InvalidaCacheYConservaHistorial() throws Exception {
        Partida partida = matchService.crear(new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0));
        String partidaId = partida.getId();
        String jugadorId = partida.getEquipoA().getJugadores().get(0).id();

        // Primer tiro confirmado.
        matchService.registrarTiro(partidaId,
            new TiroEventRequest("evt-ok-1", jugadorId, 1, List.of(), List.of(), 5));
        assertTrue(cache().containsKey(partidaId), "La partida confirmada debe estar en caché");

        // Se fuerza el conflicto en la escritura del segundo tiro.
        doThrow(new ObjectOptimisticLockingFailureException(PartidaEntity.class.getName(), partidaId))
            .when(storage).guardar(any(PartidaEntity.class));

        String jugadorDeTurno = matchService.buscar(partidaId).jugadorActual().id();
        TiroEventRequest segundo =
            new TiroEventRequest("evt-conflicto-2", jugadorDeTurno, 2, List.of(), List.of(), 5);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> matchService.registrarTiro(partidaId, segundo));
        assertEquals(409, ex.getStatusCode().value(), "El conflicto debe traducirse a 409 CONFLICT");
        assertFalse(cache().containsKey(partidaId), "La caché debe invalidarse ante el conflicto");

        // El estado recuperado desde la BD no contiene el tiro rechazado.
        reset(storage);
        Partida persistida = matchService.buscar(partidaId);
        assertEquals(1, persistida.getEstado().cantidadTiros(),
            "Debe persistir exactamente un tiro: el confirmado");
        assertTrue(persistida.getEstado().buscarResultadoPorEvento("evt-ok-1").isPresent());
        assertTrue(persistida.getEstado().buscarResultadoPorEvento("evt-conflicto-2").isEmpty());
    }

    /**
     * El conflicto detectado en el commit se expone al cliente como HTTP 409
     * a través del manejador global de excepciones.
     */
    @Test
    @DisplayName("POST /events devuelve HTTP 409 ante conflicto de versión")
    void endpointDevuelve409AnteConflicto() throws Exception {
        Partida partida = matchService.crear(new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0));
        String partidaId = partida.getId();
        String jugadorId = partida.getEquipoA().getJugadores().get(0).id();

        doThrow(new ObjectOptimisticLockingFailureException(PartidaEntity.class.getName(), partidaId))
            .when(storage).guardar(any(PartidaEntity.class));

        TiroEventRequest tiro =
            new TiroEventRequest("evt-http-409", jugadorId, 1, List.of(), List.of(), 5);

        mockMvc.perform(post("/api/v1/matches/" + partidaId + "/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tiro)))
            .andExpect(status().isConflict());

        assertFalse(cache().containsKey(partidaId), "La caché debe invalidarse también vía HTTP");
    }

    /**
     * Accede a la caché de lectura del servicio.
     *
     * @return mapa interno de partidas en memoria.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Partida> cache() {
        Map<String, Partida> cache =
            (Map<String, Partida>) ReflectionTestUtils.getField(matchService, "partidasEnMemoria");
        assertNotNull(cache, "No se pudo acceder al campo 'partidasEnMemoria'");
        return cache;
    }

    /**
     * Determina si una excepción (o alguna de sus causas) representa un conflicto
     * de bloqueo optimista, sin importar la capa que la envuelva.
     *
     * @param error excepción capturada.
     * @return {@code true} si hay un conflicto de versión en la cadena de causas.
     */
    private boolean esConflictoOptimista(Throwable error) {
        Throwable actual = error;
        while (actual != null) {
            if (actual instanceof OptimisticLockingFailureException
                || actual instanceof OptimisticLockException
                || actual.getClass().getSimpleName().contains("StaleObjectState")
                || actual.getClass().getSimpleName().contains("OptimisticLock")) {
                return true;
            }
            actual = actual.getCause();
        }
        return false;
    }

    /**
     * Espera un latch con tiempo límite, propagando el fallo como error de prueba.
     *
     * @param latch latch a esperar.
     */
    private void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tiempo de espera agotado coordinando transacciones");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hilo interrumpido coordinando transacciones", e);
        }
    }
}