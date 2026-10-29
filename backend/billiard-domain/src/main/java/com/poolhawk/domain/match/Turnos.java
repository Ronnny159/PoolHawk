package com.poolhawk.domain.match;

import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Gestiona el orden de turnos de forma circular.
 *
 * <p>Utiliza genéricos para poder rotar indistintamente entre {@link Jugador}
 * (en modalidades individuales) o {@link Equipo} (en modalidades de parejas).</p>
 *
 * <p>Esta clase es mutable: {@link #avanzar()} modifica el índice interno
 * del jugador/equipo actual.</p>
 *
 * @param <T> El tipo de participante (Jugador o Equipo).
 * @author PoolHawk Team
 * @since 0.2.0
 */
public class Turnos<T> {

    private final List<T> orden;
    private int indiceActual;

    /**
     * Crea un gestor de turnos con el orden indicado, comenzando por el
     * primer participante de la lista.
     *
     * @param orden lista de participantes en el orden de juego
     * @throws NullPointerException     si la lista es nula
     * @throws IllegalArgumentException si la lista tiene menos de 2 participantes
     */
    public Turnos(List<T> orden) {
        this(orden, 0);
    }

    /**
     * Crea un gestor de turnos con el orden indicado, comenzando en la
     * posición especificada.
     *
     * @param orden         lista de participantes en el orden de juego
     * @param indiceInicial índice del participante que comienza (0-based)
     * @throws NullPointerException     si la lista es nula
     * @throws IllegalArgumentException si la lista tiene menos de 2 participantes
     *                                  o el índice inicial está fuera de rango
     */
    public Turnos(List<T> orden, int indiceInicial) {
        Objects.requireNonNull(orden, "La lista de participantes no puede ser nula");

        if (orden.size() < 2) {
            throw new IllegalArgumentException(
                "Se requieren al menos 2 participantes para gestionar turnos. " +
                "Cantidad recibida: " + orden.size()
            );
        }

        if (indiceInicial < 0 || indiceInicial >= orden.size()) {
            throw new IllegalArgumentException(
                "El índice inicial debe estar entre 0 y " + (orden.size() - 1) +
                ". Valor recibido: " + indiceInicial
            );
        }

        // Verificar que no haya participantes duplicados
        long distintos = orden.stream().distinct().count();
        if (distintos != orden.size()) {
            throw new IllegalArgumentException(
                "La lista de participantes contiene duplicados"
            );
        }

        this.orden = Collections.unmodifiableList(new ArrayList<>(orden));
        this.indiceActual = indiceInicial;
    }

    /**
     * Fábrica para crear los turnos a partir de una lista de Equipos.
     * (Útil para 2 vs 2).
     *
     * @param equipos Lista de equipos.
     * @return Gestor de turnos de tipo Equipo.
     */
    public static Turnos<Equipo> desdeEquipos(List<Equipo> equipos) {
        return new Turnos<>(equipos);
    }

    /**
     * Fábrica para crear los turnos a partir de una lista de Jugadores.
     * (Útil para 1 vs 1, 1 vs 1 vs 1, etc.).
     *
     * @param jugadores Lista de jugadores.
     * @return Gestor de turnos de tipo Jugador.
     */
    public static Turnos<Jugador> desdeJugadores(List<Jugador> jugadores) {
        return new Turnos<>(jugadores);
    }

    /**
     * Retorna el participante que tiene el turno actual.
     *
     * @return jugador o equipo actual
     */
    public T jugadorActual() {
        return orden.get(indiceActual);
    }

    /**
     * Retorna el índice actual en el orden de turnos.
     *
     * @return índice 0-based
     */
    public int indiceActual() {
        return indiceActual;
    }

    /**
     * Avanza al siguiente participante en la rotación circular.
     */
    public void avanzar() {
        indiceActual = (indiceActual + 1) % orden.size();
    }

    /**
     * Avanza {@code n} posiciones en la rotación circular.
     *
     * @param n número de posiciones a avanzar (puede ser negativo para retroceder)
     */
    public void avanzar(int n) {
        int tamaño = orden.size();
        indiceActual = ((indiceActual + n) % tamaño + tamaño) % tamaño;
    }

    /**
     * Reinicia la rotación al primer participante de la lista.
     */
    public void reiniciar() {
        indiceActual = 0;
    }

    /**
     * Reinicia la rotación de manera que comience el participante indicado.
     *
     * @param participante jugador o equipo que debe comenzar
     * @throws IllegalArgumentException si el participante no está en el orden
     */
    public void reiniciarDesde(T participante) {
        Objects.requireNonNull(participante, "El participante no puede ser nulo");
        int posicion = posicionDe(participante);
        if (posicion < 0) {
            throw new IllegalArgumentException(
                "El participante no está en el orden de turnos"
            );
        }
        indiceActual = posicion;
    }

    /**
     * Retorna una lista inmutable con el orden completo.
     *
     * @return orden de turnos
     */
    public List<T> jugadores() {
        return orden;
    }

    /**
     * Retorna la cantidad total de participantes en la rotación.
     *
     * @return cantidad
     */
    public int cantidadJugadores() {
        return orden.size();
    }

    /**
     * Verifica si es el turno del participante indicado.
     *
     * @param participante jugador o equipo a verificar
     * @return {@code true} si es su turno
     */
    public boolean esTurnoDe(T participante) {
        return jugadorActual().equals(participante);
    }

    /**
     * Retorna la posición de un participante en el orden.
     *
     * @param ganador jugador o equipo a buscar
     * @return índice 0-based, o -1 si no está en el orden
     */
    public int posicionDe(Object ganador) {
        return orden.indexOf(ganador);
    }

    /**
     * Retorna el siguiente participante en la rotación sin modificar el estado.
     *
     * @return siguiente participante
     */
    public T turnoSiguiente() {
        int siguiente = (indiceActual + 1) % orden.size();
        return orden.get(siguiente);
    }

    /**
     * Retorna el participante anterior en la rotación sin modificar el estado.
     *
     * @return participante anterior
     */
    public T turnoAnterior() {
        int anterior = (indiceActual - 1 + orden.size()) % orden.size();
        return orden.get(anterior);
    }
}