package com.poolhawk.rules.impl;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.MatchState;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.rules.AbstractReglasBillar;
import com.poolhawk.rules.ReglasConfig;

import java.util.List;
import java.util.Objects;

public class ReglasPorPuntos extends AbstractReglasBillar {

    public ReglasPorPuntos(ReglasConfig config) {
        super(config);
    }

    @Override
    public String getNombreModalidad() {
        return "Por Puntos";
    }

    @Override
    protected ResultadoTiro evaluarTiroLegal(MatchState estado, TiroEvent tiro) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        Objects.requireNonNull(tiro, "El tiro no puede ser nulo");

        Jugador jugador = tiro.jugador();

        if (!tiro.tocoAlgunaBola()) {
            return ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
        }
        if (tiro.embolsoLaBlanca()) {
            return ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);
        }
        if (esContactoIlegalConOcho(estado, tiro)) {
            return ResultadoTiro.falta(FaltaMotivo.BOLA_EQUIVOCADA);
        }

        int puntosDelTiro = calcularPuntos(tiro);
        int puntajeEquipo = calcularPuntajeEquipo(estado, jugador);
        int puntajeNuevo = puntajeEquipo + puntosDelTiro;

        if (puntajeNuevo >= config.puntajeObjetivo()) {
            return ResultadoTiro.victoria(puntosDelTiro);
        }

        if (puntosDelTiro > 0) {
            return ResultadoTiro.tiroLegalConPuntos(puntosDelTiro);
        }
        return ResultadoTiro.tiroLegalSinPuntos(true);
    }

    private int calcularPuntajeEquipo(MatchState estado, Jugador jugador) {
        Jugador companero = estado.getCompanero(jugador);
        if (companero.equals(jugador)) {
            return estado.puntajeDe(jugador);
        }
        return estado.puntajeDe(jugador) + estado.puntajeDe(companero);
    }

    private boolean esContactoIlegalConOcho(MatchState estado, TiroEvent tiro) {
        Bola tocada = tiro.primeraBolaTocada();
        if (tocada == null || !tocada.esOcho()) return false;
        return contarBolasDeGrupoEnMesa(estado) > 0;
    }

    private int contarBolasDeGrupoEnMesa(MatchState estado) {
        return (int) estado.getMesa().getBolasEnMesa().stream()
            .filter(Bola::esDeGrupo)
            .count();
    }

    private int calcularPuntos(TiroEvent tiro) {
        List<Bola> embolsadas = tiro.bolasEmbolsadas();
        return (int) embolsadas.stream()
            .filter(b -> !b.esBlanca())
            .count();
    }

    @Override
    public boolean partidaTerminada(MatchState estado) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        return estado.getPuntajes().keySet().stream()
            .anyMatch(j -> calcularPuntajeEquipo(estado, j) >= config.puntajeObjetivo());
    }

    @Override
    public Jugador determinarGanador(MatchState estado) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        return estado.getPuntajes().keySet().stream()
            .filter(j -> calcularPuntajeEquipo(estado, j) >= config.puntajeObjetivo())
            .findFirst()
            .orElse(null);
    }
}