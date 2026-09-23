package com.example.makeup.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = fieldErrors(ex.getBindingResult().getFieldErrors());
        log.warn("Validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.BAD_REQUEST, "Request cannot be processed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private String fieldErrors(List<FieldError> errors) {
        return errors.stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        response.put("message", message);
        response.put("status", status.value());
        return ResponseEntity.status(status).body(response);
    }
}