package com.poolhawk.rules;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.match.MatchState;

import java.util.Objects;

/**
 * Clase base que implementa el patrón <b>Template Method</b> para las
 * reglas del billar.
 *
 * <p>Define el esqueleto del algoritmo de evaluación de un tiro. Las
 * subclases concretas ({@code ReglasBola8}, {@code ReglasPorPuntos})
 * implementan los pasos específicos de cada modalidad.</p>
 *
 * <p>El esqueleto del algoritmo es:</p>
 * <ol>
 *   <li>Verificar precondiciones (estado y tiro no nulos).</li>
 *   <li>Validar el shot clock si está configurado.</li>
 *   <li>Evaluar la legalidad del tiro (paso variable).</li>
 *   <li>Calcular el resultado (paso variable).</li>
 * </ol>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public abstract class AbstractReglasBillar implements ReglasBillar {

    protected final ReglasConfig config;

    /**
     * Crea una instancia de reglas con la configuración indicada.
     *
     * @param config configuración de reglas opcionales
     * @throws NullPointerException si la configuración es nula
     */
    protected AbstractReglasBillar(ReglasConfig config) {
        this.config = Objects.requireNonNull(
            config, "La configuración de reglas no puede ser nula"
        );
    }

    /**
     * Evalúa un tiro aplicando el esqueleto del algoritmo.
     *
     * <p>Este método es {@code final} para garantizar que todas las
     * modalidades sigan el mismo flujo. Las diferencias se implementan
     * en los métodos abstractos.</p>
     *
     * @param estado estado actual de la partida
     * @param tiro   evento del tiro a evaluar
     * @return veredicto del tiro
     */
    @Override
    public final ResultadoTiro evaluar(MatchState estado, TiroEvent tiro) {
        Objects.requireNonNull(estado, "El estado de la partida no puede ser nulo");
        Objects.requireNonNull(tiro, "El tiro no puede ser nulo");

        // 1. Validar shot clock si está configurado
        if (config.tieneShotClock() && excedeShotClock(tiro)) {
            return construirResultadoPorShotClock();
        }

        // 2. Evaluar el tiro (paso variable)
        return evaluarTiroLegal(estado, tiro);
    }

    /**
     * Verifica si el tiro excede el tiempo permitido por el shot clock.
     *
     * @param tiro evento del tiro
     * @return {@code true} si excede el tiempo
     */
    protected boolean excedeShotClock(TiroEvent tiro) {
        return tiro.duracion().compareTo(config.shotClock()) > 0;
    }

    /**
     * Construye el resultado cuando se excede el shot clock.
     *
     * @return resultado de falta por tiempo excedido
     */
    protected ResultadoTiro construirResultadoPorShotClock() {
        return ResultadoTiro.falta(
            com.poolhawk.domain.event.FaltaMotivo.TIEMPO_EXCEDIDO
        );
    }

    /**
     * Evalúa un tiro concreto según las reglas de la modalidad.
     *
     * <p>Este es el paso variable del Template Method. Cada modalidad
     * implementa su propia lógica.</p>
     *
     * @param estado estado actual de la partida
     * @param tiro   evento del tiro
     * @return veredicto del tiro
     */
    protected abstract ResultadoTiro evaluarTiroLegal(MatchState estado, TiroEvent tiro);

    /**
     * Retorna la configuración de reglas activa.
     *
     * @return configuración de reglas
     */
    public ReglasConfig getConfig() {
        return config;
    }
}