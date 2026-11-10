package com.poolhawk.domain.match;

import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Mesa;
import com.poolhawk.domain.table.Bola;

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
 * <p>Contiene la información de quién juega, los puntajes, los grupos 
 * asignados y las bolas en la mesa. Es un objeto mutable que el motor 
 * de reglas modifica al aplicar cada {@link ResultadoTiro}.</p>
 * 
 * @author PoolHawk Team
 * @since 0.1.0
 */
public class MatchState {

    private final String partidaId;
    private final TipoModalidad modalidad;
    private final Turnos<Jugador> turnos; 
    private final Mesa mesa;
    private final Instant fechaInicio;

    private FasePartida fase;
    private final Map<Jugador, Integer> puntajes;
    private final Map<Jugador, Grupo> gruposAsignados;
    private final List<TiroEvent> historial;
    private Jugador ganador;

    public MatchState(String partidaId, TipoModalidad modalidad, Turnos<Jugador> turnos, Mesa mesa, Instant fechaInicio) {
        Objects.requireNonNull(partidaId, "El id de la partida no puede ser nulo");
        Objects.requireNonNull(modalidad, "La modalidad no puede ser nula");
        Objects.requireNonNull(turnos, "El gestor de turnos no puede ser nulo");
        Objects.requireNonNull(mesa, "La mesa no puede ser nula");
        Objects.requireNonNull(fechaInicio, "La fecha de inicio no puede ser nula");

        if (partidaId.isBlank()) {
            throw new IllegalArgumentException("El id de la partida no puede estar en blanco");
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

        for (Jugador j : turnos.jugadores()) {
            puntajes.put(j, 0);
            gruposAsignados.put(j, Grupo.SIN_ASIGNAR);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS Y CONSULTAS DE ESTADO
    // ═══════════════════════════════════════════════════════════

    public String getPartidaId() { return partidaId; }
    public TipoModalidad getModalidad() { return modalidad; }
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

    /**
     * Retorna el compañero de equipo de un jugador en formato Parejas.
     * Si el jugador no tiene compañero (individual), retorna el mismo jugador.
     */
    public Jugador getCompanero(Jugador jugador) {
        List<Jugador> jugadores = turnos.jugadores();
        int indice = jugadores.indexOf(jugador);
        
        if (indice == -1) return jugador;
        
        // Si hay 4 jugadores, asumimos que A=[0,2] y B=[1,3].
        // Si el índice es 0, el compañero es 2. Si es 2, es 0.
        // Si es 1, es 3. Si es 3, es 1.
        if (jugadores.size() == 4) {
            int indiceCompanero = (indice % 2 == 0) ? (indice + 2) % 4 : (indice - 1);
            return jugadores.get(indiceCompanero);
        }
        
        // En individual o 1v1, no hay compañero
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

    // ═══════════════════════════════════════════════════════════
    // MODIFICACIÓN DE ESTADO (TRANSICIONES)
    // ═══════════════════════════════════════════════════════════

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
     */
    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        Objects.requireNonNull(resultado, "El resultado no puede ser nulo");

        if (fase != FasePartida.EN_CURSO) {
            throw new IllegalStateException("Solo se pueden aplicar tiros a una partida EN_CURSO. Fase actual: " + fase);
        }

        registrarTiro(evento);

        if (!resultado.esFalta()) {
            for (Bola bola : evento.bolasEmbolsadas()) {
                if (bola.enMesa()) {
                    mesa.embolsarBola(bola.numero());
                }
            }
        }

        if (resultado.puntos() > 0) {
            sumarPuntos(evento.jugador(), resultado.puntos());
        }

        if (resultado.cambiaTurno()) {
            avanzarTurno();
        }
    }

    /**
     * Restaura el estado de una partida desde persistencia.
     * Esto es usado por el MatchPersistenceMapper.
     */
    public void restaurarEstado(FasePartida faseGuardada, Jugador ganadorGuardado, Map<Jugador, Integer> puntajesGuardados, Map<Jugador, Grupo> gruposGuardados, List<TiroEvent> historialGuardado) {
        this.fase = faseGuardada;
        this.ganador = ganadorGuardado;
        
        if (puntajesGuardados != null) {
            this.puntajes.putAll(puntajesGuardados);
        }
        if (gruposGuardados != null) {
            this.gruposAsignados.putAll(gruposGuardados);
        }
        if (historialGuardado != null) {
            this.historial.addAll(historialGuardado);
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