package com.poolhawk.domain.match;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Mesa;

import java.time.Instant;
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
     *
     * <p>Construye internamente el {@link MatchState} a partir de los
     * equipos, intercalando sus jugadores según la modalidad.</p>
     *
     * @param id            identificador único de la partida
     * @param modalidad     modalidad de la partida
     * @param equipoA       primer equipo
     * @param equipoB       segundo equipo
     * @param mesa          mesa con las bolas en su posición inicial
     * @param fechaCreacion momento en que se creó la partida
     * @throws NullPointerException     si algún parámetro es nulo
     * @throws IllegalArgumentException si el id está en blanco, si los
     *                                  equipos tienen distinta cantidad de
     *                                  jugadores, o si la cantidad no
     *                                  corresponde a la modalidad
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

        Turnos turnos = Turnos.desdeEquipos(equipoA, equipoB);
        this.estado = new MatchState(id, modalidad, turnos, mesa, fechaCreacion);
    }

    /**
     * Valida que los equipos sean coherentes con la modalidad.
     *
     * @param modalidad modalidad de la partida
     * @param equipoA   primer equipo
     * @param equipoB   segundo equipo
     * @throws IllegalArgumentException si la validación falla
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

    // ═══════════════════════════════════════════════════════════
    // FÁBRICAS ESTÁTICAS
    // ═══════════════════════════════════════════════════════════

    /**
     * Crea una partida de Bola 8 individual con dos jugadores.
     *
     * @param id     identificador de la partida
     * @param j1     primer jugador
     * @param j2     segundo jugador
     * @param mesa   mesa con las bolas en posición inicial
     * @return partida de Bola 8 individual
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
     *
     * @param id    identificador de la partida
     * @param eqA   primer equipo (debe tener 2 jugadores)
     * @param eqB   segundo equipo (debe tener 2 jugadores)
     * @param mesa  mesa con las bolas en posición inicial
     * @return partida de Por Puntos en parejas
     */
    public static Partida parejas(String id, Equipo eqA, Equipo eqB, Mesa mesa) {
        return new Partida(id, TipoModalidad.POR_PUNTOS, eqA, eqB,
            mesa, Instant.now());
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS
    // ═══════════════════════════════════════════════════════════

    public String getId() {
        return id;
    }

    public TipoModalidad getModalidad() {
        return modalidad;
    }

    public Equipo getEquipoA() {
        return equipoA;
    }

    public Equipo getEquipoB() {
        return equipoB;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    /**
     * Retorna el estado actual de la partida.
     *
     * @return estado de la partida
     */
    public MatchState getEstado() {
        return estado;
    }

    /**
     * Retorna la fase actual de la partida.
     *
     * @return fase actual
     */
    public FasePartida getFase() {
        return estado.getFase();
    }

    /**
     * Retorna el ganador de la partida, si ya terminó.
     *
     * @return ganador, o {@code null} si aún no hay
     */
    public Jugador getGanador() {
        return estado.getGanador();
    }

    /**
     * Retorna los dos equipos participantes.
     *
     * @return lista inmutable con los dos equipos
     */
    public List<Equipo> getEquipos() {
        return List.of(equipoA, equipoB);
    }

    // ═══════════════════════════════════════════════════════════
    // OPERACIONES DELEGADAS AL ESTADO
    // ═══════════════════════════════════════════════════════════

    /**
     * Inicia la partida.
     *
     * @throws IllegalStateException si no está en fase CREADA
     */
    public void iniciar() {
        estado.iniciar();
    }

    /**
     * Pausa la partida.
     *
     * @throws IllegalStateException si no está en curso
     */
    public void pausar() {
        estado.pausar();
    }

    /**
     * Reanuda la partida pausada.
     *
     * @throws IllegalStateException si no está pausada
     */
    public void reanudar() {
        estado.reanudar();
    }

    /**
     * Finaliza la partida declarando un ganador.
     *
     * @param ganador jugador ganador
     */
    public void finalizar(Jugador ganador) {
        estado.finalizar(ganador);
    }

    /**
     * Abandona la partida sin ganador.
     */
    public void abandonar() {
        estado.abandonar();
    }

    /**
     * Aplica un tiro a la partida.
     *
     * @param evento    evento registrado
     * @param resultado veredicto del motor de reglas
     */
    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) {
        estado.aplicarTiro(evento, resultado);
    }

    /**
     * Verifica si la partida está terminada.
     *
     * @return {@code true} si finalizó o fue abandonada
     */
    public boolean estaTerminada() {
        return estado.estaTerminada();
    }

    /**
     * Verifica si la partida está activa (en curso o pausada).
     *
     * @return {@code true} si está activa
     */
    public boolean estaActiva() {
        return estado.estaActiva();
    }

    /**
     * Retorna el jugador que tiene el turno actual.
     *
     * @return jugador actual
     */
    public Jugador jugadorActual() {
        return estado.jugadorActual();
    }

    // ═══════════════════════════════════════════════════════════
    // IGUALDAD Y TOSTRING
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Partida that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

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