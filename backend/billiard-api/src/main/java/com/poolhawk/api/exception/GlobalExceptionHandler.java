package com.poolhawk.api.exception;

import com.poolhawk.api.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PartidaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            PartidaNoEncontradaException ex, HttpServletRequest request) {
        log.warn("Partida no encontrada: {} - URI: {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn("Error de validación en {}: {}", request.getRequestURI(), message);
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        log.warn("Violación de constraint en {}: {}", request.getRequestURI(), message);
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Error deserializando el request: " + ex.getMostSpecificCause().getMessage();
        log.warn("Error de deserialización en {}: {}", request.getRequestURI(), message);
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            RuntimeException ex, HttpServletRequest request) {
        log.warn("Error de estado/argumento ilegal en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        log.warn("ResponseStatusException en {}: status={}, reason={}", 
                request.getRequestURI(), ex.getStatusCode(), ex.getReason());
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            ex.getStatusCode().value(),
            ex.getStatusCode().toString(),
            ex.getReason(),
            request.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    /**
     * Captura conflictos de bloqueo optimista que ocurren durante el commit.
     */
    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(Exception ex, HttpServletRequest request) {
        log.warn("Conflicto de versión durante el commit en {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "La partida fue modificada por otro proceso. Intente de nuevo.",
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "Ocurrió un error inesperado en el servidor: " + ex.getMessage(),
            request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}