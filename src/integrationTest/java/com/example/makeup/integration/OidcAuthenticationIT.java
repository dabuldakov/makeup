package com.example.makeup.integration;

import com.example.makeup.auth.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-путь OIDC-аутентификации: при {@code auth.oidc.enabled=true}
 * {@link com.example.makeup.security.JwtAuthenticationFilter} принимает
 * access-токен единого IdP, провижинит локального пользователя через
 * {@link com.example.makeup.auth.OidcUserProvisioner} и пускает его в
 * защищённые эндпоинты. Декодер токенов по JWKS подменён моком — сеть не нужна.
 */
@TestPropertySource(properties = "auth.oidc.enabled=true")
class OidcAuthenticationIT extends AbstractIntegrationTest {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void bearerTokenIsDecodedAndProvisionsLocalUser() throws Exception {
        UUID subject = UUID.randomUUID();
        when(jwtDecoder.decode("good-token")).thenReturn(Jwt.withTokenValue("good-token")
                .header("alg", "RS256")
                .subject(subject.toString())
                .claim("username", "oidc-user")
                .claim("email", "oidc-user@example.com")
                .build());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("oidc-user"))
                .andExpect(jsonPath("$.email").value("oidc-user@example.com"));

        User provisioned = userRepository.findByAuthSubject(subject).orElseThrow();
        assertThat(provisioned.getUsername()).isEqualTo("oidc-user");
        assertThat(provisioned.getEmail()).isEqualTo("oidc-user@example.com");
    }

    @Test
    void invalidBearerTokenIsRejected() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("invalid token"));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer broken"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }
}
