package com.example.makeup.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Spring Security выставляет защитные заголовки и на ошибочных ответах. */
class SecurityHeadersIT extends AbstractIntegrationTest {

    @Test
    void defensiveHeadersArePresentOnForbiddenResponse() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
