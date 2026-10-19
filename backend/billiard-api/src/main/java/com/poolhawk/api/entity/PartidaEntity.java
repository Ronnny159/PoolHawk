package com.poolhawk.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "partidas")
public class PartidaEntity {
    @Id
    private String id;
    private String modalidad;
    private String fase;
    private Instant fechaCreacion;
    private String ganadorId;

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
    public List<JugadorEntity> getJugadores() { return jugadores; }
    public void setJugadores(List<JugadorEntity> jugadores) { this.jugadores = jugadores; }
}