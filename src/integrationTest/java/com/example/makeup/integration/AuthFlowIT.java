package com.example.makeup.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий аутентификации: регистрация, логин, ошибки, доступ
 * к защищённым эндпоинтам без токена.
 */
class AuthFlowIT extends AbstractIntegrationTest {

    @Test
    void registerReturnsTokenAndUser() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"password123","fullName":"Alice A"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        registerUser("alice");

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"other@example.com","password":"password123","fullName":"Alice B"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        registerUser("alice");

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"bob","email":"alice@example.com","password":"password123","fullName":"Bob B"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturnsTokenForRegisteredUser() throws Exception {
        registerUser("alice");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        registerUser("alice");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginForUnknownUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"nobody","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}