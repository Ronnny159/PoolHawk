package com.poolhawk.domain.match;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Mesa;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Estado completo de una partida de billar en un momento dado.
 *
 * <p>Contiene toda la información necesaria para conocer en qué situación
 * está la partida: quién juega, cuántos puntos tiene cada jugador, qué
 * grupos están asignados (en Bola 8), qué bolas siguen en la mesa y qué
 * ha pasado hasta ahora.</p>
 *
 * <p><b>Este objeto es mutable.</b> El motor de reglas lo modifica al
 * aplicar cada {@link ResultadoTiro}. A diferencia de las entidades del
 * dominio como {@code Bola} o {@code Jugador}, el estado de la partida
 * cambia constantemente y crear una nueva instancia por cada tiro sería
 * ineficiente e innecesario.</p>
 *
 * <p>El {@code MatchState} NO conoce las reglas del billar. Es un
 * contenedor de estado. Las reglas viven en clases aparte
 * ({@code ReglasBola8}, {@code ReglasPorPuntos}).</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class MatchState {

    private final String partidaId;
    private final TipoModalidad modalidad;
    private final Turnos turnos;
    private final Mesa mesa;
    private final Instant fechaInicio;

    private FasePartida fase;
    private final Map<Jugador, Integer> puntajes;
    private final Map<Jugador, Grupo> gruposAsignados;
    private final List<TiroEvent> historial;
    private Jugador ganador;

    /**
     * Crea un nuevo estado de partida en fase CREADA.
     *
     * @param partidaId  identificador único de la partida
     * @param modalidad  modalidad de la partida
     * @param turnos     gestor de turnos con los jugadores ordenados
     * @param mesa       mesa con las bolas en su posición inicial
     * @param fechaInicio momento en que se creó la partida
     * @throws NullPointerException     si algún parámetro es nulo
     * @throws IllegalArgumentException si el partidaId está en blanco
     */
    public MatchState(String partidaId,
                      TipoModalidad modalidad,
                      Turnos turnos,
                      Mesa mesa,
                      Instant fechaInicio) {
        Objects.requireNonNull(partidaId, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(turnos, "El gestor de turnos no puede ser nulo");
        Objects.requireNonNull(mesa, "La mesa no puede ser nula");
        Objects.requireNonNull(fechaInicio, "La fecha de inicio no puede ser nula");

        if (partidaId.isBlank()) {
            throw new IllegalArgumentException(
                "El id de la partida no puede estar en blanco"
            );
        }

        this.partidaId = partidaId;
        this.modalidad = modalidad;
        this.turnos = turnos;
        this.mesa = mesa;
        this.fechaInicio = fechaInicio;
        this.fase = FasePartida.CREADA;
        this.puntajes = new HashMap<>();
        this.gruposAsignados = new HashMap<>();
        this.historial = new ArrayList<>();
        this.ganador = null;

        // Inicializar puntajes y grupos en cero/sin asignar
        for (Jugador j : turnos.jugadores()) {
            puntajes.put(j, 0);
            gruposAsignados.put(j, Grupo.SIN_ASIGNAR);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS BÁSICOS
    // ═══════════════════════════════════════════════════════════

    public String getPartidaId() {
        return partidaId;
    }

    public TipoModalidad getModalidad() {
        return modalidad;
    }

    public Turnos getTurnos() {
        return turnos;
    }

    public Mesa getMesa() {
        return mesa;
    }

    public Instant getFechaInicio() {
        return fechaInicio;
    }

    public FasePartida getFase() {
        return fase;
    }

    public Jugador getGanador() {
        return ganador;
    }

    // ═══════════════════════════════════════════════════════════
    // CONSULTAS DE ESTADO
    // ═══════════════════════════════════════════════════════════

    /**
     * Retorna el jugador que tiene el turno actual.
     *
     * @return jugador actual
     */
    public Jugador jugadorActual() {
        return turnos.jugadorActual();
    }

    /**
     * Retorna el puntaje actual de un jugador.
     *
     * @param jugador jugador a consultar
     * @return puntaje del jugador, o 0 si no tiene registro
     */
    public int puntajeDe(Jugador jugador) {
        return puntajes.getOrDefault(jugador, 0);
    }

    /**
     * Retorna el grupo asignado a un jugador.
     *
     * @param jugador jugador a consultar
     * @return grupo asignado, o {@link Grupo#SIN_ASIGNAR} si no tiene
     */
    public Grupo grupoDe(Jugador jugador) {
        return gruposAsignados.getOrDefault(jugador, Grupo.SIN_ASIGNAR);
    }

    /**
     * Retorna el mapa inmutable de puntajes.
     *
     * @return mapa jugador → puntaje
     */
    public Map<Jugador, Integer> getPuntajes() {
        return Collections.unmodifiableMap(puntajes);
    }

    /**
     * Retorna el mapa inmutable de grupos asignados.
     *
     * @return mapa jugador → grupo
     */
    public Map<Jugador, Grupo> getGruposAsignados() {
        return Collections.unmodifiableMap(gruposAsignados);
    }

    /**
     * Retorna una lista inmutable con el historial de tiros.
     *
     * @return historial de eventos
     */
    public List<TiroEvent> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    /**
     * Retorna la cantidad de tiros registrados.
     *
     * @return cantidad de eventos
     */
    public int cantidadTiros() {
        return historial.size();
    }

    /**
     * Verifica si la partida está terminada.
     *
     * @return {@code true} si está finalizada o abandonada
     */
    public boolean estaTerminada() {
        return fase.esTerminal();
    }

    /**
     * Verifica si la partida está activa (en curso o pausada).
     *
     * @return {@code true} si está activa
     */
    public boolean estaActiva() {
        return fase.estaActiva();
    }

    /**
     * Verifica si quedan bolas de un grupo en la mesa.
     *
     * @param grupo grupo a consultar (LISAS o RAYADAS)
     * @return {@code true} si quedan bolas de ese grupo
     * @throws IllegalArgumentException si el grupo es SIN_ASIGNAR
     */
    public boolean quedanBolasDeGrupo(Grupo grupo) {
        if (!grupo.estaAsignado()) {
            throw new IllegalArgumentException(
                "El grupo debe estar asignado (LISAS o RAYADAS)"
            );
        }
        return mesa.quedanBolasDeGrupo(grupo.getTipoBolaAsociado());
    }

    /**
     * Verifica si un jugador ya limpió su grupo asignado.
     *
     * @param jugador jugador a consultar
     * @return {@code true} si no le quedan bolas de su grupo
     */
    public boolean jugadorLimpioSuGrupo(Jugador jugador) {
        Grupo grupo = grupoDe(jugador);
        if (!grupo.estaAsignado()) {
            return false;
        }
        return !quedanBolasDeGrupo(grupo);
    }

    // ═══════════════════════════════════════════════════════════
    // MODIFICACIÓN DE ESTADO
    // ═══════════════════════════════════════════════════════════

    /**
     * Inicia la partida pasando de CREADA a EN_CURSO.
     *
     * @throws IllegalStateException si la partida no está en fase CREADA
     */
    public void iniciar() {
        if (fase != FasePartida.CREADA) {
            throw new IllegalStateException(
                "Solo se puede iniciar una partida en fase CREADA. " +
                "Fase actual: " + fase
            );
        }
        fase = FasePartida.EN_CURSO;
    }

    /**
     * Pausa la partida.
     *
     * @throws IllegalStateException si la partida no está en curso
     */
    public void pausar() {
        if (fase != FasePartida.EN_CURSO) {
            throw new IllegalStateException(
                "Solo se puede pausar una partida EN_CURSO. " +
                "Fase actual: " + fase
            );
        }
        fase = FasePartida.PAUSADA;
    }

    /**
     * Reanuda la partida pausada.
     *
     * @throws IllegalStateException si la partida no está pausada
     */
    public void reanudar() {
        if (fase != FasePartida.PAUSADA) {
            throw new IllegalStateException(
                "Solo se puede reanudar una partida PAUSADA. " +
                "Fase actual: " + fase
            );
        }
        fase = FasePartida.EN_CURSO;
    }

    /**
     * Finaliza la partida declarando un ganador.
     *
     * @param ganador jugador ganador
     * @throws NullPointerException  si el ganador es nulo
     * @throws IllegalStateException si la partida ya terminó
     * @throws IllegalArgumentException si el ganador no participa en la partida
     */
    public void finalizar(Jugador ganador) {
        Objects.requireNonNull(ganador, "El ganador no puede ser nulo");
        if (estaTerminada()) {
            throw new IllegalStateException(
                "La partida ya está terminada en fase: " + fase
            );
        }
        if (turnos.posicionDe(ganador) < 0) {
            throw new IllegalArgumentException(
                "El ganador " + ganador.nombre() + " no participa en esta partida"
            );
        }
        this.ganador = ganador;
        this.fase = FasePartida.FINALIZADA;
    }

    /**
     * Abandona la partida sin ganador.
     *
     * @throws IllegalStateException si la partida ya terminó
     */
    public void abandonar() {
        if (estaTerminada()) {
            throw new IllegalStateException(
                "La partida ya está terminada en fase: " + fase
            );
        }
        this.fase = FasePartida.ABANDONADA;
    }

    /**
     * Suma puntos al jugador indicado.
     *
     * @param jugador jugador al que sumar
     * @param puntos  puntos a sumar (debe ser positivo)
     * @throws IllegalArgumentException si los puntos son negativos
     *                                  o el jugador no participa
     */
    public void sumarPuntos(Jugador jugador, int puntos) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        if (puntos < 0) {
            throw new IllegalArgumentException(
                "Los puntos a sumar no pueden ser negativos. " +
                "Valor recibido: " + puntos
            );
        }
        if (turnos.posicionDe(jugador) < 0) {
            throw new IllegalArgumentException(
                "El jugador " + jugador.nombre() + " no participa en esta partida"
            );
        }
        puntajes.merge(jugador, puntos, Integer::sum);
    }

    /**
     * Asigna un grupo a un jugador.
     *
     * @param jugador jugador al que asignar
     * @param grupo   grupo a asignar (LISAS o RAYADAS)
     * @throws NullPointerException     si algún parámetro es nulo
     * @throws IllegalArgumentException si el grupo es SIN_ASIGNAR,
     *                                  o si el jugador ya tiene un grupo asignado
     */
    public void asignarGrupo(Jugador jugador, Grupo grupo) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        Objects.requireNonNull(grupo, "El grupo no puede ser nulo");

        if (!grupo.estaAsignado()) {
            throw new IllegalArgumentException(
                "No se puede asignar el grupo SIN_ASIGNAR"
            );
        }

        if (grupoDe(jugador).estaAsignado()) {
            throw new IllegalArgumentException(
                "El jugador " + jugador.nombre() + " ya tiene asignado el grupo " +
                grupoDe(jugador)
            );
        }

        gruposAsignados.put(jugador, grupo);
    }

    /**
     * Avanza el turno al siguiente jugador.
     */
    public void avanzarTurno() {
        turnos.avanzar();
    }

    /**
     * Registra un tiro en el historial.
     *
     * @param evento evento a registrar
     * @throws NullPointerException si el evento es nulo
     */
    public void registrarTiro(TiroEvent evento) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        historial.add(evento);
    }

    /**
     * Aplica un resultado de tiro al estado: suma puntos, cambia turno, etc.
     *
     * <p>Este método NO evalúa el tiro (eso es responsabilidad del motor de
     * reglas). Solo aplica las consecuencias ya decididas.</p>
     *
     * @param evento   evento registrado
     * @param resultado veredicto del motor de reglas
     * @throws NullPointerException  si algún parámetro es nulo
     * @throws IllegalStateException si la partida no está activa
     */
    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        Objects.requireNonNull(resultado, "El resultado no puede ser nulo");

        if (!estaActiva()) {
            throw new IllegalStateException(
                "Solo se pueden aplicar tiros a una partida activa. " +
                "Fase actual: " + fase
            );
        }

        registrarTiro(evento);

        if (resultado.puntos() > 0) {
            sumarPuntos(evento.jugador(), resultado.puntos());
        }

        if (resultado.cambiaTurno()) {
            avanzarTurno();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // IGUALDAD Y TOSTRING
    // ═══════════════════════════════════════════════════════════

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchState that)) return false;
        return partidaId.equals(that.partidaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(partidaId);
    }

    @Override
    public String toString() {
        return "MatchState{" +
            "partidaId='" + partidaId + '\'' +
            ", modalidad=" + modalidad +
            ", fase=" + fase +
            ", jugadorActual=" + (turnos != null ? turnos.jugadorActual().nombre() : "N/A") +
            ", puntajes=" + puntajes +
            '}';
    }
}