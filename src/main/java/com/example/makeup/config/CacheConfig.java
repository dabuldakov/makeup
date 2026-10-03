package com.example.makeup.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Кеш приложения.
 *
 * <p>По умолчанию — локальный Caffeine ({@code app.cache.redis.enabled=false}):
 * годится для dev и одной ноды. При нескольких инстансах локальный кеш
 * расходится (инвалидация не видна другим нодам), поэтому на проде включается
 * общий Redis-кеш через {@code app.cache.redis.enabled=true}.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    static final List<String> CACHE_NAMES = List.of("users");
    private static final Duration TTL = Duration.ofMinutes(5);

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "true")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(TTL)
                .prefixCacheNameWith("makeup::");
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .initialCacheNames(new LinkedHashSet<>(CACHE_NAMES))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.cache.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_NAMES.toArray(String[]::new));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(TTL));
        return cacheManager;
    }
}
