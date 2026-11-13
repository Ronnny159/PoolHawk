package com.poolhawk.domain.match;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Mesa;

import java.time.Instant;
import java.util.*;

/**
 * Estado completo de una partida de billar en un momento dado.
 * 
 * <p>Recibe los equipos explícitamente para que {@link #getCompanero(Jugador)}
 * consulte la pertenencia real, no el índice en la rotación de turnos.</p>
 */
public class MatchState {

    private final String partidaId;
    private final TipoModalidad modalidad;
    private final Equipo equipoA;
    private final Equipo equipoB;
    private final Turnos<Jugador> turnos;
    private final Mesa mesa;
    private final Instant fechaInicio;

    private FasePartida fase;
    private final Map<Jugador, Integer> puntajes;
    private final Map<Jugador, Grupo> gruposAsignados;
    private final List<TiroEvent> historial;
    private Jugador ganador;

    public MatchState(String partidaId, TipoModalidad modalidad, 
                      Equipo equipoA, Equipo equipoB, 
                      Turnos<Jugador> turnos, Mesa mesa, Instant fechaInicio) {
        Objects.requireNonNull(partidaId, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(equipoA, "El equipo A no puede ser nulo");
        Objects.requireNonNull(equipoB, "El equipo B no puede ser nulo");
        Objects.requireNonNull(turnos, "El gestor de turnos no puede ser nulo");
        Objects.requireNonNull(mesa, "La mesa no puede ser nula");
        Objects.requireNonNull(fechaInicio, "La fecha de inicio no puede ser nula");

        if (partidaId.isBlank()) {
            throw new IllegalArgumentException("El id de la partida no puede estar en blanco");
        }

        this.partidaId = partidaId;
        this.modalidad = modalidad;
        this.equipoA = equipoA;
        this.equipoB = equipoB;
        this.turnos = turnos;
        this.mesa = mesa;
        this.fechaInicio = fechaInicio;
        this.fase = FasePartida.CREADA;
        this.puntajes = new HashMap<>();
        this.gruposAsignados = new HashMap<>();
        this.historial = new ArrayList<>();
        this.ganador = null;

        for (Jugador j : turnos.jugadores()) {
            puntajes.put(j, 0);
            gruposAsignados.put(j, Grupo.SIN_ASIGNAR);
        }
    }

    // GETTERS Y CONSULTAS
    public String getPartidaId() { return partidaId; }
    public TipoModalidad getModalidad() { return modalidad; }
    public Equipo getEquipoA() { return equipoA; }
    public Equipo getEquipoB() { return equipoB; }
    public Turnos<Jugador> getTurnos() { return turnos; }
    public Mesa getMesa() { return mesa; }
    public Instant getFechaInicio() { return fechaInicio; }
    public FasePartida getFase() { return fase; }
    public Jugador getGanador() { return ganador; }

    public Jugador jugadorActual() { return turnos.jugadorActual(); }

    public int puntajeDe(Jugador jugador) { return puntajes.getOrDefault(jugador, 0); }
    public Grupo grupoDe(Jugador jugador) { return gruposAsignados.getOrDefault(jugador, Grupo.SIN_ASIGNAR); }
    public Map<Jugador, Integer> getPuntajes() { return Collections.unmodifiableMap(puntajes); }
    public Map<Jugador, Grupo> getGruposAsignados() { return Collections.unmodifiableMap(gruposAsignados); }
    public List<TiroEvent> getHistorial() { return Collections.unmodifiableList(historial); }
    public int cantidadTiros() { return historial.size(); }

    public boolean estaTerminada() { return fase.esTerminal(); }
    public boolean estaActiva() { return fase.estaActiva(); }

    public Jugador getCompanero(Jugador jugador) {
        if (equipoA.contiene(jugador)) {
            List<Jugador> miembros = equipoA.getJugadores();
            return miembros.size() == 1 ? jugador : miembros.stream().filter(j -> !j.equals(jugador)).findFirst().orElse(jugador);
        }
        if (equipoB.contiene(jugador)) {
            List<Jugador> miembros = equipoB.getJugadores();
            return miembros.size() == 1 ? jugador : miembros.stream().filter(j -> !j.equals(jugador)).findFirst().orElse(jugador);
        }
        return jugador;
    }

    public boolean quedanBolasDeGrupo(Grupo grupo) {
        if (!grupo.estaAsignado()) {
            throw new IllegalArgumentException("El grupo debe estar asignado (LISAS o RAYADAS)");
        }
        return mesa.quedanBolasDeGrupo(grupo.getTipoBolaAsociado());
    }

    public boolean jugadorLimpioSuGrupo(Jugador jugador) {
        Grupo grupo = grupoDe(jugador);
        if (!grupo.estaAsignado()) return false;
        return !quedanBolasDeGrupo(grupo);
    }

    // MODIFICACIÓN DE ESTADO
    public void iniciar() {
        if (fase != FasePartida.CREADA) {
            throw new IllegalStateException("Solo se puede iniciar una partida en fase CREADA. Fase actual: " + fase);
        }
        fase = FasePartida.EN_CURSO;
    }

    public void pausar() {
        if (fase != FasePartida.EN_CURSO) {
            throw new IllegalStateException("Solo se puede pausar una partida EN_CURSO. Fase actual: " + fase);
        }
        fase = FasePartida.PAUSADA;
    }

    public void reanudar() {
        if (fase != FasePartida.PAUSADA) {
            throw new IllegalStateException("Solo se puede reanudar una partida PAUSADA. Fase actual: " + fase);
        }
        fase = FasePartida.EN_CURSO;
    }

    public void finalizar(Jugador ganador) {
        Objects.requireNonNull(ganador, "El ganador no puede ser nulo");
        if (estaTerminada()) {
            throw new IllegalStateException("La partida ya está terminada en fase: " + fase);
        }
        if (turnos.posicionDe(ganador) < 0) {
            throw new IllegalArgumentException("El ganador " + ganador.nombre() + " no participa en esta partida");
        }
        this.ganador = ganador;
        this.fase = FasePartida.FINALIZADA;
    }

    public void abandonar() {
        if (estaTerminada()) {
            throw new IllegalStateException("La partida ya está terminada en fase: " + fase);
        }
        this.fase = FasePartida.ABANDONADA;
    }

    public void sumarPuntos(Jugador jugador, int puntos) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos a sumar no pueden ser negativos. Valor recibido: " + puntos);
        }
        if (turnos.posicionDe(jugador) < 0) {
            throw new IllegalArgumentException("El jugador " + jugador.nombre() + " no participa en esta partida");
        }
        puntajes.merge(jugador, puntos, Integer::sum);
    }

    public void asignarGrupo(Jugador jugador, Grupo grupo) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        Objects.requireNonNull(grupo, "El grupo no puede ser nulo");

        if (!grupo.estaAsignado()) {
            throw new IllegalArgumentException("No se puede asignar el grupo SIN_ASIGNAR");
        }
        if (grupoDe(jugador).estaAsignado()) {
            throw new IllegalArgumentException("El jugador " + jugador.nombre() + " ya tiene asignado el grupo " + grupoDe(jugador));
        }
        gruposAsignados.put(jugador, grupo);
    }

    public void avanzarTurno() { turnos.avanzar(); }

    public void registrarTiro(TiroEvent evento) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        historial.add(evento);
    }

    /**
     * Aplica un resultado de tiro al estado.
     * 
     * <p>CORRECCIÓN: Las bolas embolsadas se aplican SIEMPRE, incluso si el resultado es falta o derrota.
     * El turno no avanza si el resultado es victoria o derrota terminal.</p>
     */
    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        Objects.requireNonNull(resultado, "El resultado no puede ser nulo");

        if (fase != FasePartida.EN_CURSO) {
            throw new IllegalStateException("Solo se pueden aplicar tiros a una partida EN_CURSO. Fase actual: " + fase);
        }

        registrarTiro(evento);

        // 1. Aplicar grupo si el tiro lo asignó
        if (resultado.grupoAsignado() != null) {
            Jugador tirador = evento.jugador();
            Jugador oponente = equipoA.contiene(tirador) 
                ? equipoB.getJugadores().get(0) 
                : equipoA.getJugadores().get(0);

            if (!grupoDe(tirador).estaAsignado()) {
                gruposAsignados.put(tirador, resultado.grupoAsignado());
            }
            if (!grupoDe(oponente).estaAsignado()) {
                gruposAsignados.put(oponente, resultado.grupoAsignado().opuesto());
            }
        }

        // 2. Aplicar bolas embolsadas (SIEMPRE, incluso en derrota o falta)
        for (Bola bola : evento.bolasEmbolsadas()) {
            if (bola.enMesa()) {
                mesa.embolsarBola(bola.numero());
            }
        }

        // 3. Sumar puntos
        if (resultado.puntos() > 0) {
            sumarPuntos(evento.jugador(), resultado.puntos());
        }

        // 4. Cambiar turno SOLO si no es victoria ni derrota
        if (resultado.cambiaTurno() && !resultado.esVictoria() && !resultado.esDerrota()) {
            avanzarTurno();
        }
    }

    public void restaurarEstado(FasePartida faseGuardada, Jugador ganadorGuardado, Map<Jugador, Integer> puntajesGuardados, Map<Jugador, Grupo> gruposGuardados) {
        this.fase = faseGuardada;
        this.ganador = ganadorGuardado;
        
        if (puntajesGuardados != null) {
            this.puntajes.putAll(puntajesGuardados);
        }
        if (gruposGuardados != null) {
            this.gruposAsignados.putAll(gruposGuardados);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchState that)) return false;
        return partidaId.equals(that.partidaId);
    }

    @Override
    public int hashCode() { return Objects.hash(partidaId); }

    @Override
    public String toString() {
        return "MatchState{" + "partidaId='" + partidaId + '\'' + ", modalidad=" + modalidad + ", fase=" + fase + ", jugadorActual=" + (turnos != null ? turnos.jugadorActual().nombre() : "N/A") + ", puntajes=" + puntajes + '}';
    }
}