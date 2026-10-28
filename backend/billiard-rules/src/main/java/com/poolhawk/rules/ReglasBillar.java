package com.poolhawk.rules;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.MatchState;
import com.poolhawk.domain.player.Jugador;

/**
 * Contrato que deben implementar todas las modalidades de reglas.
 *
 * <p>Esta interfaz define las operaciones comunes a cualquier modalidad
 * de billar: evaluar un tiro, decidir si la partida terminó y determinar
 * el ganador. Cada modalidad (Bola 8, Por Puntos, futuras) implementa su
 * propia versión.</p>
 *
 * <p>Sigue el patrón <b>Strategy</b>: el motor de reglas puede cambiar
 * de implementación según la modalidad activa sin que el código cliente
 * se modifique.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public interface ReglasBillar {

    /**
     * Evalúa un tiro según las reglas de la modalidad.
     *
     * <p>Este método NO modifica el estado de la partida. Solo decide qué
     * habría que hacer. La aplicación del resultado es responsabilidad
     * del llamador (normalmente el {@code MatchService}).</p>
     *
     * @param estado estado actual de la partida
     * @param tiro   evento del tiro a evaluar
     * @return veredicto del tiro
     * @throws NullPointerException si algún parámetro es nulo
     */
    ResultadoTiro evaluar(MatchState estado, TiroEvent tiro);

    /**
     * Verifica si la partida ha terminado según las reglas de la modalidad.
     *
     * @param estado estado actual de la partida
     * @return {@code true} si la partida terminó
     * @throws NullPointerException si el estado es nulo
     */
    boolean partidaTerminada(MatchState estado);

    /**
     * Determina el ganador de la partida, si ya terminó.
     *
     * @param estado estado actual de la partida
     * @return ganador de la partida, o {@code null} si no ha terminado
     * @throws NullPointerException si el estado es nulo
     */
    Jugador determinarGanador(MatchState estado);

    /**
     * Retorna el nombre visible de la modalidad.
     *
     * @return nombre de la modalidad en español
     */
    String getNombreModalidad();
}