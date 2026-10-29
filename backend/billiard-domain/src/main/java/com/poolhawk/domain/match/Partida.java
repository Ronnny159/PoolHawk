package com.poolhawk.domain.match;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Mesa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa una partida completa de billar.
 *
 * <p>Es el <b>agregado raíz</b> del dominio: contiene la identidad de la
 * partida, los equipos participantes, la modalidad y el estado actual.
 * Toda operación sobre la partida (aplicar tiro, cambiar fase, declarar
 * ganador) se hace a través de esta clase.</p>
 *
 * <p>{@code Partida} delega el estado del juego a {@link MatchState}.
 * Esta clase solo se encarga de la identidad y los metadatos.</p>
 *
 * <p>Su identidad (id, modalidad, equipos, fecha de creación) es
 * inmutable. El estado interno cambia durante el desarrollo de la
 * partida.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class Partida {

    private final String id;
    private final TipoModalidad modalidad;
    private final Equipo equipoA;
    private final Equipo equipoB;
    private final Instant fechaCreacion;
    private final MatchState estado;

    /**
     * Crea una nueva partida en fase CREADA.
     */
    public Partida(String id,
                   TipoModalidad modalidad,
                   Equipo equipoA,
                   Equipo equipoB,
                   Mesa mesa,
                   Instant fechaCreacion) {
        Objects.requireNonNull(id, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(equipoA, "El equipo A no puede ser nulo");
        Objects.requireNonNull(equipoB, "El equipo B no puede ser nulo");
        Objects.requireNonNull(mesa, "La mesa no puede ser nula");
        Objects.requireNonNull(fechaCreacion, "La fecha de creación no puede ser nula");

        if (id.isBlank()) {
            throw new IllegalArgumentException(
                "El id de la partida no puede estar en blanco"
            );
        }

        validarEquipos(modalidad, equipoA, equipoB);

        this.id = id;
        this.modalidad = modalidad;
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.fechaCreacion = fechaCreacion;

        List<Jugador> jugadores = new ArrayList<>(equipoA.getJugadores());
        jugadores.addAll(equipoB.getJugadores());

        Turnos<Jugador> turnos = new Turnos<>(jugadores);
        this.estado = new MatchState(id, modalidad, turnos, mesa, fechaCreacion);
    }

    /**
     * NUEVO CONSTRUCTOR: Crea una partida con una lista dinámica de 2 a 4 jugadores.
     * (Necesario para el modo 1v1v1v1 o 2v2).
     */
    public Partida(String id,
                   TipoModalidad modalidad,
                   List<Jugador> jugadores,
                   MatchState estado,
                   Instant fechaCreacion) {
        this.id = id;
        this.modalidad = modalidad;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        
        // Asignamos los 2 primeros jugadores como Equipo A y B solo para mantener 
        // compatibilidad con el mapper actual y la base de datos.
        // (El estado interno ya tiene a los 4 jugadores en el Turnos).
        this.equipoA = Equipo.individual("eA-" + id, jugadores.get(0).nombre(), jugadores.get(0));
        this.equipoB = Equipo.individual("eB-" + id, jugadores.get(1).nombre(), jugadores.get(1));
    }

    /**
     * Valida que los equipos sean coherentes con la modalidad.
     */
    private static void validarEquipos(TipoModalidad modalidad,
                                       Equipo equipoA,
                                       Equipo equipoB) {
        int cantidadA = equipoA.cantidadJugadores();
        int cantidadB = equipoB.cantidadJugadores();

        if (cantidadA != cantidadB) {
            throw new IllegalArgumentException(
                "Los equipos deben tener la misma cantidad de jugadores. " +
                "Equipo A: " + cantidadA + ", Equipo B: " + cantidadB
            );
        }

        if (modalidad.esIndividual() && cantidadA != 1) {
            throw new IllegalArgumentException(
                "La modalidad Bola 8 individual requiere equipos de 1 jugador. " +
                "Cantidad recibida: " + cantidadA
            );
        }

        if (modalidad.esPorEquipos() && cantidadA != 2) {
            throw new IllegalArgumentException(
                "La modalidad Por Puntos requiere equipos de 2 jugadores. " +
                "Cantidad recibida: " + cantidadA
            );
        }
    }

    /**
     * Crea una partida de Bola 8 individual con dos jugadores.
     */
    public static Partida individual(String id, Jugador j1, Jugador j2, Mesa mesa) {
        Objects.requireNonNull(j1, "El jugador 1 no puede ser nulo");
        Objects.requireNonNull(j2, "El jugador 2 no puede ser nulo");
        if (j1.equals(j2)) {
            throw new IllegalArgumentException(
                "Los dos jugadores de una partida individual deben ser distintos"
            );
        }

        Equipo equipoA = Equipo.individual("eA-" + id, j1.nombre(), j1);
        Equipo equipoB = Equipo.individual("eB-" + id, j2.nombre(), j2);

        return new Partida(id, TipoModalidad.BOLA_8, equipoA, equipoB,
            mesa, Instant.now());
    }

    /**
     * Crea una partida de Por Puntos en parejas con dos equipos.
     */
    public static Partida parejas(String id, Equipo eqA, Equipo eqB, Mesa mesa) {
        return new Partida(id, TipoModalidad.POR_PUNTOS, eqA, eqB,
            mesa, Instant.now());
    }

    /** @return identificador único de la partida */
    public String getId() { return id; }

    /** @return modalidad de la partida */
    public TipoModalidad getModalidad() { return modalidad; }

    /** @return primer equipo participante */
    public Equipo getEquipoA() { return equipoA; }

    /** @return segundo equipo participante */
    public Equipo getEquipoB() { return equipoB; }

    /** @return momento en que se creó la partida */
    public Instant getFechaCreacion() { return fechaCreacion; }

    /** @return estado de la partida */
    public MatchState getEstado() { return estado; }

    /** @return fase actual */
    public FasePartida getFase() { return estado.getFase(); }

    /** @return ganador, o {@code null} */
    public Jugador getGanador() { return estado.getGanador(); }

    /** @return lista con los dos equipos */
    public List<Equipo> getEquipos() { return List.of(equipoA, equipoB); }

    /** @return mesa de la partida */
    public Mesa getMesa() { return estado.getMesa(); }

    public void iniciar() { estado.iniciar(); }

    public void pausar() { estado.pausar(); }

    public void reanudar() { estado.reanudar(); }

    public void finalizar(Jugador ganador) { estado.finalizar(ganador); }

    public void abandonar() { estado.abandonar(); }

    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) {
        estado.aplicarTiro(evento, resultado);
    }

    public boolean estaTerminada() { return estado.estaTerminada(); }

    public boolean estaActiva() { return estado.estaActiva(); }

    public Jugador jugadorActual() { return estado.jugadorActual(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Partida that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "Partida{" +
            "id='" + id + '\'' +
            ", modalidad=" + modalidad +
            ", fase=" + getFase() +
            ", equipoA='" + equipoA.getNombre() + '\'' +
            ", equipoB='" + equipoB.getNombre() + '\'' +
            '}';
    }
}