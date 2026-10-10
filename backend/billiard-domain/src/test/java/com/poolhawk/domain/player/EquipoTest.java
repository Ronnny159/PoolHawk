package com.poolhawk.domain.player;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Equipo}.
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@DisplayName("Equipo - agrupación de jugadores")
class EquipoTest {

    private static final Jugador ANA = new Jugador("j1", "Ana");
    private static final Jugador BETO = new Jugador("j2", "Beto");

    @Nested
    @DisplayName("Creación y validación")
    class Creacion {

        @Test
        @DisplayName("Crea un equipo vacío válido")
        void creaEquipoVacio() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            assertEquals("e1", equipo.getId());
            assertEquals("Los Tigres", equipo.getNombre());
            assertEquals(0, equipo.cantidadJugadores());
            assertFalse(equipo.estaCompleto());
            assertFalse(equipo.tieneJugadores());
        }

        @Test
        @DisplayName("Rechaza id nulo")
        void rechazaIdNulo() {
            assertThrows(NullPointerException.class,
                () -> new Equipo(null, "Los Tigres"));
        }

        @Test
        @DisplayName("Rechaza nombre nulo")
        void rechazaNombreNulo() {
            assertThrows(NullPointerException.class,
                () -> new Equipo("e1", null));
        }

        @Test
        @DisplayName("Rechaza id en blanco")
        void rechazaIdEnBlanco() {
            assertThrows(IllegalArgumentException.class,
                () -> new Equipo("", "Los Tigres"));
        }

        @Test
        @DisplayName("Rechaza nombre en blanco")
        void rechazaNombreEnBlanco() {
            assertThrows(IllegalArgumentException.class,
                () -> new Equipo("e1", "  "));
        }

        @Test
        @DisplayName("Rechaza nombre mayor a 80 caracteres")
        void rechazaNombreLargo() {
            String largo = "A".repeat(81);
            assertThrows(IllegalArgumentException.class,
                () -> new Equipo("e1", largo));
        }

        @Test
        @DisplayName("Normaliza espacios del nombre")
        void normalizaEspacios() {
            Equipo equipo = new Equipo("e1", "  Los Tigres  ");
            assertEquals("Los Tigres", equipo.getNombre());
        }
    }

    @Nested
    @DisplayName("Fábricas")
    class Fabricas {

        @Test
        @DisplayName("Fábrica individual crea equipo con un jugador")
        void fabricaIndividual() {
            Equipo equipo = Equipo.individual("e1", "Ana", ANA);
            assertEquals(1, equipo.cantidadJugadores());
            assertTrue(equipo.contieneJugador("j1"));
            assertFalse(equipo.estaCompleto());
        }

        @Test
        @DisplayName("Fábrica pareja crea equipo con dos jugadores")
        void fabricaPareja() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            assertEquals(2, equipo.cantidadJugadores());
            assertTrue(equipo.estaCompleto());
        }

        @Test
        @DisplayName("Fábrica pareja rechaza jugadores idénticos")
        void parejaRechazaIdenticos() {
            assertThrows(IllegalArgumentException.class,
                () -> Equipo.pareja("e1", "Los Tigres", ANA, ANA));
        }
    }

    @Nested
    @DisplayName("Gestión de jugadores")
    class GestionDeJugadores {

        @Test
        @DisplayName("Agrega jugadores correctamente")
        void agregaJugadores() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            equipo.agregarJugador(ANA);
            equipo.agregarJugador(BETO);
            assertEquals(2, equipo.cantidadJugadores());
            assertTrue(equipo.estaCompleto());
        }

        @Test
        @DisplayName("Rechaza más de 2 jugadores")
        void rechazaTercerJugador() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            Jugador carla = new Jugador("j3", "Carla");
            assertThrows(IllegalArgumentException.class,
                () -> equipo.agregarJugador(carla));
        }

        @Test
        @DisplayName("Rechaza jugador duplicado")
        void rechazaDuplicado() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            equipo.agregarJugador(ANA);
            assertThrows(IllegalArgumentException.class,
                () -> equipo.agregarJugador(ANA));
        }

        @Test
        @DisplayName("Rechaza jugador nulo")
        void rechazaNulo() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            assertThrows(NullPointerException.class,
                () -> equipo.agregarJugador(null));
        }

        @Test
        @DisplayName("Quita jugador correctamente")
        void quitarJugador() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            equipo.quitarJugador("j1");
            assertEquals(1, equipo.cantidadJugadores());
            assertFalse(equipo.contieneJugador("j1"));
        }

        @Test
        @DisplayName("Quitar jugador inexistente lanza excepción")
        void quitarInexistente() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            assertThrows(IllegalArgumentException.class,
                () -> equipo.quitarJugador("j99"));
        }

        @Test
        @DisplayName("La lista de jugadores es inmutable")
        void listaInmutable() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            assertThrows(UnsupportedOperationException.class,
                () -> equipo.getJugadores().clear());
        }
    }

    @Nested
    @DisplayName("Consulta de jugadores")
    class Consulta {

        @Test
        @DisplayName("Busca un jugador existente")
        void buscarExistente() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            Optional<Jugador> resultado = equipo.buscarJugador("j1");
            assertTrue(resultado.isPresent());
            assertEquals("Ana", resultado.get().nombre());
        }

        @Test
        @DisplayName("Buscar jugador inexistente retorna vacío")
        void buscarInexistente() {
            Equipo equipo = new Equipo("e1", "Los Tigres");
            assertTrue(equipo.buscarJugador("j99").isEmpty());
        }

        @Test
        @DisplayName("contieneJugador retorna true para miembro")
        void contieneMiembro() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            assertTrue(equipo.contieneJugador("j1"));
            assertTrue(equipo.contieneJugador("j2"));
            assertFalse(equipo.contieneJugador("j3"));
        }

        @Test
        @DisplayName("getJugadores retorna la lista completa")
        void obtenerTodos() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            List<Jugador> jugadores = equipo.getJugadores();
            assertEquals(2, jugadores.size());
            assertTrue(jugadores.contains(ANA));
            assertTrue(jugadores.contains(BETO));
        }
    }

    @Nested
    @DisplayName("Compañero de equipo")
    class Companero {

        @Test
        @DisplayName("getCompaneroDe retorna el otro jugador")
        void retornaCompanero() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            Optional<Jugador> companero = equipo.getCompaneroDe("j1");
            assertTrue(companero.isPresent());
            assertEquals(BETO, companero.get());
        }

        @Test
        @DisplayName("getCompaneroDe retorna vacío si el equipo tiene 1 jugador")
        void sinCompanero() {
            Equipo equipo = Equipo.individual("e1", "Ana", ANA);
            Optional<Jugador> companero = equipo.getCompaneroDe("j1");
            assertTrue(companero.isEmpty());
        }

        @Test
        @DisplayName("getCompaneroDe rechaza jugador que no pertenece al equipo")
        void rechazaAjeno() {
            Equipo equipo = Equipo.pareja("e1", "Los Tigres", ANA, BETO);
            assertThrows(IllegalArgumentException.class,
                () -> equipo.getCompaneroDe("j99"));
        }
    }
}