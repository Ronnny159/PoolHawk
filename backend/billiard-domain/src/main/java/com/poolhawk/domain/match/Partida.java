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

public class Partida {

    private final String id;
    private final TipoModalidad modalidad;
    private final Equipo equipoA;
    private final Equipo equipoB;
    private final Instant fechaCreacion;
    private final MatchState estado;

    // Constructor original (por si acaso se usa en tests)
    public Partida(String id, TipoModalidad modalidad, Equipo equipoA, Equipo equipoB, Mesa mesa, Instant fechaCreacion) {
        Objects.requireNonNull(id, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(equipoA, "El equipo A no puede ser nulo");
        Objects.requireNonNull(equipoB, "El equipo B no puede ser nulo");
        Objects.requireNonNull(mesa, "La mesa no puede ser nula");
        Objects.requireNonNull(fechaCreacion, "La fecha de creación no puede ser nula");

        if (id.isBlank()) {
            throw new IllegalArgumentException("El id de la partida no puede estar en blanco");
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
     * Constructor dinámico corregido: Arma los equipos A y B a partir de la lista plana.
     * Si hay 2 jugadores, es 1v1. Si hay 4, es 2v2 (J1,J3 vs J2,J4).
     */
    public Partida(String id, TipoModalidad modalidad, List<Jugador> jugadores, MatchState estado, Instant fechaCreacion) {
        this.id = id;
        this.modalidad = modalidad;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;

        if (jugadores.size() == 2) {
            // 1 vs 1
            this.equipoA = Equipo.individual("eA-" + id, jugadores.get(0).nombre(), jugadores.get(0));
            this.equipoB = Equipo.individual("eB-" + id, jugadores.get(1).nombre(), jugadores.get(1));
        } else if (jugadores.size() == 4) {
            // 2 vs 2 (Asumimos que el frontend envía A1, B1, A2, B2)
            this.equipoA = Equipo.pareja("eA-" + id, jugadores.get(0).nombre() + " & " + jugadores.get(2).nombre(), jugadores.get(0), jugadores.get(2));
            this.equipoB = Equipo.pareja("eB-" + id, jugadores.get(1).nombre() + " & " + jugadores.get(3).nombre(), jugadores.get(1), jugadores.get(3));
        } else {
            // Fallback para 1v1v1v1 o casos raros: los primeros 2 forman los equipos base, el resto se maneja en el MatchState
            this.equipoA = Equipo.individual("eA-" + id, jugadores.get(0).nombre(), jugadores.get(0));
            this.equipoB = Equipo.individual("eB-" + id, jugadores.get(1).nombre(), jugadores.get(1));
        }
    }

    private static void validarEquipos(TipoModalidad modalidad, Equipo equipoA, Equipo equipoB) {
        int cantidadA = equipoA.cantidadJugadores();
        int cantidadB = equipoB.cantidadJugadores();

        if (cantidadA != cantidadB) {
            throw new IllegalArgumentException("Los equipos deben tener la misma cantidad de jugadores.");
        }
        if (modalidad.esIndividual() && cantidadA != 1) {
            throw new IllegalArgumentException("La modalidad Bola 8 individual requiere equipos de 1 jugador.");
        }
        if (modalidad.esPorEquipos() && cantidadA != 2) {
            throw new IllegalArgumentException("La modalidad Por Puntos requiere equipos de 2 jugadores.");
        }
    }

    public static Partida individual(String id, Jugador j1, Jugador j2, Mesa mesa) {
        return new Partida(id, TipoModalidad.BOLA_8, 
            Equipo.individual("eA-" + id, j1.nombre(), j1), 
            Equipo.individual("eB-" + id, j2.nombre(), j2), 
            mesa, Instant.now());
    }

    public static Partida parejas(String id, Equipo eqA, Equipo eqB, Mesa mesa) {
        return new Partida(id, TipoModalidad.POR_PUNTOS, eqA, eqB, mesa, Instant.now());
    }

    public String getId() { return id; }
    public TipoModalidad getModalidad() { return modalidad; }
    public Equipo getEquipoA() { return equipoA; }
    public Equipo getEquipoB() { return equipoB; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public MatchState getEstado() { return estado; }
    public FasePartida getFase() { return estado.getFase(); }
    public Jugador getGanador() { return estado.getGanador(); }
    public List<Equipo> getEquipos() { return List.of(equipoA, equipoB); }
    public Mesa getMesa() { return estado.getMesa(); }

    public void iniciar() { estado.iniciar(); }
    public void pausar() { estado.pausar(); }
    public void reanudar() { estado.reanudar(); }
    public void finalizar(Jugador ganador) { estado.finalizar(ganador); }
    public void abandonar() { estado.abandonar(); }
    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) { estado.aplicarTiro(evento, resultado); }
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
        return "Partida{id='" + id + "', modalidad=" + modalidad + ", fase=" + getFase() + "}";
    }
}