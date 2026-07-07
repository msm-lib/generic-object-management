package com.msm.core.objects.cache;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public interface RedisCacheOperator {

    <T> T get(String key, Class<T> type);
    default <T> List<T> get(String[] key, Class<T> type) {
        return get(Arrays.asList(key), type);
    }

    <T> List<T> get(List<String> key, Class<T> type);

    void set(String key, String value, Duration ttl);

    <T> void set(String key, T value, Duration ttl);

    default void set(String key, String value) {
        set(key, value, null);
    }

    <T> void set(String key, T value);

    boolean delete(String key);

    long delete(Collection<String> key);

    boolean exists(String key);

    void hSet(String key, String hashKey, String value);

    Set<String> scanKeysWithPrefix(String prefix);

    <T> List<T> hGet(String key, Class<T> type);

    Object getHashFieldValue(String key, String field);

    <T> T getHashFieldValue(String key, String field, Class<T> type);

    <F, V> Map<F, V> hGetAll(String key);

    default boolean expire(String key, long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    boolean expire(String key, long timeout, TimeUnit unit);

    boolean acquireLock(String lockKey, String value, long timeoutInSeconds);

    default boolean acquireLock(String lockKey, long timeoutInSeconds) {
        return acquireLock(lockKey, UUID.randomUUID().toString(), timeoutInSeconds);
    }

    default boolean acquireLock(String lockKey, Duration duration) {
        return acquireLock(lockKey, UUID.randomUUID().toString(), duration.toSeconds());
    }

    default void releaseLock(String lockKey) {
        delete(lockKey);
    }
}
