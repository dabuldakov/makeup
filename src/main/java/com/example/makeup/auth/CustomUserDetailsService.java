package com.example.makeup.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Загружает пользователя для JWT-фильтра. Результат кешируется в общем
 * {@link CacheManager} (Caffeine на одной ноде, Redis при нескольких), чтобы
 * фильтр не делал SELECT на каждый запрос.
 *
 * <p>Кеш инвалидируется при изменении профиля/удалении аккаунта; при общей
 * Redis-конфигурации инвалидация видна всем нодам.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    static final String USERS_CACHE = "users";

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            User cached = cache.get(username, User.class);
            if (cached != null) {
                return cached;
            }
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        if (cache != null) {
            cache.put(username, user);
        }
        return user;
    }

    public void evict(String username) {
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) {
            cache.evict(username);
        }
    }
}
