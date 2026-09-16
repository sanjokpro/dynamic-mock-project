package com.dynamicmock.adapter.out.script;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A Map implementation that delegates state directly to a Redis Hash.
 * This allows GraalVM scripts to manipulate state naturally (e.g. `state.key = value`)
 * while ensuring that state persists globally across multiple endpoints.
 */
public class RedisBackedStateMap extends AbstractMap<String, Object> {

    private final String redisKey;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisBackedStateMap(String redisKey, RedisTemplate<String, Object> redisTemplate) {
        this.redisKey = redisKey;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Object get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        return redisTemplate.opsForHash().get(redisKey, key);
    }

    @Override
    public Object put(String key, Object value) {
        Object oldValue = get(key);
        redisTemplate.opsForHash().put(redisKey, key, value);
        return oldValue;
    }

    @Override
    public Object remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        Object oldValue = get(key);
        redisTemplate.opsForHash().delete(redisKey, key);
        return oldValue;
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        return redisTemplate.opsForHash().hasKey(redisKey, key);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        return entries.entrySet().stream()
                .map(e -> new SimpleEntry<>((String) e.getKey(), e.getValue()))
                .collect(Collectors.toSet());
    }

    @Override
    public void clear() {
        redisTemplate.delete(redisKey);
    }

    @Override
    public int size() {
        return redisTemplate.opsForHash().size(redisKey).intValue();
    }
}
