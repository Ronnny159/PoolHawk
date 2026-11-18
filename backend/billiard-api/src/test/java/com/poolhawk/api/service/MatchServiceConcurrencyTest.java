package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.api.dto.TiroEventRequest;
import com.poolhawk.domain.match.FormatoJuego;
import com.poolhawk.domain.match.Partida;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.player.Jugador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas de concurrencia para validar el control optimista de versiones (@Version).
 */
@SpringBootTest
@DisplayName("MatchService - Pruebas de Concurrencia")
class MatchServiceConcurrencyTest {

    @Autowired
    private MatchService matchService;

    @Test
    @DisplayName("Dos hilos simultáneos con distintos eventIds deben generar un conflicto de versión (409) controlado")
    void concurrenciaSimultaneaGeneraConflictoControlado() throws InterruptedException {
        // 1. Preparar partida
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8, FormatoJuego.INDIVIDUAL, List.of("Ana", "Beto"), 254.0, 127.0
        );
        Partida partida = matchService.crear(request);
        String partidaId = partida.getId();
        Jugador ana = partida.getEquipoA().getJugadores().get(0);

        // 2. Configurar hilos concurrentes
        int numHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numHilos);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numHilos);
        
        AtomicReference<Exception> exceptionHilo1 = new AtomicReference<>();
        AtomicReference<Exception> exceptionHilo2 = new AtomicReference<>();

        // ¡CORRECCIÓN CLAVE!: Usamos eventIds DIFERENTES para que ambos hilos intenten 
        // persistir un nuevo tiro y así desencadenar el conflicto de versión (@Version) en la BD.
        TiroEventRequest tiro1 = new TiroEventRequest("evento-concurrente-1", ana.id(), 1, List.of(), List.of(), 5);
        TiroEventRequest tiro2 = new TiroEventRequest("evento-concurrente-2", ana.id(), 1, List.of(), List.of(), 5);

        // Hilo 1
        executor.submit(() -> {
            try {
                startLatch.await(); // Esperar señal para iniciar simultáneamente
                matchService.registrarTiro(partidaId, tiro1);
            } catch (Exception e) {
                exceptionHilo1.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        // Hilo 2
        executor.submit(() -> {
            try {
                startLatch.await();
                matchService.registrarTiro(partidaId, tiro2);
            } catch (Exception e) {
                exceptionHilo2.set(e);
            } finally {
                endLatch.countDown();
            }
        });

        // 3. Disparar la ejecución simultánea
        startLatch.countDown();
        endLatch.await(); // Esperar a que ambos terminen
        executor.shutdown();

        // 4. Verificar que uno tuvo éxito y el otro fue rechazado por conflicto de versión
        long exitos = (exceptionHilo1.get() == null ? 1 : 0) + (exceptionHilo2.get() == null ? 1 : 0);
        long conflictos = 0;
        
        if (exceptionHilo1.get() instanceof ResponseStatusException rse1 && rse1.getStatusCode().is4xxClientError()) conflictos++;
        if (exceptionHilo2.get() instanceof ResponseStatusException rse2 && rse2.getStatusCode().is4xxClientError()) conflictos++;

        assertEquals(1, exitos, "Exactamente un hilo debe tener éxito");
        assertEquals(1, conflictos, "Exactamente un hilo debe recibir un error de cliente (409 Conflict por @Version)");
        
        // Verificar que la partida solo tiene 1 tiro registrado (el del hilo que ganó la carrera)
        Partida finalPartida = matchService.buscar(partidaId);
        assertEquals(1, finalPartida.getEstado().cantidadTiros(), "Solo debe haber 1 tiro registrado en la partida");
    }
}