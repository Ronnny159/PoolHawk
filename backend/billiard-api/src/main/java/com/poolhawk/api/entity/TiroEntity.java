package com.poolhawk.api.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Entidad JPA que persiste un {@code TiroEvent} del dominio junto con su
 * {@code ResultadoTiro}. Es la fuente durable de verdad para la idempotencia.
 *
 * <p>Todos los campos JSON son {@code nullable = false}: un tiro persistido
 * sin sus bolas, sin sus troneras o sin su resultado se considera corrupción
 * y la reconstrucción fallará de forma explícita con
 * {@link com.poolhawk.api.exception.PartidaCorruptaException}.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Entity
@Table(
    name = "tiros",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tiros_partida_event",
        columnNames = {"partida_id", "event_id"}
    )
)
public class TiroEntity {

    /** Identificador del evento (= eventId del dominio). */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** Identificador idempotente del tiro, recibido del cliente. */
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    /** Partida a la que pertenece el tiro. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false, updatable = false)
    private PartidaEntity partida;

    /** Jugador que ejecutó el tiro. */
    @Column(name = "jugador_id", nullable = false)
    private String jugadorId;

    /** Número de la primera bola tocada, o {@code null} si no tocó ninguna. */
    @Column(name = "primera_bola_tocada")
    private Integer primeraBolaTocada;

    /** Lista de números de bola embolsadas, serializada como JSON. */
    @Column(name = "bolas_embolsadas_json", columnDefinition = "TEXT", nullable = false)
    private String bolasEmbolsadasJson;

    /** Lista de nombres de tronera, serializada como JSON. */
    @Column(name = "troneras_json", columnDefinition = "TEXT", nullable = false)
    private String tronerasJson;

    /** Momento en que ocurrió el tiro. */
    @Column(name = "timestamp_tiro", nullable = false)
    private Instant timestamp;

    /** Duración del tiro en segundos. */
    @Column(name = "duracion_segundos", nullable = false)
    private Long duracionSegundos;

    /** Veredicto del motor de reglas, serializado como JSON. Obligatorio. */
    @Column(name = "resultado_json", columnDefinition = "TEXT", nullable = false)
    private String resultadoJson;

    public TiroEntity() {
        // Requerido por JPA
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public PartidaEntity getPartida() { return partida; }
    public void setPartida(PartidaEntity partida) { this.partida = partida; }

    public String getJugadorId() { return jugadorId; }
    public void setJugadorId(String jugadorId) { this.jugadorId = jugadorId; }

    public Integer getPrimeraBolaTocada() { return primeraBolaTocada; }
    public void setPrimeraBolaTocada(Integer primeraBolaTocada) { this.primeraBolaTocada = primeraBolaTocada; }

    public String getBolasEmbolsadasJson() { return bolasEmbolsadasJson; }
    public void setBolasEmbolsadasJson(String bolasEmbolsadasJson) { this.bolasEmbolsadasJson = bolasEmbolsadasJson; }

    public String getTronerasJson() { return tronerasJson; }
    public void setTronerasJson(String tronerasJson) { this.tronerasJson = tronerasJson; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Long getDuracionSegundos() { return duracionSegundos; }
    public void setDuracionSegundos(Long duracionSegundos) { this.duracionSegundos = duracionSegundos; }

    public String getResultadoJson() { return resultadoJson; }
    public void setResultadoJson(String resultadoJson) { this.resultadoJson = resultadoJson; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TiroEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "TiroEntity{id='" + id + "', eventId='" + eventId + "'}";
    }
}