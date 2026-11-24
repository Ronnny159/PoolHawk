
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
     * Guarda una partida y fuerza la sincronización con la BD dentro de la
     * transacción actual. Esto permite detectar conflictos de @Version
     * durante la ejecución del servicio.
     */
    public void guardar(PartidaEntity partida) {
        entityManager.merge(partida);
        entityManager.flush();
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
