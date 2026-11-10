package com.poolhawk.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

/**
 * Entidad JPA que representa una partida en la base de datos.
 */
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

    // NUEVO: Guarda el estado completo del juego (puntajes, grupos, turno actual)
    @Column(name = "estado_partida_json", columnDefinition = "TEXT", length = 10000)
    private String estadoPartidaJson;

    @Column(name = "estado_mesa_json", columnDefinition = "TEXT", length = 10000)
    private String estadoMesaJson;

    // NUEVO: Control de concurrencia optimista
    @Version
    private Long version;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<JugadorEntity> jugadores;

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
}