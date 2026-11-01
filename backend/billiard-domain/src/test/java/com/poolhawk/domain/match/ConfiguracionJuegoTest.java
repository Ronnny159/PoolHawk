package com.poolhawk.domain.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfiguracionJuego - Reglas de armado")
class ConfiguracionJuegoTest {

    @Test
    @DisplayName("Crea una configuración válida de 4 jugadores individuales en Bola 8")
    void creaConfiguracionValida() {
        ConfiguracionJuego config = new ConfiguracionJuego(
                TipoModalidad.BOLA_8, 4, FormatoJuego.INDIVIDUAL);
        
        assertEquals(4, config.cantidadJugadores());
        assertEquals(4, config.obtenerCantidadBandos());
    }

    @Test
    @DisplayName("Calcula correctamente los bandos en formato parejas")
    void calculaBandosParejas() {
        ConfiguracionJuego config = new ConfiguracionJuego(
                TipoModalidad.POR_PUNTOS, 4, FormatoJuego.PAREJAS);
        
        assertEquals(2, config.obtenerCantidadBandos());
    }

    @Test
    @DisplayName("Rechaza cantidad de jugadores menor a 2")
    void rechazaUnJugador() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ConfiguracionJuego(TipoModalidad.BOLA_8, 1, FormatoJuego.INDIVIDUAL));
    }

    @Test
    @DisplayName("Rechaza cantidad de jugadores mayor a 4")
    void rechazaCincoJugadores() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ConfiguracionJuego(TipoModalidad.BOLA_8, 5, FormatoJuego.INDIVIDUAL));
    }

    @Test
    @DisplayName("Rechaza parejas con número impar de jugadores")
    void rechazaParejasImpares() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ConfiguracionJuego(TipoModalidad.POR_PUNTOS, 3, FormatoJuego.PAREJAS));
    }
}