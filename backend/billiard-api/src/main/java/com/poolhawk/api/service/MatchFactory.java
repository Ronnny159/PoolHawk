package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.domain.match.*;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Coordenada;
import com.poolhawk.domain.table.Mesa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Fábrica encargada de construir las instancias de Partida a partir de la petición.
 * Aísla la lógica de inicialización del dominio.
 */
@Component
public class MatchFactory {

    public Partida crearDesdeRequest(String id, CrearPartidaRequest request) {
        double ancho = request.anchoMesa() != null ? request.anchoMesa() : Mesa.ANCHO_DEFECTO;
        double alto = request.altoMesa() != null ? request.altoMesa() : Mesa.ALTO_DEFECTO;
        Mesa mesa = construirMesaInicial(ancho, alto);

        List<Jugador> listaJugadores = request.jugadores().stream()
            .map(nombre -> new Jugador(UUID.randomUUID().toString(), nombre))
            .toList();

        Turnos<Jugador> turnos = new Turnos<>(listaJugadores);
        MatchState estado = new MatchState(id, request.modalidad(), turnos, mesa, Instant.now());
        estado.iniciar();

        return new Partida(id, request.modalidad(), listaJugadores, estado, Instant.now());
    }

    private Mesa construirMesaInicial(double ancho, double alto) {
        Mesa mesa = new Mesa(ancho, alto);
        mesa.agregarBola(Bola.crear(0, new Coordenada(ancho * 0.25, alto * 0.5)));
        
        double cx = ancho * 0.75;
        double cy = alto * 0.5;
        double dx = 6;
        int[] numeros = {1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15};
        int idx = 0;
        for (int fila = 0; fila < 5; fila++) {
            for (int col = 0; col <= fila; col++) {
                double x = cx + fila * dx;
                double y = cy - fila * 3 + col * 6;
                mesa.agregarBola(Bola.crear(numeros[idx++], new Coordenada(x, y)));
            }
        }
        return mesa;
    }
}