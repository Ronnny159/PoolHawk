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

import java.util.List;
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

        if (!tiro.tocoAlgunaBola()) {
            return ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
        }
        if (tiro.embolsoLaBlanca()) {
            return ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);
        }
        if (!esPrimerContactoLegal(estado, tiro, jugador, grupoJugador)) {
            return ResultadoTiro.falta(FaltaMotivo.BOLA_EQUIVOCADA);
        }
        if (tiro.embolsoLaOcho()) {
            if (!grupoLimpio(estado, jugador)) {
                return ResultadoTiro.falta(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO);
            }
            return ResultadoTiro.victoria(0);
        }
        if (!grupoJugador.estaAsignado()) {
            asignarGrupoSiCorresponde(estado, tiro);
        }
        if (tiro.embolsoAlgunaBola()) {
            return construirResultadoLegalSigueTirando(tiro);
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
            return tocada.esOcho(); // Si el grupo está limpio, SOLO la 8 es legal
        }

        return tocada.tipo() == grupoJugador.getTipoBolaAsociado();
    }

    private boolean grupoLimpio(MatchState estado, Jugador jugador) {
        Grupo grupo = estado.grupoDe(jugador);
        if (!grupo.estaAsignado()) return false;
        return !estado.quedanBolasDeGrupo(grupo);
    }

    private void asignarGrupoSiCorresponde(MatchState estado, TiroEvent tiro) {
        List<Bola> embolsadas = tiro.bolasEmbolsadas();
        if (embolsadas.isEmpty()) return;

        Grupo grupoDetectado = null;
        for (Bola bola : embolsadas) {
            if (bola.esLisa()) { grupoDetectado = Grupo.LISAS; break; }
            if (bola.esRayada()) { grupoDetectado = Grupo.RAYADAS; break; }
        }

        if (grupoDetectado == null) return;

        Jugador jugador = tiro.jugador();
        Jugador companero = estado.getCompanero(jugador);

        if (companero.equals(jugador)) {
            // Bola 8 individual: el oponente es el siguiente en el turno
            Jugador oponente = estado.getTurnos().turnoSiguiente();
            estado.asignarGrupo(jugador, grupoDetectado);
            estado.asignarGrupo(oponente, grupoDetectado.opuesto());
        } else {
            // Parejas: ambos reciben el mismo grupo
            if (!estado.grupoDe(jugador).estaAsignado() && !estado.grupoDe(companero).estaAsignado()) {
                estado.asignarGrupo(jugador, grupoDetectado);
                estado.asignarGrupo(companero, grupoDetectado);
            }
        }
    }

    private ResultadoTiro construirResultadoLegalSigueTirando(TiroEvent tiro) {
        return new ResultadoTiro(false, null, 0, false, false, "Tiro legal, embolsó " + tiro.cantidadBolasEmbolsadas() + " bola(s)");
    }

    @Override
    public boolean partidaTerminada(MatchState estado) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        return estado.estaTerminada();
    }

    @Override
    public Jugador determinarGanador(MatchState estado) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        return estado.getGanador();
    }
}