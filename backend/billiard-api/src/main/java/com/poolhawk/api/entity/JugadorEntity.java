package com.poolhawk.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "jugadores")
public class JugadorEntity {
    @Id
    private String id;
    private String nombre;
    private String equipo;

    @ManyToOne
    @JoinColumn(name = "partida_id")
    private PartidaEntity partida;

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEquipo() { return equipo; }
    public void setEquipo(String equipo) { this.equipo = equipo; }
    public PartidaEntity getPartida() { return partida; }
    public void setPartida(PartidaEntity partida) { this.partida = partida; }
}