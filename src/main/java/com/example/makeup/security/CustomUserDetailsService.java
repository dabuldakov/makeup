package com.example.makeup.security;

import com.example.makeup.auth.User;
import com.example.makeup.auth.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Кэширует пользователя на короткое время, чтобы JWT-фильтр не выполнял
     * SELECT на каждый HTTP-запрос. Стирается при обновлении профиля.
     */
    private final Cache<String, User> userCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userCache.get(username, key -> userRepository.findByUsername(key)
                .orElse(null));
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return user;
    }

    public void evict(String username) {
        userCache.invalidate(username);
    }
}