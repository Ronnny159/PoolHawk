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

        // LOGICA CORREGIDA: Para validar la victoria en Parejas, sumamos los puntos del equipo.
        // Asumimos que los equipos son A: [0, 2] y B: [1, 3] en la lista de turnos.
        // Para ser más precisos, calculamos la suma de los puntos de los jugadores que están
        // en el mismo equipo que el jugador actual.
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

    /**
     * Calcula el puntaje total del equipo del jugador actual.
     * En Parejas, el equipo está formado por el jugador y su compañero.
     * Usamos el orden de los turnos para inferir los equipos:
     * - Equipo A: índices 0 y 2
     * - Equipo B: índices 1 y 3
     */
    private int calcularPuntajeEquipo(MatchState estado, Jugador jugador) {
        List<Jugador> jugadores = estado.getTurnos().jugadores();
        int miIndice = jugadores.indexOf(jugador);
        
        if (miIndice == -1) return 0;
        
        // En parejas, si soy 0 mi compañero es 2. Si soy 1, mi compañero es 3.
        int indiceCompanero = (miIndice % 2 == 0) ? miIndice + 2 : miIndice - 1;
        
        // Validación para no salirnos de los límites (por si es individual)
        if (indiceCompanero >= jugadores.size()) {
            // Si no hay compañero (individual), solo sumamos el puntaje individual
            return estado.puntajeDe(jugador);
        }
        
        Jugador companero = jugadores.get(indiceCompanero);
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
        // Para verificar si terminó, comprobamos si algún equipo alcanzó el objetivo
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