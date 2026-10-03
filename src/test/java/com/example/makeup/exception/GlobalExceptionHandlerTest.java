package com.example.makeup.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        assertStatus(handler.handleNotFound(new NotFoundException("missing")), HttpStatus.NOT_FOUND, "missing");
    }

    @Test
    void conflictMapsTo409() {
        assertStatus(handler.handleConflict(new ConflictException("dup")), HttpStatus.CONFLICT, "dup");
    }

    @Test
    void runtimeMapsTo400WithSafeMessage() {
        assertStatus(handler.handleRuntimeException(new RuntimeException("secret internals")),
                HttpStatus.BAD_REQUEST, "Request cannot be processed");
    }

    @Test
    void genericMapsTo500WithSafeMessage() {
        assertStatus(handler.handleGenericException(new Exception("boom")),
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @Test
    void bodyAlwaysContainsTimestampStatusAndMessage() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new NotFoundException("x"));

        assertThat(response.getBody()).containsKeys("timestamp", "message", "status");
        assertThat(response.getBody().get("status")).isEqualTo(404);
    }

    private void assertStatus(ResponseEntity<Map<String, Object>> response, HttpStatus expected, String message) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody().get("message")).isEqualTo(message);
    }
}
