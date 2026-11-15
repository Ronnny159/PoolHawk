package com.poolhawk.api.repository;

import com.poolhawk.api.entity.PartidaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA de partidas.
 */
@Repository
public interface PartidaRepository extends JpaRepository<PartidaEntity, String> {

    /**
     * Busca una partida por id cargando su historial de tiros.
     */
    @EntityGraph(attributePaths = {"tiros"})
    @Query("SELECT p FROM PartidaEntity p WHERE p.id = :id")
    Optional<PartidaEntity> findByIdConHistorial(@Param("id") String id);

    /**
     * Lista todas las partidas cargando su historial de tiros.
     */
    @EntityGraph(attributePaths = {"tiros"})
    @Query("SELECT p FROM PartidaEntity p")
    List<PartidaEntity> findAllConHistorial();
}