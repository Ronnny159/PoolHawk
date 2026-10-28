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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reglas de la modalidad Por Puntos en parejas.
 *
 * <p>En esta modalidad, dos equipos de dos jugadores acumulan puntos por
 * cada bola embolsada. Cada bola vale 1 punto. El primer equipo cuya suma
 * de puntajes alcance el objetivo gana la partida.</p>
 *
 * <p><b>Diferencias con Bola 8:</b></p>
 * <ul>
 *   <li>No hay asignación de grupos. Cualquier bola vale 1 punto.</li>
 *   <li>Los puntos se acumulan; no hay victoria por embolsar una bola
 *       específica.</li>
 *   <li>La partida termina cuando un jugador alcanza el puntaje objetivo.</li>
 *   <li>Las faltas solo cambian turno, sin penalización de puntos.</li>
 * </ul>
 *
 * <p>Orden de validación:</p>
 * <ol>
 *   <li>Si la blanca no tocó ninguna bola → falta {@code NO_TOCAR_BOLA}.</li>
 *   <li>Si la blanca fue embolsada → falta {@code BLANCA_EMBOLSADA}.</li>
 *   <li>Si el primer contacto es la bola 8 sin ser la última bola en mesa
 *       → falta {@code BOLA_EQUIVOCADA}.</li>
 *   <li>Se suman los puntos de las bolas embolsadas.</li>
 *   <li>Si el puntaje del jugador alcanza el objetivo → <b>victoria</b>.</li>
 *   <li>Si embolsó alguna bola → sigue tirando.</li>
 *   <li>Si no embolsó → cambia turno.</li>
 * </ol>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class ReglasPorPuntos extends AbstractReglasBillar {

    /**
     * Crea una instancia de reglas de Por Puntos.
     *
     * @param config configuración de reglas, incluye el puntaje objetivo
     */
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

        // 1. ¿Tocó alguna bola?
        if (!tiro.tocoAlgunaBola()) {
            return ResultadoTiro.falta(FaltaMotivo.NO_TOCAR_BOLA);
        }

        // 2. ¿Embolsó la blanca?
        if (tiro.embolsoLaBlanca()) {
            return ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);
        }

        // 3. ¿Tocó primero la bola 8 sin ser la última?
        if (esContactoIlegalConOcho(estado, tiro)) {
            return ResultadoTiro.falta(FaltaMotivo.BOLA_EQUIVOCADA);
        }

        // 4. Calcular puntos del tiro (1 punto por bola embolsada, excluyendo la blanca)
        int puntosDelTiro = calcularPuntos(tiro);

        // 5. ¿Alcanzó el puntaje objetivo?
        int puntajeActual = estado.puntajeDe(jugador);
        int puntajeNuevo = puntajeActual + puntosDelTiro;
        if (puntajeNuevo >= config.puntajeObjetivo()) {
            return ResultadoTiro.victoria(puntosDelTiro);
        }

        // 6 y 7. ¿Embolsó alguna bola?
        if (puntosDelTiro > 0) {
            return ResultadoTiro.tiroLegalConPuntos(puntosDelTiro);
        }
        return ResultadoTiro.tiroLegalSinPuntos(true);
    }

    /**
     * Verifica si el tiro tocó primero la bola 8 sin que sea la última
     * bola en la mesa.
     *
     * <p>En Por Puntos, la bola 8 es la última bola que se puede golpear
     * legalmente. Si se golpea antes de tiempo, es falta.</p>
     *
     * @param estado estado de la partida
     * @param tiro   evento del tiro
     * @return {@code true} si el contacto es ilegal
     */
    private boolean esContactoIlegalConOcho(MatchState estado, TiroEvent tiro) {
        Bola tocada = tiro.primeraBolaTocada();
        if (tocada == null || !tocada.esOcho()) {
            return false;
        }

        // Es la 8: verificar si quedan otras bolas de grupo en la mesa
        int bolasDeGrupoEnMesa = contarBolasDeGrupoEnMesa(estado);
        return bolasDeGrupoEnMesa > 0;
    }

    /**
     * Cuenta cuántas bolas de grupo (lisas y rayadas) quedan en la mesa.
     *
     * @param estado estado de la partida
     * @return cantidad de bolas de grupo en juego
     */
    private int contarBolasDeGrupoEnMesa(MatchState estado) {
        return (int) estado.getMesa().getBolasEnMesa().stream()
            .filter(Bola::esDeGrupo)
            .count();
    }

    /**
     * Calcula los puntos ganados en un tiro: 1 punto por cada bola
     * embolsada, sin contar la blanca.
     *
     * @param tiro evento del tiro
     * @return puntos ganados
     */
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
        Optional<Map.Entry<Jugador, Integer>> ganador = estado.getPuntajes()
            .entrySet().stream()
            .filter(e -> e.getValue() >= config.puntajeObjetivo())
            .findFirst();
        return ganador.map(Map.Entry::getKey).orElse(null);
    }
}