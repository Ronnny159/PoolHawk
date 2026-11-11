package com.poolhawk.api.service;

import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.repository.PartidaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Almacen de partidas en la base de datos.
 * Utiliza EntityManager.merge() para actualizaciones seguras con @Version.
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
     * Guarda una partida. Si ya existe, la actualiza (merge) respetando la versión.
     * Si no existe, la inserta.
     */
    public void guardar(PartidaEntity partida) {
        // merge() actualiza si existe o inserta si no existe, sin lanzar Duplicate Key
        entityManager.merge(partida);
    }

    public Optional<PartidaEntity> buscar(String id) {
        return partidaRepository.findById(id);
    }

    public List<PartidaEntity> listarTodas() {
        return partidaRepository.findAll();
    }

    public int cantidad() {
        return (int) partidaRepository.count();
    }
}