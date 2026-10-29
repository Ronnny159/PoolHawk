package com.poolhawk.domain.match;

/**
 * Define cómo se agrupan los jugadores en la partida.
 */
public enum FormatoJuego {
    /** Todos juegan por su cuenta (1 vs 1, 1 vs 1 vs 1, etc.) */
    INDIVIDUAL,
    
    /** Juegan en equipos de 2 personas (2 vs 2) */
    PAREJAS
}
