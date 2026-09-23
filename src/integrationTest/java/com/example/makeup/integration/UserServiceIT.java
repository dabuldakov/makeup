package com.example.makeup.integration;

import com.example.makeup.auth.RegisterRequest;
import com.example.makeup.auth.User;
import com.example.makeup.security.JwtService;
import com.example.makeup.auth.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Интеграционные тесты регистрации и работы с пользователями: реальный
 * BCrypt-хэш, дубликаты username/email, обновление профиля и round-trip JWT.
 */
class UserServiceIT extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    private RegisterRequest request(String username, String email) {
        RegisterRequest r = new RegisterRequest();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword("password123");
        r.setFullName("Test " + username);
        return r;
    }

    @Test
    void registerPersistsUserWithEncodedPassword() {
        User user = userService.register(request("alice", "alice@example.com"));

        assertThat(user.getId()).isNotNull();
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
        assertThat(user.getRole().name()).isEqualTo("USER");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void registerRejectsDuplicateUsername() {
        userService.register(request("alice", "alice@example.com"));

        assertThatThrownBy(() -> userService.register(request("alice", "other@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        userService.register(request("alice", "alice@example.com"));

        assertThatThrownBy(() -> userService.register(request("bob", "alice@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void getUserByUsernameReturnsRegisteredUser() {
        userService.register(request("alice", "alice@example.com"));

        User user = userService.getUserByUsername("alice");

        assertThat(user.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void getUserByUsernameThrowsForUnknownUser() {
        assertThatThrownBy(() -> userService.getUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void updateUserChangesFullNameAndAvatar() {
        User saved = userService.register(request("alice", "alice@example.com"));

        com.example.makeup.auth.UpdateUserRequest updates =
                new com.example.makeup.auth.UpdateUserRequest();
        updates.setFullName("Alice Updated");
        updates.setAvatarUrl("http://avatar/alice.png");
        User updated = userService.updateUser(saved.getId(), updates);

        assertThat(updated.getFullName()).isEqualTo("Alice Updated");
        assertThat(updated.getAvatarUrl()).isEqualTo("http://avatar/alice.png");
    }

    @Test
    void jwtRoundTripForRegisteredUser() {
        User user = userService.register(request("alice", "alice@example.com"));
        UserDetails details = loadUser("alice");

        String token = jwtService.generateToken(details);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, details)).isTrue();
        assertThat(user.isEnabled()).isTrue();
    }
}