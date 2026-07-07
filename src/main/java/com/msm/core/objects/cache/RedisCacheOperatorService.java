package com.msm.core.objects.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.msm.core.commons.Utils;
import com.msm.core.objects.exception.ObjectJsonMappingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Slf4j
@RequiredArgsConstructor
public class RedisCacheOperatorService implements RedisCacheOperator{
    private final RedisTemplate<String, String> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public <T> T get(String key, Class<T> type) {
        try {
            return Utils.O.toObject(redisTemplate.opsForValue().get(key), type);
        } catch (JsonProcessingException e) {
            log.error("Error while reading value from redis", e);
            throw new ObjectJsonMappingException(e);
        }
    }

    public <T> List<T> get(List<String> key, Class<T> type) {
        return Objects.requireNonNull(redisTemplate.opsForValue()
                        .multiGet(key))
                .stream()
                .map(s -> {
                    try {
                        return Utils.O.toObject(s, type);
                    } catch (JsonProcessingException e) {
                        log.error("Error while reading value from redis", e);
                        throw new ObjectJsonMappingException(e);
                    }
                })
                .toList();
    }

    public void set(String key, String value, Duration ttl) {
        if (Objects.isNull(ttl)) {
            redisTemplate.opsForValue().set(key, value);
        } else {
            redisTemplate.opsForValue().set(key, value, ttl);
        }
    }

    public <T> void set(String key, T value, Duration ttl) {
        try {
            set(key, Utils.O.toJsonString(value), ttl);
        } catch (JsonProcessingException e) {
            log.error("Error while reading value from input", e);
            throw new ObjectJsonMappingException(e);
        }
    }

    public <T> void set(String key, T value) {
        try {
            set(key, Utils.O.toJsonString(value), null);
        } catch (JsonProcessingException e) {
            log.error("Error while parse value: {}", value, e);
            throw new ObjectJsonMappingException(e);
        }
    }

    public boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public long delete(Collection<String> key) {
        return redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    public void hSet(String key, String hashKey, String value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Set<String> scanKeysWithPrefix(String prefix) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(1000).build();
        try (Cursor<String> cursor = redisTemplate.executeWithStickyConnection(
                redisConnection -> redisTemplate.scan(options))) {
          while (cursor.hasNext()) {
              keys.add(cursor.next());
          }
        } catch (Exception e) {
            log.error("Error while scanning keys with prefix: {}", prefix, e);
        }
        return keys;
    }

    public <T> List<T> hGet(String key, Class<T> type) {
        return redisTemplate
                .opsForHash()
                .values(key)
                .stream()
                .map(s -> {
                    try {
                        return Utils.O.toObject((String) s, type);
                    } catch (JsonProcessingException e) {
                        log.error("Error while parse value: {}", s, e);
                        throw new ObjectJsonMappingException(e);
                    }
                }).toList();
    }

    public Object getHashFieldValue(String key, String field) {
        return redisTemplate.opsForHash().get(key, field);
    }

    public <T> T getHashFieldValue(String key, String field, Class<T> type) {
        Object obj = getHashFieldValue(key, field);
        try {
            return Utils.O.toObject((String) obj, type);
        } catch (JsonProcessingException e) {
            throw new ObjectJsonMappingException(e);
        }
    }

    public <F, V> Map<F, V> hGetAll(String key) {
        return redisTemplate
                .<F, V>opsForHash()
                .entries(key);
    }


    public boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    public boolean acquireLock(String lockKey, String value, long timeoutInSeconds) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(lockKey, value, timeoutInSeconds, TimeUnit.SECONDS));
    }
}
