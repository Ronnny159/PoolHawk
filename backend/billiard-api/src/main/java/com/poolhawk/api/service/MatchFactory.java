package com.poolhawk.api.service;

import com.poolhawk.api.dto.CrearPartidaRequest;
import com.poolhawk.domain.match.*;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Coordenada;
import com.poolhawk.domain.table.Mesa;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fábrica encargada de construir las instancias de Partida con equipos explícitos.
 */
@Component
public class MatchFactory {

    public Partida crearDesdeRequest(String id, CrearPartidaRequest request) {
        double ancho = request.anchoMesa() != null ? request.anchoMesa() : Mesa.ANCHO_DEFECTO;
        double alto = request.altoMesa() != null ? request.altoMesa() : Mesa.ALTO_DEFECTO;
        Mesa mesa = construirMesaInicial(ancho, alto);

        List<Jugador> listaJugadores = new ArrayList<>();
        for (String nombre : request.jugadores()) {
            listaJugadores.add(new Jugador(UUID.randomUUID().toString(), nombre.trim()));
        }

        int cantidad = listaJugadores.size();
        Equipo equipoA;
        Equipo equipoB;

        if (cantidad == 2) {
            // 1 vs 1
            equipoA = Equipo.individual("eA-" + id, listaJugadores.get(0).nombre(), listaJugadores.get(0));
            equipoB = Equipo.individual("eB-" + id, listaJugadores.get(1).nombre(), listaJugadores.get(1));
        } else if (cantidad == 4) {
            // 2 vs 2: [A1, B1, A2, B2] según el contrato del frontend
            equipoA = Equipo.pareja("eA-" + id, 
                listaJugadores.get(0).nombre() + " & " + listaJugadores.get(2).nombre(),
                listaJugadores.get(0), listaJugadores.get(2));
            equipoB = Equipo.pareja("eB-" + id,
                listaJugadores.get(1).nombre() + " & " + listaJugadores.get(3).nombre(),
                listaJugadores.get(1), listaJugadores.get(3));
        } else {
            throw new IllegalArgumentException("Cantidad de jugadores no soportada: " + cantidad);
        }

        Partida partida = new Partida(id, request.modalidad(), equipoA, equipoB, 
            request.formato(), mesa, Instant.now());
        partida.iniciar();
        return partida;
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