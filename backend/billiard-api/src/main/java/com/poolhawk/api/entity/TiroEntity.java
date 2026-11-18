package com.poolhawk.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad JPA que persiste un tiro.
 * El ID es interno (UUID generado en servidor), el eventId es para idempotencia.
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

    /** ID interno generado por el servidor. */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /** ID de idempotencia enviado por el cliente. */
    @Column(name = "event_id", nullable = false, updatable = false)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id", nullable = false, updatable = false)
    private PartidaEntity partida;

    @Column(name = "jugador_id", nullable = false)
    private String jugadorId;

    @Column(name = "primera_bola_tocada")
    private Integer primeraBolaTocada;

    @Column(name = "bolas_embolsadas_json", columnDefinition = "TEXT", nullable = false)
    private String bolasEmbolsadasJson;

    @Column(name = "troneras_json", columnDefinition = "TEXT", nullable = false)
    private String tronerasJson;

    @Column(name = "timestamp_tiro", nullable = false)
    private Instant timestamp;

    @Column(name = "duracion_segundos", nullable = false)
    private Long duracionSegundos;

    @Column(name = "resultado_json", columnDefinition = "TEXT", nullable = false)
    private String resultadoJson;

    public TiroEntity() {}

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
}