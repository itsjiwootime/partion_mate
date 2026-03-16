package com.project.partition_mate.service;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class StoreQueryCacheSupport {

    public static final String NEARBY_STORES_CACHE = "nearbyStores";
    public static final String STORE_PARTIES_CACHE = "storeParties";

    private final CacheManager cacheManager;

    public StoreQueryCacheSupport(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public static String nearbyCacheKey(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.4f:%.4f", latitude, longitude);
    }

    public void evictStoreQueries(Long storeId) {
        clearNearbyStores();
        evictStoreParties(storeId);
    }

    public void clearAll() {
        clearNearbyStores();
        Optional.ofNullable(cacheManager.getCache(STORE_PARTIES_CACHE)).ifPresent(Cache::clear);
    }

    public void clearNearbyStores() {
        Optional.ofNullable(cacheManager.getCache(NEARBY_STORES_CACHE)).ifPresent(Cache::clear);
    }

    public void evictStoreParties(Long storeId) {
        Optional.ofNullable(cacheManager.getCache(STORE_PARTIES_CACHE)).ifPresent(cache -> cache.evict(storeId));
    }
}
