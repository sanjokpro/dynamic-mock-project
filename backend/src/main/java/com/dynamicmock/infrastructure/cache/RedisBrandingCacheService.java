package com.dynamicmock.infrastructure.cache;

import com.dynamicmock.domain.entity.OrganizationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache for the effective instance branding configuration.
 *
 * <ul>
 *   <li>Key: {@value #CACHE_KEY}, stored as a JSON string.</li>
 *   <li>TTL: 24 hours.</li>
 *   <li>Eviction: synchronous {@code DEL} after every admin update or reset.</li>
 *   <li>Fail-open: any Redis or serialization failure is logged as a warning and treated
 *       as a cache miss, so branding requests never fail because Redis is unavailable.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBrandingCacheService {

    public static final String CACHE_KEY = "config:branding:instance";
    public static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<OrganizationConfig> get() {
        try {
            String json = redisTemplate.opsForValue().get(CACHE_KEY);
            if (json == null) {
                log.debug("Branding cache MISS");
                return Optional.empty();
            }
            log.debug("Branding cache HIT");
            return Optional.of(objectMapper.readValue(json, OrganizationConfig.class));
        } catch (Exception e) {
            log.warn("Branding cache read failed, falling back to MongoDB: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void set(OrganizationConfig config) {
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(config), CACHE_TTL);
            log.debug("Branding cache SET (ttl={})", CACHE_TTL);
        } catch (Exception e) {
            log.warn("Branding cache write failed: {}", e.getMessage());
        }
    }

    public void evict() {
        try {
            redisTemplate.delete(CACHE_KEY);
            log.info("Branding cache EVICTED ({})", CACHE_KEY);
        } catch (Exception e) {
            log.warn("Branding cache eviction failed: {}", e.getMessage());
        }
    }
}
