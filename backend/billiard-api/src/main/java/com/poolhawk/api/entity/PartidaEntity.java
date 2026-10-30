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

    // NUEVO: Campo para guardar el JSON de la mesa
    @Column(columnDefinition = "TEXT")
    private String estadoMesaJson;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JugadorEntity> jugadores = new ArrayList<>();

    // Getters y Setters (si usas Lombok, omite esto y usa @Getter @Setter)
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
    
    // Getters y Setters del nuevo campo
    public String getEstadoMesaJson() { return estadoMesaJson; }
    public void setEstadoMesaJson(String estadoMesaJson) { this.estadoMesaJson = estadoMesaJson; }

    public List<JugadorEntity> getJugadores() { return jugadores; }
    public void setJugadores(List<JugadorEntity> jugadores) { this.jugadores = jugadores; }
}