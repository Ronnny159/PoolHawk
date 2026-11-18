package com.poolhawk.api.exception;

import com.poolhawk.api.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Manejador global de excepciones de la API.
 *
 * <p>Captura las excepciones lanzadas por los controladores y por el proxy
 * transaccional, y las convierte en respuestas HTTP con un cuerpo
 * {@link ErrorResponse} estandarizado.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Convierte {@link PartidaNoEncontradaException} en HTTP 404.
     *
     * @param ex      excepción de partida inexistente.
     * @param request petición HTTP actual.
     * @return respuesta 404 con cuerpo estandarizado.
     */
    @ExceptionHandler(PartidaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            PartidaNoEncontradaException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Convierte errores de validación de Bean Validation en HTTP 400.
     *
     * @param ex      excepción de validación de argumentos.
     * @param request petición HTTP actual.
     * @return respuesta 400 con el primer error de campo detectado.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .findFirst()
            .orElse("Error de validacion");
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Convierte {@link IllegalArgumentException} en HTTP 400.
     *
     * @param ex      excepción de argumento inválido.
     * @param request petición HTTP actual.
     * @return respuesta 400 con cuerpo estandarizado.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Captura {@link ResponseStatusException} y respeta el código HTTP que trae.
     * Es la vía por la que el servicio reporta 400 (validación/idempotencia) y
     * 409 (conflicto detectado dentro del método transaccional).
     *
     * @param ex      excepción con estado HTTP explícito.
     * @param request petición HTTP actual.
     * @return respuesta con el código y motivo originales.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            ex.getStatusCode().value(),
            ex.getStatusCode().toString(),
            ex.getReason(),
            request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    /**
     * Traduce conflictos de bloqueo optimista a HTTP 409.
     *
     * <p>Cubre el escenario que el {@code try/catch} del servicio no puede ver:
     * la colisión de {@code @Version} detectada durante el <i>flush</i> o el
     * <i>commit</i>, es decir, después de que el método transaccional retornó.
     * La caché ya fue invalidada por el callback {@code afterCompletion}.</p>
     *
     * @param ex      excepción de bloqueo optimista (Spring o JPA).
     * @param request petición HTTP actual.
     * @return respuesta 409 con cuerpo estandarizado.
     */
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de versión durante el commit en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "La partida fue modificada por otro proceso. Intente de nuevo.",
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Captura cualquier excepción no manejada como HTTP 500.
     *
     * @param ex      excepción no controlada.
     * @param request petición HTTP actual.
     * @return respuesta 500 con cuerpo estandarizado.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}