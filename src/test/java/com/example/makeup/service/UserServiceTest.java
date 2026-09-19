package com.example.makeup.service;

import com.example.makeup.dto.request.RegisterRequest;
import com.example.makeup.entity.Role;
import com.example.makeup.entity.User;
import com.example.makeup.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private RegisterRequest request(String username, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("secret123");
        request.setFullName("Alice Wonder");
        return request;
    }

    @Test
    void register_shouldEncodePasswordAndAssignDefaultRole() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("#encoded#");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.register(request("alice", "alice@mail.com"));

        assertEquals("alice", saved.getUsername());
        assertEquals("#encoded#", saved.getPassword());
        assertEquals(Role.USER, saved.getRole());
        assertEquals("Alice Wonder", saved.getFullName());
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register(request("alice", "x@mail.com")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@mail.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register(request("alice", "alice@mail.com")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserByUsername_shouldReturnUserWhenPresent() {
        User user = User.builder().username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertEquals(user, userService.getUserByUsername("alice"));
    }

    @Test
    void getUserByUsername_shouldThrowWhenAbsent() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.getUserByUsername("ghost"));
    }

    @Test
    void updateUser_shouldUpdateOnlyProfileFields() {
        User existing = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@mail.com")
                .build();
        User details = User.builder()
                .fullName("New Name")
                .avatarUrl("/avatars/1.jpg")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateUser(1L, details);

        assertEquals("New Name", updated.getFullName());
        assertEquals("/avatars/1.jpg", updated.getAvatarUrl());
        assertEquals("alice", updated.getUsername());
        assertEquals("alice@mail.com", updated.getEmail());
    }
}