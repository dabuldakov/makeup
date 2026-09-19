package com.example.makeup.service;

import com.example.makeup.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;

    private UserDetails user(String name) {
        return new User(name, "password",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret("test-secret-key-with-at-least-32-bytes!!");
        config.setExpiration(3_600_000L);
        jwtService = new JwtService(config);
    }

    @Test
    void generateToken_shouldContainUsernameAsSubject() {
        String token = jwtService.generateToken(user("alice"));

        assertEquals("alice", jwtService.extractUsername(token));
    }

    @Test
    void generateToken_shouldHaveFutureExpiration() {
        String token = jwtService.generateToken(user("alice"));

        long expiresAt = jwtService.extractClaim(token, claims -> claims.getExpiration().getTime());
        assertTrue(expiresAt > System.currentTimeMillis());
    }

    @Test
    void isTokenValid_shouldAcceptTokenForSameUser() {
        UserDetails alice = user("alice");
        String token = jwtService.generateToken(alice);

        assertTrue(jwtService.isTokenValid(token, alice));
    }

    @Test
    void isTokenValid_shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(user("alice"));

        assertFalse(jwtService.isTokenValid(token, user("bob")));
    }

    @Test
    void generateToken_shouldProduceAcceptableTokensWithinSameSecond() {
        UserDetails alice = user("alice");
        String first = jwtService.generateToken(alice);
        String second = jwtService.generateToken(alice);

        assertTrue(first.length() > 0);
        assertTrue(second.length() > 0);
        assertTrue(jwtService.isTokenValid(first, alice));
        assertTrue(jwtService.isTokenValid(second, alice));
    }

    @Test
    void extractUsername_shouldFailOnTamperedToken() {
        String token = jwtService.generateToken(user("alice"));

        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThrows(Exception.class, () -> jwtService.extractUsername(tampered));
    }
}