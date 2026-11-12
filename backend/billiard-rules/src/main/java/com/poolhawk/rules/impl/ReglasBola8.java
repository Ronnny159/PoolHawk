package com.poolhawk.rules.impl;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.Grupo;
import com.poolhawk.domain.match.MatchState;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.rules.AbstractReglasBillar;
import com.poolhawk.rules.ReglasConfig;

import java.util.Objects;

public class ReglasBola8 extends AbstractReglasBillar {

    public ReglasBola8(ReglasConfig config) {
        super(config);
    }

    @Override
    public String getNombreModalidad() {
        return "Bola 8";
    }

    @Override
    protected ResultadoTiro evaluarTiroLegal(MatchState estado, TiroEvent tiro) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        Objects.requireNonNull(tiro, "El tiro no puede ser nulo");

        Jugador jugador = tiro.jugador();
        Grupo grupoJugador = estado.grupoDe(jugador);

        if (!tiro.tocoAlgunaBola()) {
            return ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
        }
        if (tiro.embolsoLaBlanca()) {
            return ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);
        }

        // PRIORIDAD 1: Bola 8 prematura o victoria
        if (tiro.embolsoLaOcho()) {
            if (!grupoLimpio(estado, jugador)) {
                return ResultadoTiro.derrota(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO);
            }
            return ResultadoTiro.victoria(0);
        }

        // PRIORIDAD 2: Primer contacto legal
        if (!esPrimerContactoLegal(estado, tiro, jugador, grupoJugador)) {
            return ResultadoTiro.falta(FaltaMotivo.BOLA_EQUIVOCADA);
        }

        // PRIORIDAD 3: Asignación de grupo (sin mutar el estado)
        Grupo grupoDetectado = null;
        if (!grupoJugador.estaAsignado() && tiro.embolsoAlgunaBola()) {
            for (Bola bola : tiro.bolasEmbolsadas()) {
                if (bola.esLisa()) { grupoDetectado = Grupo.LISAS; break; }
                if (bola.esRayada()) { grupoDetectado = Grupo.RAYADAS; break; }
            }
        }

        if (tiro.embolsoAlgunaBola()) {
            return ResultadoTiro.tiroLegalConPuntos(0, grupoDetectado);
        }
        return ResultadoTiro.tiroLegalSinPuntos(true);
    }

    private boolean esPrimerContactoLegal(MatchState estado, TiroEvent tiro, Jugador jugador, Grupo grupoJugador) {
        Bola tocada = tiro.primeraBolaTocada();
        if (tocada == null || tocada.esBlanca()) return false;

        if (!grupoJugador.estaAsignado()) {
            return !tocada.esOcho();
        }
        if (grupoLimpio(estado, jugador)) {
            return tocada.esOcho(); 
        }
        return tocada.tipo() == grupoJugador.getTipoBolaAsociado();
    }

    private boolean grupoLimpio(MatchState estado, Jugador jugador) {
        Grupo grupo = estado.grupoDe(jugador);
        if (!grupo.estaAsignado()) return false;
        return !estado.quedanBolasDeGrupo(grupo);
    }

    @Override
    public boolean partidaTerminada(MatchState estado) {
        return estado.estaTerminada();
    }

    @Override
    public Jugador determinarGanador(MatchState estado) {
        return estado.getGanador();
    }
}