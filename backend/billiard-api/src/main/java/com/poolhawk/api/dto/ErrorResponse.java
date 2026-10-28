package com.poolhawk.api.dto;

import java.time.Instant;

/**
 * DTO estandar para respuestas de error de la API.
 *
 * @param timestamp momento en que ocurrio el error
 * @param status    codigo HTTP del error
 * @param error     tipo de error (por ejemplo, "Not Found")
 * @param message   mensaje descriptivo del error
 * @param path      ruta del endpoint que genero el error
 * @author PoolHawk Team
 * @since 0.1.0
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}