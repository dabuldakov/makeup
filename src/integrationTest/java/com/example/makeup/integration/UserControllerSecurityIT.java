package com.example.makeup.integration;

import com.example.makeup.auth.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-проверки контроля доступа к профилю: пользователь может менять только
 * свой профиль, чужой — нет; анонимные запросы отклоняются.
 */
class UserControllerSecurityIT extends AbstractIntegrationTest {

    @Test
    void currentUserReturnsOwnProfile() throws Exception {
        User self = createUser("self");

        mockMvc.perform(get("/api/users/me").with(auth(self)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("self"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void currentUserWithoutTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOwnProfileIsAllowed() throws Exception {
        User owner = createUser("owner");

        mockMvc.perform(put("/api/users/{id}", owner.getId())
                        .with(auth(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"));
    }

    @Test
    void updateForeignProfileIsForbidden() throws Exception {
        User attacker = createUser("attacker");
        User victim = createUser("victim");

        mockMvc.perform(put("/api/users/{id}", victim.getId())
                        .with(auth(attacker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Hijacked\"}"))
                .andExpect(status().isForbidden());

        // Профиль жертвы не изменился.
        User reloaded = userRepository.findByUsername("victim").orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Test victim");
    }

    @Test
    void updateProfileWithoutTokenIsForbidden() throws Exception {
        User victim = createUser("victim-anon");

        mockMvc.perform(put("/api/users/{id}", victim.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Hijacked\"}"))
                .andExpect(status().isForbidden());
    }
}
