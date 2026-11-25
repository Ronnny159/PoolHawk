package com.poolhawk.api.service;

import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.repository.PartidaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Almacen de partidas en la base de datos.
 * Usa save() de Spring Data JPA para actualizaciones seguras con @Version.
 */
@Component
public class MatchStorage {

    private final PartidaRepository partidaRepository;

    public MatchStorage(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

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