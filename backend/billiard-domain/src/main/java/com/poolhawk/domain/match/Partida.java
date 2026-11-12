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
 * Entidad de dominio que representa una partida de billar.
 */
public class Partida {

    private final String id;
    private final TipoModalidad modalidad;
    private final Equipo equipoA;
    private final Equipo equipoB;
    private final Instant fechaCreacion;
    private final MatchState estado;
    private final FormatoJuego formato;

    // Constructor original para nuevas partidas
    public Partida(String id, TipoModalidad modalidad, 
                   Equipo equipoA, Equipo equipoB, 
                   FormatoJuego formato, Mesa mesa, Instant fechaCreacion) {
        Objects.requireNonNull(id, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(equipoA, "El equipo A no puede ser nulo");
        Objects.requireNonNull(equipoB, "El equipo B no puede ser nulo");
        Objects.requireNonNull(mesa, "La mesa no puede ser nula");
        Objects.requireNonNull(fechaCreacion, "La fecha de creación no puede ser nula");
        Objects.requireNonNull(formato, "El formato no puede ser nulo");

        if (id.isBlank()) {
            throw new IllegalArgumentException("El id de la partida no puede estar en blanco");
        }

        validarEquiposYFormato(modalidad, formato, equipoA, equipoB);

        this.id = id;
        this.modalidad = modalidad;
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.fechaCreacion = fechaCreacion;
        this.formato = formato;

        // Construir la rotación de turnos A1 -> B1 -> A2 -> B2
        List<Jugador> rotacion = construirRotacion(equipoA, equipoB);
        Turnos<Jugador> turnos = new Turnos<>(rotacion);
        this.estado = new MatchState(id, modalidad, equipoA, equipoB, turnos, mesa, fechaCreacion);
    }

    // NUEVO CONSTRUCTOR: Para rehidratación desde la base de datos
    public Partida(String id, TipoModalidad modalidad, 
                   Equipo equipoA, Equipo equipoB, 
                   FormatoJuego formato, Instant fechaCreacion, 
                   MatchState estadoRestaurado) {
        Objects.requireNonNull(id, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(equipoA, "El equipo A no puede ser nulo");
        Objects.requireNonNull(equipoB, "El equipo B no puede ser nulo");
        Objects.requireNonNull(fechaCreacion, "La fecha de creación no puede ser nula");
        Objects.requireNonNull(formato, "El formato no puede ser nulo");
        Objects.requireNonNull(estadoRestaurado, "El estado restaurado no puede ser nulo");

        if (id.isBlank()) {
            throw new IllegalArgumentException("El id de la partida no puede estar en blanco");
        }

        validarEquiposYFormato(modalidad, formato, equipoA, equipoB);

        this.id = id;
        this.modalidad = modalidad;
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.fechaCreacion = fechaCreacion;
        this.formato = formato;
        this.estado = estadoRestaurado; // Usamos el estado que viene de BD
    }

    private static List<Jugador> construirRotacion(Equipo equipoA, Equipo equipoB) {
        List<Jugador> jugadoresA = equipoA.getJugadores();
        List<Jugador> jugadoresB = equipoB.getJugadores();
        
        List<Jugador> rotacion = new ArrayList<>();
        int maxSize = Math.max(jugadoresA.size(), jugadoresB.size());
        for (int i = 0; i < maxSize; i++) {
            if (i < jugadoresA.size()) rotacion.add(jugadoresA.get(i));
            if (i < jugadoresB.size()) rotacion.add(jugadoresB.get(i));
        }
        return rotacion;
    }

    private static void validarEquiposYFormato(TipoModalidad modalidad, FormatoJuego formato, 
                                                Equipo equipoA, Equipo equipoB) {
        int cantidadA = equipoA.cantidadJugadores();
        int cantidadB = equipoB.cantidadJugadores();

        if (cantidadA != cantidadB) {
            throw new IllegalArgumentException("Los equipos deben tener la misma cantidad de jugadores.");
        }
        if (modalidad == TipoModalidad.BOLA_8) {
            if (formato != FormatoJuego.INDIVIDUAL) {
                throw new IllegalArgumentException("Bola 8 solo admite formato INDIVIDUAL.");
            }
            if (cantidadA != 1) {
                throw new IllegalArgumentException("Bola 8 requiere 1 jugador por equipo.");
            }
        } else if (modalidad == TipoModalidad.POR_PUNTOS) {
            if (formato == FormatoJuego.PAREJAS && cantidadA != 2) {
                throw new IllegalArgumentException("Por Puntos en PAREJAS requiere 2 jugadores por equipo.");
            }
            if (formato == FormatoJuego.INDIVIDUAL && cantidadA != 1 && cantidadA != 2) {
                throw new IllegalArgumentException("Por Puntos en INDIVIDUAL requiere 1 o 2 jugadores por equipo.");
            }
        }
    }

    // --- MÉTODOS ESTÁTICOS ---
    public static Partida individual(String id, Jugador j1, Jugador j2, Mesa mesa) {
        Equipo eqA = Equipo.individual("eA-" + id, j1.nombre(), j1);
        Equipo eqB = Equipo.individual("eB-" + id, j2.nombre(), j2);
        return new Partida(id, TipoModalidad.BOLA_8, eqA, eqB, FormatoJuego.INDIVIDUAL, mesa, Instant.now());
    }

    public static Partida parejas(String id, Equipo eqA, Equipo eqB, Mesa mesa) {
        return new Partida(id, TipoModalidad.POR_PUNTOS, eqA, eqB, FormatoJuego.PAREJAS, mesa, Instant.now());
    }

    // --- GETTERS ---
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
    public FormatoJuego getFormato() { return formato; }

    // --- MÉTODOS DELEGADOS ---
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