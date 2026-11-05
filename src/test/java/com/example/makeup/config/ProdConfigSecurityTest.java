package com.example.makeup.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Прод-профиль не должен содержать небезопасных дефолтов БД. */
class ProdConfigSecurityTest {

    private String prodYaml() throws Exception {
        try (var input = new ClassPathResource("application-prod.yaml").getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void datasourceRequiresEnvironmentWithoutDefaults() throws Exception {
        String yaml = prodYaml();

        assertThat(yaml).contains("url: ${POSTGRES_URL}");
        assertThat(yaml).contains("username: ${POSTGRES_USERNAME}");
        assertThat(yaml).contains("password: ${POSTGRES_PASSWORD}");
        assertThat(yaml).doesNotContain(":admin");
    }
}
