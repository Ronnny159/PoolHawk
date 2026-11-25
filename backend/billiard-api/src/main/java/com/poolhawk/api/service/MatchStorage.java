package com.poolhawk.api.service;

import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.repository.PartidaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Almacén de partidas en la base de datos.
 * Usa saveAndFlush() de Spring Data JPA para actualizaciones seguras con @Version,
 * forzando la sincronización con la BD dentro de la transacción actual para detectar
 * conflictos de concurrencia inmediatamente.
 */
@Component
public class MatchStorage {

    private final PartidaRepository partidaRepository;

    public MatchStorage(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

    /**
     * Guarda una partida y fuerza la sincronización con la BD dentro de la
     * transacción actual. Esto permite detectar conflictos de @Version
     * durante la ejecución del servicio, antes de que la transacción se complete.
     */
    public void guardar(PartidaEntity partida) {
        partidaRepository.saveAndFlush(partida);
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