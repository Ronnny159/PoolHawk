package com.poolhawk.api.exception;

import com.poolhawk.api.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
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
 * <p>Captura las excepciones lanzadas por los controladores y las
 * convierte en respuestas HTTP con un cuerpo {@link ErrorResponse}
 * estandarizado.</p>
 *
 * @author PoolHawk Team
 * @since 0.1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Convierte {@link PartidaNoEncontradaException} en HTTP 404.
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
     * Convierte errores de validacion de Bean Validation en HTTP 400.
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
     * Captura {@link ResponseStatusException} y devuelve el código HTTP
     * y el mensaje que trae la excepción. Esto es fundamental para que
     * los tests de idempotencia (409 Conflict) y validaciones (400 Bad Request)
     * devuelvan el código correcto al cliente.
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
     * Captura cualquier excepcion no manejada como HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}