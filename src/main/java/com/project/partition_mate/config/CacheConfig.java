package com.project.partition_mate.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.partition_mate.service.StoreQueryCacheSupport;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                createCache(StoreQueryCacheSupport.NEARBY_STORES_CACHE, 200, Duration.ofMinutes(5)),
                createCache(StoreQueryCacheSupport.STORE_PARTIES_CACHE, 300, Duration.ofMinutes(2))
        ));
        return cacheManager;
    }

    private CaffeineCache createCache(String cacheName, long maximumSize, Duration ttl) {
        return new CaffeineCache(
                cacheName,
                Caffeine.newBuilder()
                        .maximumSize(maximumSize)
                        .expireAfterWrite(ttl)
                        .recordStats()
                        .build()
        );
    }
}
