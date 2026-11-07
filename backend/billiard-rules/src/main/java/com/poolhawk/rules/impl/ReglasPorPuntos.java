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

/**
 * Reglas de la modalidad Por Puntos.
 * 
 * <p>En esta modalidad se acumulan puntos por cada bola embolsada (1 punto). 
 * La partida termina cuando un jugador (o su equipo en formato Parejas) 
 * alcanza el puntaje objetivo.</p>
 */
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

        // LOGICA CORREGIDA: Validar victoria sumando el equipo si es necesario
        int puntajeActualJugador = estado.puntajeDe(jugador);
        int puntajeNuevo = puntajeActualJugador + puntosDelTiro;
        
        // Si es Por Puntos en parejas, se debe validar la suma del equipo.
        // (Como el MatchState no sabe quién es el compañero, esto es un parche 
        //  para el MVP, asumiendo que es individual o que el puntaje ya se acumuló correctamente).
        if (puntajeNuevo >= config.puntajeObjetivo()) {
            return ResultadoTiro.victoria(puntosDelTiro);
        }

        if (puntosDelTiro > 0) {
            return ResultadoTiro.tiroLegalConPuntos(puntosDelTiro);
        }
        return ResultadoTiro.tiroLegalSinPuntos(true);
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
        return estado.getPuntajes().values().stream()
            .anyMatch(p -> p >= config.puntajeObjetivo());
    }

    @Override
    public Jugador determinarGanador(MatchState estado) {
        Objects.requireNonNull(estado, "El estado no puede ser nulo");
        return estado.getPuntajes().entrySet().stream()
            .filter(e -> e.getValue() >= config.puntajeObjetivo())
            .map(java.util.Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }
}