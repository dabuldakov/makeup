package com.example.makeup.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcUserProvisionerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OidcUserProvisioner provisioner;

    @BeforeEach
    void setUp() {
        OidcProperties properties =
                new OidcProperties(true, "iss", "jwks", java.util.List.of("api-muzea"), true);
        provisioner = new OidcUserProvisioner(userRepository, passwordEncoder, properties);
    }

    @Test
    void returnsExistingUserBySubject() {
        UUID subject = UUID.randomUUID();
        User existing = user("existing");
        when(userRepository.findByAuthSubject(subject)).thenReturn(Optional.of(existing));

        assertThat(provisioner.findOrCreate(subject, "ignored", null)).isSameAs(existing);
    }

    @Test
    void linksExistingAccountByEmail() {
        UUID subject = UUID.randomUUID();
        User existing = user("legacy");
        when(userRepository.findByAuthSubject(subject)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = provisioner.findOrCreate(subject, "user", "user@example.com");

        assertThat(result.getAuthSubject()).isEqualTo(subject);
    }

    @Test
    void linksExistingAccountByUsernameWhenEmailDiffers() {
        UUID subject = UUID.randomUUID();
        User existing = user("legacylogin");
        when(userRepository.findByAuthSubject(subject)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("legacylogin")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = provisioner.findOrCreate(subject, "legacylogin", "new@example.com");

        assertThat(result.getAuthSubject()).isEqualTo(subject);
    }

    @Test
    void provisionsNewUserWhenUnknown() {
        UUID subject = UUID.randomUUID();
        when(userRepository.findByAuthSubject(subject)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = provisioner.findOrCreate(subject, "newuser", "new@example.com");

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getAuthSubject()).isEqualTo(subject);
        assertThat(result.getPassword()).isEqualTo("encoded");
        assertThat(result.getFullName()).isEqualTo("newuser");
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    void suffixesUsernameOnCollision() {
        UUID subject = UUID.randomUUID();
        when(userRepository.findByAuthSubject(subject)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername("taken")).thenReturn(true);
        when(userRepository.existsByUsername("taken_" + subject.toString().replace("-", "").substring(0, 6)))
                .thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = provisioner.findOrCreate(subject, "taken", "t@example.com");

        assertThat(result.getUsername()).startsWith("taken_").hasSizeLessThanOrEqualTo(50);
    }

    @Test
    void recoversFromConcurrentCreation() {
        UUID subject = UUID.randomUUID();
        User concurrent = user("racer");
        when(userRepository.findByAuthSubject(subject))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrent));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        User result = provisioner.findOrCreate(subject, "racer", "racer@example.com");

        assertThat(result).isSameAs(concurrent);
    }

    private User user(String username) {
        return User.builder()
                .username(username)
                .email(username + "@example.com")
                .password("hash")
                .fullName(username)
                .role(Role.USER)
                .isActive(true)
                .build();
    }
}
