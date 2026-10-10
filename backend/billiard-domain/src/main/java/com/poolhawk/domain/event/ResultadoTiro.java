package com.poolhawk.domain.event;

import java.util.Objects;

/**
 * Resultado de evaluar un tiro según las reglas de la modalidad activa.
 *
 * <p>Un {@link TiroEvent} representa los hechos observados (qué pasó
 * físicamente). Este objeto representa el veredicto del motor de reglas:
 * si fue falta, cuántos puntos se ganaron, si cambia el turno, etc.</p>
 *
 * <p>Esta separación entre hechos y veredicto es fundamental en la
 * arquitectura de PoolHawk: el agente de captura (Python) reporta hechos;
 * el motor de reglas (Java) decide el resultado.</p>
 *
 * <p>Esta clase es inmutable: una vez evaluado el tiro, el resultado no
 * cambia.</p>
 *
 * @param esFalta       {@code true} si el tiro constituyó una falta
 * @param motivo        motivo de la falta, o {@code null} si no hubo falta
 * @param puntos        puntos ganados por el jugador (0 si fue falta)
 * @param cambiaTurno   {@code true} si el turno debe pasar al siguiente jugador
 * @param esVictoria    {@code true} si este tiro dio la victoria al jugador
 * @param descripcion   descripción textual del resultado para mostrar al usuario
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record ResultadoTiro(
    boolean esFalta,
    FaltaMotivo motivo,
    int puntos,
    boolean cambiaTurno,
    boolean esVictoria,
    String descripcion
) {

    /**
     * Constructor compacto con validaciones de coherencia.
     *
     * <p>Las reglas de coherencia son:</p>
     * <ul>
     *   <li>Si hay falta, debe haber motivo.</li>
     *   <li>Si no hay falta, no debe haber motivo.</li>
     *   <li>Si hay falta, los puntos deben ser cero.</li>
     *   <li>Los puntos nunca pueden ser negativos.</li>
     *   <li>La descripción no puede ser nula ni vacía.</li>
     * </ul>
     *
     * @throws NullPointerException     si la descripción es nula
     * @throws IllegalArgumentException si hay incoherencias entre los campos
     */
    public ResultadoTiro {
        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException(
                "La descripción del resultado no puede ser nula ni vacía"
            );
        }

        if (esFalta && motivo == null) {
            throw new IllegalArgumentException(
                "Si el tiro es falta, debe especificarse el motivo"
            );
        }

        if (!esFalta && motivo != null) {
            throw new IllegalArgumentException(
                "Si el tiro no es falta, el motivo debe ser nulo. " +
                "Motivo recibido: " + motivo
            );
        }

        if (esFalta && puntos != 0) {
            throw new IllegalArgumentException(
                "Si el tiro es falta, los puntos deben ser 0. " +
                "Puntos recibidos: " + puntos
            );
        }

        if (puntos < 0) {
            throw new IllegalArgumentException(
                "Los puntos no pueden ser negativos. Valor recibido: " + puntos
            );
        }
    }

    // ═══════════════════════════════════════════════════════════
    // FÁBRICAS ESTÁTICAS
    // ═══════════════════════════════════════════════════════════

    /**
     * Crea un resultado de tiro legal sin puntos (el jugador no embolsó).
     *
     * @return resultado legal, 0 puntos, sin cambio de turno
     */
    public static ResultadoTiro tiroLegalSinPuntos() {
        return new ResultadoTiro(
            false,
            null,
            0,
            false,
            false,
            "Tiro legal sin embolsar bolas"
        );
    }

    /**
     * Crea un resultado de tiro legal que embolsó bolas.
     *
     * @param puntos puntos ganados (mayor o igual a 1)
     * @return resultado legal con puntos
     * @throws IllegalArgumentException si los puntos son menores a 1
     */
    public static ResultadoTiro tiroLegalConPuntos(int puntos) {
        if (puntos < 1) {
            throw new IllegalArgumentException(
                "Un tiro legal con puntos debe sumar al menos 1. " +
                "Valor recibido: " + puntos
            );
        }
        return new ResultadoTiro(
            false,
            null,
            puntos,
            false,
            false,
            "Tiro legal, " + puntos + " punto(s)"
        );
    }

    /**
     * Crea un resultado de falta con su motivo.
     *
     * @param motivo motivo de la falta
     * @return resultado de falta con cambio de turno
     * @throws NullPointerException si el motivo es nulo
     */
    public static ResultadoTiro falta(FaltaMotivo motivo) {
        Objects.requireNonNull(motivo, "El motivo de la falta no puede ser nulo");
        return new ResultadoTiro(
            true,
            motivo,
            0,
            true,
            false,
            "Falta: " + motivo.getDescripcion()
        );
    }

    /**
     * Crea un resultado de victoria tras un tiro legal.
     *
     * @param puntos puntos ganados (puede ser 0 si la victoria no suma puntos)
     * @return resultado de victoria
     * @throws IllegalArgumentException si los puntos son negativos
     */
    public static ResultadoTiro victoria(int puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException(
                "Los puntos de una victoria no pueden ser negativos. " +
                "Valor recibido: " + puntos
            );
        }
        return new ResultadoTiro(
            false,
            null,
            puntos,
            true,
            true,
            "¡Victoria! Se ganó la partida"
        );
    }

    // ═══════════════════════════════════════════════════════════
    // CONSULTAS
    // ═══════════════════════════════════════════════════════════

    /**
     * Verifica si el tiro fue exitoso (legal y sumó puntos).
     *
     * @return {@code true} si no fue falta y sumó al menos 1 punto
     */
    public boolean fueExitoso() {
        return !esFalta && puntos > 0;
    }

    /**
     * Verifica si el tiro fue legal pero no sumó puntos.
     *
     * @return {@code true} si no fue falta y sumó 0 puntos
     */
    public boolean fueNeutro() {
        return !esFalta && puntos == 0;
    }
}