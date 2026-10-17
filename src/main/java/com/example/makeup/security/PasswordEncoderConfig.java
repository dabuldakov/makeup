package com.example.makeup.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder вынесен из {@link SecurityConfig}, чтобы не возникало цикла:
 * SecurityConfig -> JwtAuthenticationFilter -> OidcUserProvisioner -> PasswordEncoder
 * -> SecurityConfig.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
