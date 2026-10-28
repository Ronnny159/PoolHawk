package com.poolhawk.api.service;

import com.poolhawk.api.entity.PartidaEntity;
import com.poolhawk.api.repository.PartidaRepository;
import com.poolhawk.domain.match.Partida;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacen en memoria de partidas.
 *
 * <p>Para este MVP, las partidas viven en memoria del proceso. En la
 * siguiente iteracion se reemplazara por persistencia con JPA. La
 * implementacion actual usa {@link ConcurrentHashMap} para ser segura
 * ante peticiones concurrentes.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@Component
public class MatchStorage {

    private final PartidaRepository partidaRepository;

    public MatchStorage(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

    /**
     * Guarda una partida. Si ya existe una con el mismo id, la reemplaza.
     *
     * @param partida partida a guardar
     */
    public void guardar(PartidaEntity partida) {
        partidaRepository.save(partida);
    }

    /**
     * Busca una partida por su id.
     *
     * @param id identificador de la partida
     * @return la partida si existe
     */
    public Optional<PartidaEntity> buscar(String id) {
        return partidaRepository.findById(id);
    }

    /**
     * Retorna todas las partidas almacenadas.
     *
     * @return coleccion de partidas
     */
    public List<PartidaEntity> listarTodas() {
        return partidaRepository.findAll();
    }

    /**
     * Retorna cuantas partidas hay almacenadas.
     *
     * @return cantidad de partidas
     */
    public int cantidad() {
        return (int) partidaRepository.count();
    }
}