package com.poolhawk.rules.impl;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.Grupo;
import com.poolhawk.domain.match.MatchState;
import com.poolhawk.domain.match.TipoModalidad;
import com.poolhawk.domain.match.Turnos;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Coordenada;
import com.poolhawk.domain.table.Mesa;
import com.poolhawk.domain.table.TroneraNombre;
import com.poolhawk.rules.ReglasConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReglasBola8 - reglas de Bola 8")
class ReglasBola8Test {

    private static final String PARTIDA_ID = "p1";
    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");
    private static final Instant FECHA = Instant.parse("2026-01-01T10:00:00Z");

    private ReglasBola8 reglas;
    private Mesa mesa;
    private Turnos<Jugador> turnos;
    private MatchState estado;

    @BeforeEach
    void setUp() {
        reglas = new ReglasBola8(ReglasConfig.porDefecto());
        mesa = new Mesa();
        mesa.agregarBola(Bola.crear(0, new Coordenada(50, 50)));
        for (int i = 1; i <= 7; i++) {
            mesa.agregarBola(Bola.crear(i, new Coordenada(100 + i * 5, 100)));
        }
        mesa.agregarBola(Bola.crear(8, new Coordenada(200, 100)));
        for (int i = 9; i <= 15; i++) {
            mesa.agregarBola(Bola.crear(i, new Coordenada(100 + i * 5, 120)));
        }

        Equipo eqA = Equipo.individual("eA", ANA.nombre(), ANA);
        Equipo eqB = Equipo.individual("eB", BETO.nombre(), BETO);
        turnos = new Turnos<>(List.of(ANA, BETO));
        estado = new MatchState(PARTIDA_ID, TipoModalidad.BOLA_8, eqA, eqB, turnos, mesa, FECHA);
        estado.iniciar();
    }

    private Bola bola(int n) {
        return Bola.crear(n, new Coordenada(100 + n, 100));
    }

    private TiroEvent tiro(Jugador jugador, Bola primerContacto,
                           List<Bola> embolsadas, List<TroneraNombre> troneras) {
        return new TiroEvent(
            "evt-" + System.nanoTime(),
            PARTIDA_ID,
            jugador,
            primerContacto,
            embolsadas,
            troneras,
            Instant.now(),
            Duration.ofSeconds(5)
        );
    }

    private TiroEvent tiroSimple(Jugador jugador, Bola primerContacto) {
        return tiro(jugador, primerContacto, List.of(), List.of());
    }

    private TiroEvent tiroConEmbolsada(Jugador jugador, Bola primerContacto,
                                       Bola embolsada) {
        return tiro(jugador, primerContacto, List.of(embolsada),
            List.of(TroneraNombre.TOP_LEFT));
    }

    // ... (El resto de las pruebas @Nested se mantienen EXACTAMENTE igual) ...
    // Puedes copiar el resto del archivo tal cual lo tenías.
}