package com.example.makeup.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private UserService userService;

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
                .build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("New Name");
        request.setAvatarUrl("/avatars/1.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateUser(1L, request);

        assertEquals("New Name", updated.getFullName());
        assertEquals("/avatars/1.jpg", updated.getAvatarUrl());
        assertEquals("alice", updated.getUsername());
    }
}
