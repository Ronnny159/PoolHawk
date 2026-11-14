package com.poolhawk.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partidas")
public class PartidaEntity {
    @Id
    private String id;
    private String modalidad;
    private String fase;
    private Instant fechaCreacion;
    private String ganadorId;
    private String formato;

    @Column(name = "estado_partida_json", columnDefinition = "TEXT", length = 10000)
    private String estadoPartidaJson;

    @Column(name = "estado_mesa_json", columnDefinition = "TEXT", length = 10000)
    private String estadoMesaJson;

    @Version
    private Long version;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn(name = "orden")
    private List<JugadorEntity> jugadores;

    /**
     * Historial durable de tiros. Es la fuente de verdad para la
     * idempotencia tras un reinicio del servidor.
     *
     * <p>{@code orphanRemoval = true} garantiza que, al regenerar la
     * lista completa desde {@code MatchPersistenceMapper.toEntity},
     * los tiros eliminados del dominio se borran también de la BD.</p>
     *
     * <p>{@code @OrderColumn("orden")} preserva el orden de ejecución,
     * necesario para reconstruir el historial de forma determinista.</p>
     */
    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderColumn(name = "orden")
    private List<TiroEntity> tiros = new ArrayList<>();

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getModalidad() { return modalidad; }
    public void setModalidad(String modalidad) { this.modalidad = modalidad; }
    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getGanadorId() { return ganadorId; }
    public void setGanadorId(String ganadorId) { this.ganadorId = ganadorId; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public String getEstadoPartidaJson() { return estadoPartidaJson; }
    public void setEstadoPartidaJson(String estadoPartidaJson) { this.estadoPartidaJson = estadoPartidaJson; }
    public String getEstadoMesaJson() { return estadoMesaJson; }
    public void setEstadoMesaJson(String estadoMesaJson) { this.estadoMesaJson = estadoMesaJson; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<JugadorEntity> getJugadores() { return jugadores; }
    public void setJugadores(List<JugadorEntity> jugadores) { this.jugadores = jugadores; }
    public List<TiroEntity> getTiros() { return tiros; }
    public void setTiros(List<TiroEntity> tiros) { this.tiros = tiros; }
}