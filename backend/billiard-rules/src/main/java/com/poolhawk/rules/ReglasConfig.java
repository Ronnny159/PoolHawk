package com.poolhawk.rules;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuración de reglas opcionales para una partida.
 *
 * <p>Algunas reglas no son obligatorias y pueden activarse o desactivarse
 * según el contexto (torneo, partida amistosa, etc.). Esta clase agrupa
 * esas opciones en un objeto inmutable.</p>
 *
 * <p>Configuraciones soportadas:</p>
 * <ul>
 *   <li><b>Call shot:</b> obliga a cantar bola y tronera en cada tiro.</li>
 *   <li><b>Shot clock:</b> límite de tiempo por tiro.</li>
 *   <li><b>Three fouls penalty:</b> penalización por tres faltas consecutivas.</li>
 * </ul>
 *
 * @param callShot          si {@code true}, se debe cantar la tronera
 * @param shotClock         duración máxima por tiro, o {@code null} si no hay límite
 * @param threeFoulsPenalty si {@code true}, tres faltas consecutivas penalizan
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record ReglasConfig(
        boolean callShot,
        Duration shotClock,
        boolean threeFoulsPenalty) {

    /**
     * Constructor compacto con validaciones.
     *
     * @throws IllegalArgumentException si el shotClock es negativo
     */
    public ReglasConfig {
        if (shotClock != null && shotClock.isNegative()) {
            throw new IllegalArgumentException(
                "El shot clock no puede ser negativo. Valor recibido: " + shotClock
            );
        }
    }

    /**
     * Configuración por defecto: sin reglas opcionales.
     *
     * @return configuración oficial estándar
     */
    public static ReglasConfig porDefecto() {
        return new ReglasConfig(false, null, false);
    }

    /**
     * Configuración oficial con call shot obligatorio.
     *
     * @return configuración con call shot
     */
    public static ReglasConfig conCallShot() {
        return new ReglasConfig(true, null, false);
    }

    /**
     * Configuración de torneo: call shot + shot clock de 45 segundos.
     *
     * @return configuración de torneo
     */
    public static ReglasConfig deTorneo() {
        return new ReglasConfig(true, Duration.ofSeconds(45), true);
    }

    /**
     * Verifica si hay límite de tiempo por tiro.
     *
     * @return {@code true} si hay shot clock configurado
     */
    public boolean tieneShotClock() {
        return shotClock != null;
    }
}