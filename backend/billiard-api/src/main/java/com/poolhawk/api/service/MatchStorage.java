package com.poolhawk.api.service;

import com.poolhawk.domain.match.Partida;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
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

    private final Map<String, Partida> partidas = new ConcurrentHashMap<>();

    /**
     * Guarda una partida. Si ya existe una con el mismo id, la reemplaza.
     *
     * @param partida partida a guardar
     */
    public void guardar(Partida partida) {
        partidas.put(partida.getId(), partida);
    }

    /**
     * Busca una partida por su id.
     *
     * @param id identificador de la partida
     * @return la partida si existe
     */
    public Optional<Partida> buscar(String id) {
        return Optional.ofNullable(partidas.get(id));
    }

    /**
     * Retorna todas las partidas almacenadas.
     *
     * @return coleccion de partidas
     */
    public Collection<Partida> listarTodas() {
        return partidas.values();
    }

    /**
     * Elimina una partida por su id.
     *
     * @param id identificador de la partida
     * @return {@code true} si existia y fue eliminada
     */
    public boolean eliminar(String id) {
        return partidas.remove(id) != null;
    }

    /**
     * Retorna cuantas partidas hay almacenadas.
     *
     * @return cantidad de partidas
     */
    public int cantidad() {
        return partidas.size();
    }
}