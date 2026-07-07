package com.msm.core.objects.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Optional;
import java.util.function.Supplier;

@Deprecated
@Slf4j
public class InMemoryCaches {

    private static CacheManager cacheManager;

    public InMemoryCaches(CacheManager cacheManager) {
        InMemoryCaches.cacheManager = cacheManager;
    }

    private static Cache getCache(String cacheName) {
        if (cacheManager == null) {
            return null;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Not found cache by name: '{}'", cacheName);
        }
        return cache;
    }

    public static <T> Optional<T> get(String cacheName, Object key, Class<T> type) {
        Cache cache = getCache(cacheName);
        if (cache == null || key == null) return Optional.empty();
        try {
            return Optional.ofNullable(cache.get(key, type));
        } catch (Exception e) {
            log.error("Exception occurred while fetching cache '{}' với key '{}': {}", cacheName, key, e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOrCompute(String cacheName, Object key, Supplier<T> fallback) {
        Cache cache = getCache(cacheName);
        if (key == null) return fallback.get();
        if (cache == null) return fallback.get();

        try {
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if (valueWrapper != null) {
                return (T) valueWrapper.get();
            }

            T value = fallback.get();
            if (value != null) {
                cache.put(key, value);
            }
            return value;
        } catch (Exception e) {
            log.error("Error getOrCompute for cache '{}': {}", cacheName, e.getMessage());
            return fallback.get();
        }
    }

    public static void put(String cacheName, Object key, Object value) {
        Cache cache = getCache(cacheName);
        if (cache != null && key != null && value != null) {
            cache.put(key, value);
        }
    }

    public static void evict(String cacheName, Object key) {
        Cache cache = getCache(cacheName);
        if (cache != null && key != null) {
            cache.evict(key);
        }
    }

    public static void clear(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}

