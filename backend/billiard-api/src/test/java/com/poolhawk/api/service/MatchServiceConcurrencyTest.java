package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.exception.PartidaNoEncontradaException;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Jugador;
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
    private String jugador2Id;

    @BeforeEach
    public void setUp() {
        // Usar nombres para los jugadores, no IDs
        String nombreJugador1 = "Jugador 1";
        String nombreJugador2 = "Jugador 2";

        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of(nombreJugador1, nombreJugador2),
            254.0, 127.0  // Mesa 9 pies en cm
        );

        var partida = matchService.crear(request);
        this.partidaId = partida.getId();
        
        // Obtener los IDs REALES de los jugadores creados en la partida
        Jugador j1 = partida.getEquipoA().getJugadores().get(0);
        Jugador j2 = partida.getEquipoB().getJugadores().get(0);
        this.jugador1Id = j1.id();
        this.jugador2Id = j2.id();
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
                        jugador1Id,  // El primer jugador es el que tiene el turno inicial
                        1,  // Primera bola tocada: bola 1 (sólida), válida como primer contacto en Bola 8
                        List.of(1),
                        List.of("TOP_LEFT"),
                        5,
                        null
                    );

                    matchService.registrarTiro(partidaId, tiroRequest);
                    successCount.incrementAndGet();
                    System.out.println("Thread " + threadId + " SUCCESS");

                } catch (ResponseStatusException e) {
                    System.out.println("Thread " + threadId + " ResponseStatusException: " + e.getStatusCode() + " - " + e.getReason());
                    if (e.getStatusCode().value() == 409) {
                        conflictCount.incrementAndGet();
                    } else {
                        fail("Unexpected status: " + e.getStatusCode() + " - " + e.getReason());
                    }
                } catch (Exception e) {
                    System.out.println("Thread " + threadId + " Exception: " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
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
