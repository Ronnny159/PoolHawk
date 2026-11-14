package com.poolhawk.domain.match;

import com.poolhawk.domain.event.FaltaMotivo;
import com.poolhawk.domain.event.ResultadoTiro;
import com.poolhawk.domain.event.TiroEvent;
import com.poolhawk.domain.player.Equipo;
import com.poolhawk.domain.player.Jugador;
import com.poolhawk.domain.table.Bola;
import com.poolhawk.domain.table.Mesa;

import java.time.Instant;
import java.util.*;

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
    private final Map<String, ResultadoTiro> resultadosPorEvento;
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
        this.resultadosPorEvento = new HashMap<>();
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

    /**
     * Devuelve el veredicto original asociado a un {@code eventId}, si ya fue procesado.
     *
     * @param eventId identificador idempotente del tiro
     * @return el {@link ResultadoTiro} original, o {@link Optional#empty()} si es nuevo
     */
    public Optional<ResultadoTiro> buscarResultadoPorEvento(String eventId) {
        if (eventId == null || eventId.isBlank()) return Optional.empty();
        return Optional.ofNullable(resultadosPorEvento.get(eventId));
    }

    /**
     * @return mapa inmutable {@code eventId -> ResultadoTiro} de todos los tiros procesados
     */
    public Map<String, ResultadoTiro> getResultadosPorEvento() {
        return Collections.unmodifiableMap(resultadosPorEvento);
    }

    /**
     * Devuelve los números de las bolas que siguen sobre la mesa (incluida la blanca, id 0).
     *
     * @return lista ordenada de números de bola presentes en la mesa
     */
    public List<Integer> bolasEnMesa() {
        List<Integer> resultado = new ArrayList<>();
        for (int numero = 0; numero <= 15; numero++) {
            int n = numero;
            mesa.buscarBola(n).filter(Bola::enMesa).ifPresent(b -> resultado.add(n));
        }
        return resultado;
    }

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
        if (fase != FasePartida.CREADA) throw new IllegalStateException("Solo se puede iniciar en CREADA.");
        fase = FasePartida.EN_CURSO;
    }

    public void pausar() {
        if (fase != FasePartida.EN_CURSO) throw new IllegalStateException("Solo se puede pausar en EN_CURSO.");
        fase = FasePartida.PAUSADA;
    }

    public void reanudar() {
        if (fase != FasePartida.PAUSADA) throw new IllegalStateException("Solo se puede reanudar en PAUSADA.");
        fase = FasePartida.EN_CURSO;
    }

    public void finalizar(Jugador ganador) {
        Objects.requireNonNull(ganador, "El ganador no puede ser nulo");
        if (estaTerminada()) throw new IllegalStateException("La partida ya está terminada.");
        if (turnos.posicionDe(ganador) < 0) throw new IllegalArgumentException("El ganador no participa.");
        this.ganador = ganador;
        this.fase = FasePartida.FINALIZADA;
    }

    public void abandonar() {
        if (estaTerminada()) throw new IllegalStateException("La partida ya está terminada.");
        this.fase = FasePartida.ABANDONADA;
    }

    public void sumarPuntos(Jugador jugador, int puntos) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        if (puntos < 0) throw new IllegalArgumentException("Puntos negativos");
        if (turnos.posicionDe(jugador) < 0) throw new IllegalArgumentException("El jugador no participa");
        puntajes.merge(jugador, puntos, Integer::sum);
    }

    public void asignarGrupo(Jugador jugador, Grupo grupo) {
        Objects.requireNonNull(jugador, "El jugador no puede ser nulo");
        Objects.requireNonNull(grupo, "El grupo no puede ser nulo");
        if (!grupo.estaAsignado()) throw new IllegalArgumentException("No se puede asignar SIN_ASIGNAR");
        if (grupoDe(jugador).estaAsignado()) throw new IllegalArgumentException("El jugador ya tiene grupo");
        gruposAsignados.put(jugador, grupo);
    }

    public void avanzarTurno() { turnos.avanzar(); }

    public void registrarTiro(TiroEvent evento) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        historial.add(evento);
    }

    /**
     * Aplica un tiro ya evaluado. Almacena también el {@link ResultadoTiro} indexado por
     * {@code eventId} para garantizar idempotencia transparente en reintentos posteriores.
     *
     * @param evento    evento validado del tiro
     * @param resultado veredicto devuelto por el motor de reglas
     */
    public void aplicarTiro(TiroEvent evento, ResultadoTiro resultado) {
        Objects.requireNonNull(evento, "El evento no puede ser nulo");
        Objects.requireNonNull(resultado, "El resultado no puede ser nulo");

        if (fase != FasePartida.EN_CURSO) {
            throw new IllegalStateException("Solo se aplican tiros en EN_CURSO.");
        }

        registrarTiro(evento);

        if (resultado.grupoAsignado() != null) {
            Jugador tirador = evento.jugador();
            Jugador oponente = equipoA.contiene(tirador)
                ? equipoB.getJugadores().get(0)
                : equipoA.getJugadores().get(0);

            if (!grupoDe(tirador).estaAsignado()) gruposAsignados.put(tirador, resultado.grupoAsignado());
            if (!grupoDe(oponente).estaAsignado()) gruposAsignados.put(oponente, resultado.grupoAsignado().opuesto());
        }

        for (Bola bola : evento.bolasEmbolsadas()) {
            if (resultado.esFalta() && resultado.motivo() == FaltaMotivo.BLANCA_EMBOLSADA && bola.esBlanca()) {
                continue;
            }
            if (bola.enMesa()) {
                mesa.embolsarBola(bola.numero());
            }
        }

        if (resultado.puntos() > 0) sumarPuntos(evento.jugador(), resultado.puntos());

        if (resultado.cambiaTurno() && !resultado.esVictoria() && !resultado.esDerrota()) {
            avanzarTurno();
        }

        resultadosPorEvento.put(evento.id(), resultado);
    }

    public void restaurarEstado(FasePartida faseGuardada, Jugador ganadorGuardado, Map<Jugador, Integer> puntajesGuardados, Map<Jugador, Grupo> gruposGuardados) {
        this.fase = faseGuardada;
        this.ganador = ganadorGuardado;
        if (puntajesGuardados != null) this.puntajes.putAll(puntajesGuardados);
        if (gruposGuardados != null) this.gruposAsignados.putAll(gruposGuardados);
    }

    /**
     * Inyecta el historial y los resultados reconstruidos desde la base de datos
     * <b>sin re-aplicar</b> los tiros. Debe llamarse únicamente después de
     * {@link #restaurarEstado(FasePartida, Jugador, Map, Map)} para evitar
     * doble mutación sobre puntajes, grupos y turnos.
     *
     * <p>Se usa en la rehidratación desde JPA para que la idempotencia sobreviva
     * a un reinicio del servidor: los {@code eventId} ya procesados quedan
     * indexados en {@code resultadosPorEvento} y los reintentos devuelven el
     * {@link ResultadoTiro} original.</p>
     *
     * @param historialRestaurado lista de {@link TiroEvent} en orden de ejecución
     * @param resultados          mapa {@code eventId -> ResultadoTiro}
     */
    public void restaurarHistorial(List<TiroEvent> historialRestaurado,
                                   Map<String, ResultadoTiro> resultados) {
        Objects.requireNonNull(historialRestaurado, "El historial no puede ser nulo");
        Objects.requireNonNull(resultados, "Los resultados no pueden ser nulos");

        this.historial.clear();
        this.historial.addAll(historialRestaurado);
        this.resultadosPorEvento.clear();
        this.resultadosPorEvento.putAll(resultados);
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
        return "MatchState{" + "partidaId='" + partidaId + '\'' + ", fase=" + fase + '}';
    }
}