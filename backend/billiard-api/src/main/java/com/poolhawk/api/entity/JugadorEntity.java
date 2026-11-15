package com.poolhawk.api.entity;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa a un jugador dentro de una partida.
 */
@Entity
@Table(name = "jugadores")
public class JugadorEntity {

    @Id
    private String id;
    
    private String nombre;
    
    @Column(name = "equipo")
    private String equipo; // "A" o "B"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partida_id")
    private PartidaEntity partida;

    public JugadorEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getEquipo() { return equipo; }
    public void setEquipo(String equipo) { this.equipo = equipo; }
    
    public PartidaEntity getPartida() { return partida; }
    public void setPartida(PartidaEntity partida) { this.partida = partida; }
}