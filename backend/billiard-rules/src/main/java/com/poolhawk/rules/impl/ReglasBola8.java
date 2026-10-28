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
 * Reglas de la modalidad Bola 8 individual.
 *
 * <p>Implementa las reglas estándar WPA de Bola 8 para dos jugadores.
 * El motor sigue este orden de validación, donde las reglas más graves
 * tienen prioridad:</p>
 *
 * <ol>
 *   <li>Si la blanca no tocó ninguna bola → falta {@code NO_TOCAR_BOLA}.</li>
 *   <li>Si la blanca fue embolsada → falta {@code BLANCA_EMBOLSADA}.</li>
 *   <li>Si la bola 8 fue embolsada:
 *     <ul>
 *       <li>Si el jugador no limpió su grupo → falta {@code BOLA_8_ANTES_DE_TIEMPO}.</li>
 *       <li>Si limpió su grupo → <b>victoria</b>.</li>
 *     </ul>
 *   </li>
 *   <li>Si el primer contacto es una bola del grupo equivocado → falta
 *       {@code BOLA_EQUIVOCADA}.</li>
 *   <li>Si no tenía grupo asignado y embolsó una bola de grupo → asignar
 *       grupos al jugador y al oponente.</li>
 *   <li>En otro caso → tiro legal.
 *     <ul>
 *       <li>Si embolsó → sigue tirando.</li>
 *       <li>Si no embolsó → cambia turno.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><b>Nota sobre prioridad de reglas:</b> embolsar la bola 8 antes de
 * tiempo es una falta más grave que tocar bola equivocada. Por eso la
 * validación de la bola 8 se hace <i>antes</i> que la validación del
 * primer contacto. Este es un ejemplo real de conflicto entre reglas
 * donde el dominio define qué regla prevalece.</p>
 *
 * <p>En Bola 8 no se otorgan puntos. Solo se gana o se pierde.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class ReglasBola8 extends AbstractReglasBillar {

    /**
     * Crea una instancia de reglas de Bola 8.
     *
     * @param config configuración de reglas opcionales
     */
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

        // 2. ¿Embolsó la blanca?
        if (tiro.embolsoLaBlanca()) {
            return ResultadoTiro.falta(FaltaMotivo.BLANCA_EMBOLSADA);
        }

        // 3. ¿Embolsó la bola 8?
        //    (antes de validar primer contacto, porque es más grave)
        if (tiro.embolsoLaOcho()) {
            if (!grupoLimpio(estado, jugador)) {
                return ResultadoTiro.falta(FaltaMotivo.BOLA_8_ANTES_DE_TIEMPO);
            }
            return ResultadoTiro.victoria(0);
        }

        // 4. ¿Tocó primero una bola legal?
        if (!esPrimerContactoLegal(tiro, grupoJugador)) {
            return ResultadoTiro.falta(FaltaMotivo.BOLA_EQUIVOCADA);
        }

        // 5. Asignación de grupo si aún no tenía
        if (!grupoJugador.estaAsignado()) {
            asignarGrupoSiCorresponde(estado, tiro);
        }

        // 6. Tiro legal
        if (tiro.embolsoAlgunaBola()) {
            return construirResultadoLegalSigueTirando(tiro);
        }
        return ResultadoTiro.tiroLegalSinPuntos(true);
    }

    /**
     * Verifica si el primer contacto del tiro es legal para el jugador.
     *
     * <ul>
     *   <li>Si el jugador no tiene grupo asignado, puede tocar cualquier
     *       bola excepto la 8 o la blanca.</li>
     *   <li>Si ya tiene grupo, debe tocar primero una bola de su grupo.</li>
     * </ul>
     *
     * @param tiro         evento del tiro
     * @param grupoJugador grupo del jugador
     * @return {@code true} si el primer contacto es legal
     */
    private boolean esPrimerContactoLegal(TiroEvent tiro, Grupo grupoJugador) {
        Bola tocada = tiro.primeraBolaTocada();
        if (tocada == null) {
            return false;
        }

        if (!grupoJugador.estaAsignado()) {
            // Sin grupo asignado: cualquier bola excepto la 8 y la blanca
            return !tocada.esOcho() && !tocada.esBlanca();
        }

        // Con grupo asignado: debe ser del tipo de su grupo
        return tocada.tipo() == grupoJugador.getTipoBolaAsociado();
    }

    /**
     * Verifica si el jugador ya limpió su grupo (no le quedan bolas
     * de su tipo en la mesa).
     *
     * @param estado  estado de la partida
     * @param jugador jugador a verificar
     * @return {@code true} si no le quedan bolas de su grupo
     */
    private boolean grupoLimpio(MatchState estado, Jugador jugador) {
        Grupo grupo = estado.grupoDe(jugador);
        if (!grupo.estaAsignado()) {
            return false;
        }
        return !estado.quedanBolasDeGrupo(grupo);
    }

    /**
     * Asigna grupo al jugador si embolsó al menos una bola de grupo en
     * el tiro y aún no tenía grupo asignado.
     *
     * <p>El oponente recibe el grupo contrario automáticamente.</p>
     *
     * @param estado estado de la partida
     * @param tiro   evento del tiro
     */
    private void asignarGrupoSiCorresponde(MatchState estado, TiroEvent tiro) {
        List<Bola> embolsadas = tiro.bolasEmbolsadas();
        if (embolsadas.isEmpty()) {
            return;
        }

        // Buscar la primera bola de grupo embolsada
        Grupo grupoDetectado = null;
        for (Bola bola : embolsadas) {
            if (bola.esLisa()) {
                grupoDetectado = Grupo.LISAS;
                break;
            }
            if (bola.esRayada()) {
                grupoDetectado = Grupo.RAYADAS;
                break;
            }
        }

        if (grupoDetectado == null) {
            return;
        }

        // Asignar al jugador actual y al oponente el grupo contrario
        Jugador jugador = tiro.jugador();
        Jugador oponente = estado.getTurnos().turnoSiguiente();

        estado.asignarGrupo(jugador, grupoDetectado);
        estado.asignarGrupo(oponente, grupoDetectado.opuesto());
    }

    /**
     * Construye un resultado legal donde el jugador sigue tirando
     * (porque embolsó al menos una bola).
     *
     * @param tiro evento del tiro
     * @return resultado legal sin cambio de turno
     */
    private ResultadoTiro construirResultadoLegalSigueTirando(TiroEvent tiro) {
        return new ResultadoTiro(
            false,
            null,
            0,
            false,
            false,
            "Tiro legal, embolsó " + tiro.cantidadBolasEmbolsadas() + " bola(s)"
        );
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