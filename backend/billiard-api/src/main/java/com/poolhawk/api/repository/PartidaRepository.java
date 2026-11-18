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
 *
 * <p>Los métodos {@code *ConHistorial} usan {@code @EntityGraph} para forzar
 * el fetch eager de la colección {@code tiros} en una sola consulta, evitando
 * {@code LazyInitializationException} cuando la entidad se mapea fuera de la
 * transacción original y sin depender de Open Entity Manager in View.</p>
 *
 * <p>No se fetchea {@code jugadores} en el {@code EntityGraph} porque esa
 * relación ya es {@code EAGER} y añadir dos colecciones simultáneamente
 * provocaría {@code MultipleBagFetchException} en Hibernate.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Repository
public interface PartidaRepository extends JpaRepository<PartidaEntity, String> {

    /**
     * Busca una partida por id cargando su historial de tiros.
     *
     * @param id identificador de la partida
     * @return la partida con {@code tiros} inicializado, o vacío si no existe
     */
    @EntityGraph(attributePaths = {"tiros"})
    @Query("SELECT p FROM PartidaEntity p WHERE p.id = :id")
    Optional<PartidaEntity> findByIdConHistorial(@Param("id") String id);

    /**
     * Lista todas las partidas cargando su historial de tiros.
     *
     * @return lista de partidas con {@code tiros} inicializado
     */
    @EntityGraph(attributePaths = {"tiros"})
    @Query("SELECT p FROM PartidaEntity p")
    List<PartidaEntity> findAllConHistorial();
}