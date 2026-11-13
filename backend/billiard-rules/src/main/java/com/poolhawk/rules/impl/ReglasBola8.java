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

/**
 * Reglas de la modalidad Bola 8.
 */
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

        // 1. ¿Tocó alguna bola?
        if (!tiro.tocoAlgunaBola()) {
            return ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
        }

        // 2. ¿Embolsó la blanca y la 8 simultáneamente? -> DERROTA DIRECTA
        if (tiro.embolsoLaBlanca() && tiro.embolsoLaOcho()) {
            return ResultadoTiro.derrota(FaltaMotivo.BLANCA_Y_OCHO_EMBOLSADAS);
        }

        // 3. ¿Embolsó solo la blanca? -> Falta ordinaria
        if (tiro.embolsoLaBlanca()) {
            return ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);
        }

        // 4. ¿Embolsó la 8?
        if (tiro.embolsoLaOcho()) {
            if (!grupoLimpio(estado, jugador)) {
                return ResultadoTiro.derrota(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO);
            }
            // Si el grupo está limpio, debe ser un tiro legal.
            if (!esPrimerContactoLegal(estado, tiro, jugador, grupoJugador)) {
                return ResultadoTiro.derrota(FaltaMotivo.BOLA_8_CON_CONTACTO_ILEGAL);
            }
            return ResultadoTiro.victoria(0);
        }

        // 5. ¿Tocó primero una bola legal? (validación de primer contacto)
        if (!esPrimerContactoLegal(estado, tiro, jugador, grupoJugador)) {
            return ResultadoTiro.falta(FaltaMotivo.BOLA_EQUIVOCADA);
        }

        // 6. Asignación de grupo
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