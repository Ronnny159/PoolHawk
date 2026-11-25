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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MatchServiceConcurrencyTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchStorage matchStorage;

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
        
        matchStorage.buscar(partidaId).ifPresent(entity -> {
        });
    }

    @Test
    public void testConcurrentShots_OptimisticLockingWorks() throws InterruptedException {
        int numThreads = 3;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        int[] successCount = {0};
        int[] conflictCount = {0};

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
                    successCount[0]++;

                } catch (ResponseStatusException e) {
                    if (e.getStatusCode().value() == 409) {
                        conflictCount[0]++;
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

        assertEquals(1, successCount[0], "Exactamente 1 tiro debe procesarse exitosamente");
        assertEquals(numThreads - 1, conflictCount[0], "Los tiros concurrentes restantes deben ser rechazados con 409 Conflict");
    }
}
