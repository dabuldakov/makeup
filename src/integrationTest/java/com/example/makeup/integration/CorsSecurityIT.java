package com.example.makeup.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORS: разрешённый origin получает заголовки, чужой — 403. */
class CorsSecurityIT extends AbstractIntegrationTest {

    @Test
    void allowedOriginGetsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/news")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void foreignOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/news")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
