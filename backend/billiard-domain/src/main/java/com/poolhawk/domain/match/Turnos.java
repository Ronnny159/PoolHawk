package com.poolhawk.domain.match;

import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gestiona el orden de turnos entre los jugadores de una partida.
 *
 * <p>Los turnos funcionan como una lista circular: después del último
 * jugador, se vuelve al primero. El orden se define al crear el objeto
 * y se mantiene constante durante la partida, salvo reinicio explícito.</p>
 *
 * <p>El orden de juego según modalidad es:</p>
 * <ul>
 *   <li><b>Bola 8 (individual):</b> {@code [J1, J2]} — alternan uno a uno.</li>
 *   <li><b>Por Puntos (parejas):</b> {@code [A1, B1, A2, B2]} — rotación
 *       estricta intercalando equipos.</li>
 * </ul>
 *
 * <p>Esta clase es mutable: {@link #avanzar()} modifica el índice interno
 * del jugador actual.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class Turnos {

    private final List<Jugador> orden;
    private int indiceActual;

    /**
     * Crea un gestor de turnos con el orden indicado, comenzando por el
     * primer jugador de la lista.
     *
     * @param orden lista de jugadores en el orden de juego
     * @throws NullPointerException     si la lista es nula
     * @throws IllegalArgumentException si la lista está vacía o contiene
     *                                  menos de 2 jugadores
     */
    public Turnos(List<Jugador> orden) {
        this(orden, 0);
    }

    /**
     * Crea un gestor de turnos con el orden indicado, comenzando en la
     * posición especificada.
     *
     * @param orden         lista de jugadores en el orden de juego
     * @param indiceInicial índice del jugador que comienza (0-based)
     * @throws NullPointerException     si la lista es nula
     * @throws IllegalArgumentException si la lista tiene menos de 2 jugadores
     *                                  o el índice inicial está fuera de rango
     */
    public Turnos(List<Jugador> orden, int indiceInicial) {
        Objects.requireNonNull(orden, "La lista de jugadores no puede ser nula");

        if (orden.size() < 2) {
            throw new IllegalArgumentException(
                "Se requieren al menos 2 jugadores para gestionar turnos. " +
                "Cantidad recibida: " + orden.size()
            );
        }

        if (indiceInicial < 0 || indiceInicial >= orden.size()) {
            throw new IllegalArgumentException(
                "El índice inicial debe estar entre 0 y " + (orden.size() - 1) +
                ". Valor recibido: " + indiceInicial
            );
        }

        // Verificar que no haya jugadores duplicados
        long distintos = orden.stream().distinct().count();
        if (distintos != orden.size()) {
            throw new IllegalArgumentException(
                "La lista de jugadores contiene duplicados"
            );
        }

        this.orden = Collections.unmodifiableList(new ArrayList<>(orden));
        this.indiceActual = indiceInicial;
    }

    /**
     * Crea un gestor de turnos a partir de dos equipos, intercalando
     * sus jugadores en el orden correcto.
     *
     * <p>Para equipos con 2 jugadores cada uno, el orden resultante es:
     * {@code [A[0], B[0], A[1], B[1]]}. Para equipos con 1 jugador,
     * el orden es {@code [A[0], B[0]]}.</p>
     *
     * <p>Útil para construir el orden estándar de Por Puntos y Bola 8.</p>
     *
     * @param equipoA primer equipo
     * @param equipoB segundo equipo
     * @return gestor de turnos con el orden intercalado
     * @throws NullPointerException     si algún equipo es nulo
     * @throws IllegalArgumentException si ambos equipos tienen distinto
     *                                  número de jugadores
     */
    public static Turnos desdeEquipos(Equipo equipoA, Equipo equipoB) {
        Objects.requireNonNull(equipoA, "El equipo A no puede ser nulo");
        Objects.requireNonNull(equipoB, "El equipo B no puede ser nulo");

        int cantidadA = equipoA.cantidadJugadores();
        int cantidadB = equipoB.cantidadJugadores();

        if (cantidadA != cantidadB) {
            throw new IllegalArgumentException(
                "Los equipos deben tener la misma cantidad de jugadores. " +
                "Equipo A: " + cantidadA + ", Equipo B: " + cantidadB
            );
        }

        if (cantidadA == 0) {
            throw new IllegalArgumentException(
                "Los equipos deben tener al menos un jugador"
            );
        }

        List<Jugador> intercalados = new ArrayList<>(cantidadA * 2);
        List<Jugador> jugadoresA = equipoA.getJugadores();
        List<Jugador> jugadoresB = equipoB.getJugadores();

        for (int i = 0; i < cantidadA; i++) {
            intercalados.add(jugadoresA.get(i));
            intercalados.add(jugadoresB.get(i));
        }

        return new Turnos(intercalados);
    }

    /**
     * Retorna el jugador que tiene el turno actual.
     *
     * @return jugador actual
     */
    public Jugador jugadorActual() {
        return orden.get(indiceActual);
    }

    /**
     * Retorna el índice actual del jugador en el orden de turnos.
     *
     * @return índice 0-based
     */
    public int indiceActual() {
        return indiceActual;
    }

    /**
     * Avanza al siguiente jugador en la rotación circular.
     *
     * <p>Si el jugador actual es el último de la lista, el siguiente es
     * el primero.</p>
     */
    public void avanzar() {
        indiceActual = (indiceActual + 1) % orden.size();
    }

    /**
     * Avanza {@code n} posiciones en la rotación circular.
     *
     * @param n número de posiciones a avanzar (puede ser negativo para
     *          retroceder)
     */
    public void avanzar(int n) {
        int tamaño = orden.size();
        indiceActual = ((indiceActual + n) % tamaño + tamaño) % tamaño;
    }

    /**
     * Reinicia la rotación al primer jugador de la lista.
     */
    public void reiniciar() {
        indiceActual = 0;
    }

    /**
     * Reinicia la rotación de manera que comience el jugador indicado.
     *
     * <p>Útil para establecer el ganador del lag como primer tirador.</p>
     *
     * @param jugador jugador que debe comenzar
     * @throws IllegalArgumentException si el jugador no está en el orden
     */
    public void reiniciarDesde(Jugador jugador) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        int posicion = posicionDe(jugador);
        if (posicion < 0) {
            throw new IllegalArgumentException(
                "El jugador " + jugador.nombre() + " no está en el orden de turnos"
            );
        }
        indiceActual = posicion;
    }

    /**
     * Retorna una lista inmutable con el orden completo de jugadores.
     *
     * @return orden de turnos
     */
    public List<Jugador> jugadores() {
        return orden;
    }

    /**
     * Retorna la cantidad total de jugadores en la rotación.
     *
     * @return cantidad de jugadores
     */
    public int cantidadJugadores() {
        return orden.size();
    }

    /**
     * Verifica si es el turno del jugador indicado.
     *
     * @param jugador jugador a verificar
     * @return {@code true} si es su turno
     */
    public boolean esTurnoDe(Jugador jugador) {
        return jugadorActual().equals(jugador);
    }

    /**
     * Retorna la posición de un jugador en el orden.
     *
     * @param jugador jugador a buscar
     * @return índice 0-based, o -1 si no está en el orden
     */
    public int posicionDe(Jugador jugador) {
        return orden.indexOf(jugador);
    }

    /**
     * Retorna el siguiente jugador en la rotación sin modificar el estado.
     *
     * @return siguiente jugador
     */
    public Jugador turnoSiguiente() {
        int siguiente = (indiceActual + 1) % orden.size();
        return orden.get(siguiente);
    }

    /**
     * Retorna el jugador anterior en la rotación sin modificar el estado.
     *
     * @return jugador anterior
     */
    public Jugador turnoAnterior() {
        int anterior = (indiceActual - 1 + orden.size()) % orden.size();
        return orden.get(anterior);
    }
}