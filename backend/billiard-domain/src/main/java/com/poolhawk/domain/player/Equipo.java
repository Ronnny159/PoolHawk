package com.poolhawk.domain.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Representa un equipo de jugadores de billar.
 *
 * <p>Un equipo agrupa uno o dos jugadores según la modalidad:</p>
 * <ul>
 *   <li><b>Bola 8 (individual):</b> cada equipo contiene un único jugador.
 *       Se modela así por consistencia, aunque en la práctica compitan
 *       individualmente.</li>
 *   <li><b>Por Puntos en parejas:</b> cada equipo contiene dos jugadores,
 *       que se alternan en el turno según el orden definido.</li>
 * </ul>
 *
 * <p>El equipo es inmutable en su identidad (id y nombre) pero mutable
 * en su composición: se pueden agregar o quitar jugadores antes de
 * iniciar la partida. Una vez iniciada, la composición no debería
 * cambiar.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class Equipo {

    /** Máximo de jugadores permitidos en un equipo. */
    public static final int MAX_JUGADORES = 2;

    private final String id;
    private final String nombre;
    private final List<Jugador> jugadores;

    /**
     * Crea un equipo vacío con identificador y nombre.
     *
     * @param id     identificador único del equipo
     * @param nombre nombre visible del equipo
     * @throws NullPointerException     si el id o el nombre son nulos
     * @throws IllegalArgumentException si el id o el nombre están en blanco
     *                                  o si el nombre excede 80 caracteres
     */
    public Equipo(String id, String nombre) {
        Objects.requireNonNull(id, "El identificador del equipo no puede ser nulo");
        Objects.requireNonNull(nombre, "El nombre del equipo no puede ser nulo");

        if (id.isBlank()) {
            throw new IllegalArgumentException(
                "El identificador del equipo no puede estar en blanco"
            );
        }

        if (nombre.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre del equipo no puede estar en blanco"
            );
        }

        if (nombre.length() > 80) {
            throw new IllegalArgumentException(
                "El nombre del equipo no puede exceder los 80 caracteres. " +
                "Longitud actual: " + nombre.length()
            );
        }

        this.id = id;
        this.nombre = nombre.trim();
        this.jugadores = new ArrayList<>();
    }

    /**
     * Crea un equipo con un jugador individual (modalidad Bola 8).
     *
     * <p>Útil para modelar la modalidad individual donde cada "equipo"
     * contiene un solo jugador.</p>
     *
     * @param id      identificador del equipo
     * @param nombre  nombre del equipo
     * @param jugador único jugador del equipo
     * @return equipo con un jugador
     */
    public static Equipo individual(String id, String nombre, Jugador jugador) {
        Equipo equipo = new Equipo(id, nombre);
        equipo.agregarJugador(jugador);
        return equipo;
    }

    /**
     * Crea un equipo con dos jugadores (modalidad Por Puntos en parejas).
     *
     * @param id       identificador del equipo
     * @param nombre   nombre del equipo
     * @param jugador1 primer jugador
     * @param jugador2 segundo jugador
     * @return equipo con dos jugadores
     * @throws IllegalArgumentException si los dos jugadores son iguales
     */
    public static Equipo pareja(String id, String nombre,
                                Jugador jugador1, Jugador jugador2) {
        if (jugador1.equals(jugador2)) {
            throw new IllegalArgumentException(
                "Un equipo no puede tener dos jugadores idénticos"
            );
        }
        Equipo equipo = new Equipo(id, nombre);
        equipo.agregarJugador(jugador1);
        equipo.agregarJugador(jugador2);
        return equipo;
    }

    /**
     * Retorna el identificador del equipo.
     *
     * @return id del equipo
     */
    public String getId() {
        return id;
    }

    /**
     * Retorna el nombre del equipo.
     *
     * @return nombre del equipo
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Retorna una lista inmutable con los jugadores del equipo.
     *
     * @return jugadores
     */
    public List<Jugador> getJugadores() {
        return Collections.unmodifiableList(jugadores);
    }

    /**
     * Retorna cuántos jugadores tiene el equipo actualmente.
     *
     * @return número de jugadores
     */
    public int cantidadJugadores() {
        return jugadores.size();
    }

    /**
     * Verifica si el equipo está completo (tiene el máximo de jugadores).
     *
     * @return {@code true} si tiene 2 jugadores
     */
    public boolean estaCompleto() {
        return jugadores.size() == MAX_JUGADORES;
    }

    /**
     * Verifica si el equipo tiene al menos un jugador.
     *
     * @return {@code true} si tiene 1 o más jugadores
     */
    public boolean tieneJugadores() {
        return !jugadores.isEmpty();
    }

    /**
     * Agrega un jugador al equipo.
     *
     * @param jugador jugador a agregar
     * @throws NullPointerException     si el jugador es nulo
     * @throws IllegalArgumentException si el equipo ya está completo
     *                                  o si el jugador ya está en el equipo
     */
    public void agregarJugador(Jugador jugador) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");

        if (estaCompleto()) {
            throw new IllegalArgumentException(
                "El equipo ya tiene el máximo de " + MAX_JUGADORES + " jugadores"
            );
        }

        if (jugadores.contains(jugador)) {
            throw new IllegalArgumentException(
                "El jugador " + jugador.nombre() + " ya está en el equipo"
            );
        }

        jugadores.add(jugador);
    }

    /**
     * Quita un jugador del equipo por su identificador.
     *
     * @param jugadorId identificador del jugador a quitar
     * @throws IllegalArgumentException si no existe un jugador con ese id
     */
    public void quitarJugador(String jugadorId) {
        boolean removido = jugadores.removeIf(j -> j.id().equals(jugadorId));
        if (!removido) {
            throw new IllegalArgumentException(
                "No existe un jugador con id " + jugadorId + " en el equipo"
            );
        }
    }

    /**
     * Busca un jugador del equipo por su identificador.
     *
     * @param jugadorId identificador del jugador
     * @return el jugador si existe, o {@link Optional#empty()} si no
     */
    public Optional<Jugador> buscarJugador(String jugadorId) {
        return jugadores.stream()
            .filter(j -> j.id().equals(jugadorId))
            .findFirst();
    }

    /**
     * Verifica si un jugador pertenece al equipo.
     *
     * @param jugadorId identificador del jugador
     * @return {@code true} si pertenece al equipo
     */
    public boolean contieneJugador(String jugadorId) {
        return jugadores.stream()
            .anyMatch(j -> j.id().equals(jugadorId));
    }

    /**
     * Retorna el otro jugador del equipo dado uno de sus miembros.
     *
     * <p>Útil en modalidad Por Puntos para saber a quién le toca el
     * siguiente turno dentro del mismo equipo.</p>
     *
     * @param jugadorId identificador de un jugador del equipo
     * @return el otro jugador, o {@link Optional#empty()} si el equipo
     *         solo tiene un jugador
     * @throws IllegalArgumentException si el jugador no pertenece al equipo
     */
    public Optional<Jugador> getCompaneroDe(String jugadorId) {
        if (!contieneJugador(jugadorId)) {
            throw new IllegalArgumentException(
                "El jugador con id " + jugadorId + " no pertenece a este equipo"
            );
        }
        return jugadores.stream()
            .filter(j -> !j.id().equals(jugadorId))
            .findFirst();
    }
}