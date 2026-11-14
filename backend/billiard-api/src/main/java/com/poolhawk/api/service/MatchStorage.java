package com.poolhawk.api.service;

import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.repository.PartidaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Almacén de partidas en la base de datos.
 * Usa {@code EntityManager.merge()} para actualizaciones seguras con {@code @Version}.
 */
@Component
public class MatchStorage {

    private final PartidaRepository partidaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public MatchStorage(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

    /**
     * Guarda una partida. Si existe, la actualiza (merge) respetando la versión.
     * Si no existe, la inserta.
     *
     * @param partida entidad a persistir
     */
    public void guardar(PartidaEntity partida) {
        entityManager.merge(partida);
    }

    /**
     * Busca una partida por id sin cargar su historial de tiros.
     *
     * @param id identificador
     * @return la entidad si existe
     */
    public Optional<PartidaEntity> buscar(String id) {
        return partidaRepository.findById(id);
    }

    /**
     * Busca una partida por id cargando su historial de tiros en la misma
     * consulta mediante {@code @EntityGraph}. Evita {@code LazyInitializationException}
     * al mapear la entidad fuera de la transacción original.
     *
     * @param id identificador
     * @return la entidad con {@code tiros} inicializado, o vacío
     */
    public Optional<PartidaEntity> buscarConHistorial(String id) {
        return partidaRepository.findByIdConHistorial(id);
    }

    /**
     * Lista todas las partidas sin cargar su historial de tiros.
     *
     * @return lista de entidades
     */
    public List<PartidaEntity> listarTodas() {
        return partidaRepository.findAll();
    }

    /**
     * Lista todas las partidas cargando su historial de tiros en la misma consulta.
     *
     * @return lista de entidades con {@code tiros} inicializado
     */
    public List<PartidaEntity> listarTodasConHistorial() {
        return partidaRepository.findAllConHistorial();
    }

    public int cantidad() {
        return (int) partidaRepository.count();
    }
}