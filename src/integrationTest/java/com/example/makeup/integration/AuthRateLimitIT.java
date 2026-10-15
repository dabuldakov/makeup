package com.example.makeup.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Лимит частоты на логин: после серии неудачных попыток дальнейшие отклоняются
 * с 429, а не бесконечно перебирают пароль (по умолчанию 10 попыток за 5 минут).
 */
class AuthRateLimitIT extends AbstractIntegrationTest {

    private static final int MAX_ATTEMPTS = 10;

    private String loginBody(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @Test
    void loginIsRateLimitedAfterTooManyFailedAttempts() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("nobody", "wrong")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody", "wrong")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void differentAccountsAreLimitedIndependently() throws Exception {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("account-a", "wrong")))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("account-b", "wrong")))
                .andExpect(status().isUnauthorized());
    }
}
