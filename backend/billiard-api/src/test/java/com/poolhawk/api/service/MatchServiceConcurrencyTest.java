
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("MatchService - Pruebas de Concurrencia Optimista")
class MatchServiceConcurrencyTest {

    @Autowired
    private MatchService matchService;

    @Test
    @DisplayName("Dos hilos con la misma versionEsperada: uno gana y el otro recibe 409")
    void concurrenciaSimultaneaGeneraConflictoControlado() throws Exception {

        // 1. Crear una partida nueva.
        CrearPartidaRequest request = new CrearPartidaRequest(
            TipoModalidad.BOLA_8,
            FormatoJuego.INDIVIDUAL,
            List.of("Ana", "Beto"),
            254.0,
            127.0
        );

        Partida partida = matchService.crear(request);
        String partidaId = partida.getId();

        Jugador ana = partida.getEquipoA()
            .getJugadores()
            .get(0);

        // 2. Ambos tiros parten de la versión inicial: cero tiros.
        TiroEventRequest tiro1 = new TiroEventRequest(
            "evento-concurrente-1",
            ana.id(),
            1,
            List.of(),
            List.of(),
            5,
            0
        );

        TiroEventRequest tiro2 = new TiroEventRequest(
            "evento-concurrente-2",
            ana.id(),
            1,
            List.of(),
            List.of(),
            5,
            0
        );

        // 3. Preparar los dos hilos.
        int numHilos = 2;

        ExecutorService executor =
            Executors.newFixedThreadPool(numHilos);

        CountDownLatch readyLatch =
            new CountDownLatch(numHilos);

        CountDownLatch startLatch =
            new CountDownLatch(1);

        try {
            // 4. Enviar ambos trabajos.
            Future<Exception> future1 = executor.submit(() -> {
                readyLatch.countDown();

                try {
                    if (!startLatch.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                            "El hilo 1 no recibió la señal de inicio."
                        );
                    }

                    matchService.registrarTiro(partidaId, tiro1);
                    return null;

                } catch (Exception e) {
                    return e;
                }
            });

            Future<Exception> future2 = executor.submit(() -> {
                readyLatch.countDown();

                try {
                    if (!startLatch.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                            "El hilo 2 no recibió la señal de inicio."
                        );
                    }

                    matchService.registrarTiro(partidaId, tiro2);
                    return null;

                } catch (Exception e) {
                    return e;
                }
            });

            // 5. Esperar hasta que ambos hilos estén preparados.
            assertTrue(
                readyLatch.await(5, TimeUnit.SECONDS),
                "Los dos hilos debían estar preparados."
            );

            // 6. Liberarlos simultáneamente.
            startLatch.countDown();

            // 7. Esperar los resultados reales.
            Exception resultado1 =
                future1.get(10, TimeUnit.SECONDS);

            Exception resultado2 =
                future2.get(10, TimeUnit.SECONDS);

            // 8. Contabilizar éxitos y conflictos HTTP 409.
            int exitos = 0;
            int conflictos409 = 0;

            if (resultado1 == null) {
                exitos++;
            } else if (
                resultado1 instanceof ResponseStatusException rse
                    && rse.getStatusCode().value() == 409
            ) {
                conflictos409++;
            }

            if (resultado2 == null) {
                exitos++;
            } else if (
                resultado2 instanceof ResponseStatusException rse
                    && rse.getStatusCode().value() == 409
            ) {
                conflictos409++;
            }

            // 9. Verificar el comportamiento esperado.
            assertEquals(
                1,
                exitos,
                "Exactamente un hilo debe registrar el tiro."
            );

            assertEquals(
                1,
                conflictos409,
                "Exactamente un hilo debe recibir 409 Conflict."
            );

            // 10. Comprobar el estado final.
            Partida finalPartida = matchService.buscar(partidaId);

            assertEquals(
                1,
                finalPartida.getEstado().cantidadTiros(),
                "La partida debe contener exactamente un tiro."
            );

        } finally {
            executor.shutdownNow();

            assertTrue(
                executor.awaitTermination(5, TimeUnit.SECONDS),
                "Los hilos de prueba debían finalizar."
            );
        }
    }
}
