package com.example.makeup.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository repository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(repository);

    @Test
    void loadsUserAndCachesIt() {
        User user = User.builder().username("alice").password("hash").role(Role.USER).isActive(true).build();
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("alice")).isSameAs(user);
        assertThat(service.loadUserByUsername("alice")).isSameAs(user);

        verify(repository, times(1)).findByUsername("alice");
    }

    @Test
    void evictForcesReload() {
        User user = User.builder().username("alice").password("hash").role(Role.USER).isActive(true).build();
        when(repository.findByUsername("alice")).thenReturn(Optional.of(user));

        service.loadUserByUsername("alice");
        service.evict("alice");
        service.loadUserByUsername("alice");

        verify(repository, times(2)).findByUsername("alice");
    }

    @Test
    void throwsWhenUserMissing() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
