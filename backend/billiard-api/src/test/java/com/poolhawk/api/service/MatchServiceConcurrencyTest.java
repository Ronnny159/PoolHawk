package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class MatchServiceConcurrencyTest {

    @Autowired
    private MatchService matchService;

    private String partidaId;
    private String jugador1Id;

    @BeforeEach
    public void setUp() {
        jugador1Id = UUID.randomUUID().toString();
        String jugador2Id = UUID.randomUUID().toString();

        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of(jugador1Id, jugador2Id),
            1.2, 2.4
        );

        var partida = matchService.crear(request);
        this.partidaId = partida.getId();
    }

    @Test
    public void testConcurrentShots_OptimisticLockingWorks() throws InterruptedException {
        int numThreads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    TiroEventRequest tiroRequest = new TiroEventRequest(
                        "event-" + threadId + "-" + UUID.randomUUID(),
                        jugador1Id,
                        8,
                        List.of(1),
                        List.of("TOP_LEFT"),
                        5,
                        null
                    );

                    matchService.registrarTiro(partidaId, tiroRequest);
                    successCount.incrementAndGet();

                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        fail("Unexpected status: " + e.getStatusCode());
                    }
                } catch (Exception e) {
                    fail("Unexpected exception: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "Los hilos no terminaron a tiempo");
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactamente 1 tiro debe procesarse exitosamente");
        assertEquals(numThreads - 1, conflictCount.get(), "Los tiros concurrentes restantes deben ser rechazados con 409 Conflict");
    }
}
