package com.msm.core.objects.integration;

import com.msm.core.objects.cache.InMemoryCaches;

import java.util.Optional;
import java.util.function.Supplier;

public class IntegrationTokenCache {
    private final static String CACHE_NAME = "IntegrationTokenCache";

    public static <T> Optional<T> get(Object key, Class<T> type) {
        return InMemoryCaches.get(CACHE_NAME, key, type);
    }

    public static <T> T getOrCompute(Object key, Supplier<T> fallback) {
        return InMemoryCaches.getOrCompute(CACHE_NAME, key, fallback);
    }

    public static void put(Object key, Object value) {
        InMemoryCaches.put(CACHE_NAME, key, value);
    }

    public static void evict(Object key) {
        InMemoryCaches.evict(CACHE_NAME, key);
    }

    public static void clear() {
        InMemoryCaches.clear(CACHE_NAME);
    }
}
