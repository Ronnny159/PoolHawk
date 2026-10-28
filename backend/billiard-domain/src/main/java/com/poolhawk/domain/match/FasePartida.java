package com.poolhawk.domain.match;

import java.util.Set;

/**
 * Fases del ciclo de vida de una partida de billar.
 *
 * <p>Las transiciones válidas son:</p>
 * <pre>
 *   CREADA ──▶ EN_CURSO ──▶ FINALIZADA
 *                │  ▲
 *                ▼  │
 *             PAUSADA
 *                │
 *                ▼
 *           ABANDONADA
 * </pre>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public enum FasePartida {

    /** La partida fue creada pero aún no ha comenzado. */
    CREADA("Creada"),

    /** La partida está en curso. */
    EN_CURSO("En curso"),

    /** La partida está pausada temporalmente. */
    PAUSADA("Pausada"),

    /** La partida terminó con un ganador. */
    FINALIZADA("Finalizada"),

    /** La partida fue abandonada sin terminar. */
    ABANDONADA("Abandonada");

    private final String nombreVisible;

    /**
     * Constructor del enum.
     *
     * @param nombreVisible nombre para mostrar al usuario
     */
    FasePartida(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /**
     * Retorna el nombre visible de la fase para mostrar al usuario.
     *
     * @return nombre en español
     */
    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Verifica si la partida ya terminó (finalizada o abandonada).
     *
     * @return {@code true} si terminó
     */
    public boolean esTerminal() {
        return this == FINALIZADA || this == ABANDONADA;
    }

    /**
     * Verifica si la partida está activa (en curso o pausada).
     *
     * @return {@code true} si está activa
     */
    public boolean estaActiva() {
        return this == EN_CURSO || this == PAUSADA;
    }

    /**
     * Retorna las fases a las que se puede transicionar desde esta fase.
     *
     * @return conjunto de fases destino válidas
     */
    public Set<FasePartida> transicionesValidas() {
        return switch (this) {
            case CREADA -> Set.of(EN_CURSO, ABANDONADA);
            case EN_CURSO -> Set.of(PAUSADA, FINALIZADA, ABANDONADA);
            case PAUSADA -> Set.of(EN_CURSO, ABANDONADA);
            case FINALIZADA, ABANDONADA -> Set.of();
        };
    }

    /**
     * Verifica si se puede transicionar de esta fase a otra.
     *
     * @param destino fase destino
     * @return {@code true} si la transición es válida
     */
    public boolean puedeTransicionarA(FasePartida destino) {
        return transicionesValidas().contains(destino);
    }
}