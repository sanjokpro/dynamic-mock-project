package com.dynamicmock.application.service;

import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.domain.port.out.OrganizationConfigRepository;
import com.dynamicmock.infrastructure.cache.RedisBrandingCacheService;
import com.dynamicmock.infrastructure.config.BrandingEnvironmentProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationConfigServiceTest {

    @Mock
    private OrganizationConfigRepository repository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisBrandingCacheService cacheService;
    private BrandingEnvironmentProperties envProperties;
    private OrganizationConfigService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cacheService = new RedisBrandingCacheService(stringRedisTemplate, objectMapper);
        envProperties = new BrandingEnvironmentProperties();
        service = new OrganizationConfigService(repository, cacheService, envProperties);
    }

    @Test
    @DisplayName("getEffectiveConfig returns cached config on Redis HIT without querying MongoDB")
    void testGetEffectiveConfig_CacheHit() throws Exception {
        OrganizationConfig cached = OrganizationConfig.createDefault();
        cached.setProductName("Cached Brand");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisBrandingCacheService.CACHE_KEY))
                .thenReturn(objectMapper.writeValueAsString(cached));

        OrganizationConfig result = service.getEffectiveConfig();

        assertThat(result.getProductName()).isEqualTo("Cached Brand");
        verify(repository, never()).findConfig();
    }

    @Test
    @DisplayName("getEffectiveConfig queries MongoDB on cache MISS, layers env overrides, and populates Redis")
    void testGetEffectiveConfig_CacheMissAndEnvPrecedence() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisBrandingCacheService.CACHE_KEY)).thenReturn(null);

        OrganizationConfig stored = OrganizationConfig.createDefault();
        stored.setProductName("DB Brand Name");
        stored.setOrganizationName("DB Org Name");
        stored.setPrimaryColor("#112233");
        when(repository.findConfig()).thenReturn(Optional.of(stored));

        // Env override set for productName and primaryColor, but NOT orgName
        envProperties.setProductName("Env Override Brand");
        envProperties.setPrimaryColor("#7c3aed");

        OrganizationConfig effective = service.getEffectiveConfig();

        assertThat(effective.getId()).isEqualTo(OrganizationConfig.SINGLETON_ID);
        assertThat(effective.getProductName()).isEqualTo("Env Override Brand");
        assertThat(effective.getPrimaryColor()).isEqualTo("#7c3aed");
        assertThat(effective.getOrganizationName()).isEqualTo("DB Org Name");
        verify(valueOperations).set(
                eq(RedisBrandingCacheService.CACHE_KEY),
                anyString(),
                eq(RedisBrandingCacheService.CACHE_TTL)
        );
    }

    @Test
    @DisplayName("getEffectiveConfig fails open to MongoDB when Redis throws an exception")
    void testGetEffectiveConfig_RedisFailOpen() {
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));
        when(repository.findConfig()).thenReturn(Optional.empty());

        OrganizationConfig effective = service.getEffectiveConfig();

        assertThat(effective.getProductName()).isEqualTo("Dynamic Mock");
        assertThat(effective.getPrimaryColor()).isEqualTo("#3b82f6");
    }

    @Test
    @DisplayName("updateConfig saves with SINGLETON_ID and synchronously evicts Redis cache")
    void testUpdateConfig_SavesAndEvictsCache() {
        OrganizationConfig update = OrganizationConfig.builder()
                .productName("Acme Hub")
                .organizationName("Acme Financial")
                .primaryColor("#8b5cf6")
                .secondaryColor("#f1f5f9")
                .footerText("Acme Legal")
                .build();

        when(repository.save(any(OrganizationConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationConfig result = service.updateConfig(update);

        assertThat(result.getId()).isEqualTo(OrganizationConfig.SINGLETON_ID);
        assertThat(result.getProductName()).isEqualTo("Acme Hub");
        assertThat(result.getPrimaryColor()).isEqualTo("#8b5cf6");
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(stringRedisTemplate).delete(RedisBrandingCacheService.CACHE_KEY);
    }

    @Test
    @DisplayName("resetToDefault restores platform defaults and evicts Redis cache")
    void testResetToDefault() {
        when(repository.save(any(OrganizationConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationConfig reset = service.resetToDefault();

        assertThat(reset.getId()).isEqualTo(OrganizationConfig.SINGLETON_ID);
        assertThat(reset.getProductName()).isEqualTo("Dynamic Mock");
        assertThat(reset.getPrimaryColor()).isEqualTo("#3b82f6");
        verify(stringRedisTemplate).delete(RedisBrandingCacheService.CACHE_KEY);
    }

    @Test
    @DisplayName("Invalid environment variable format is ignored safely without corrupting config")
    void testInvalidEnvOverrideIgnored() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisBrandingCacheService.CACHE_KEY)).thenReturn(null);
        when(repository.findConfig()).thenReturn(Optional.empty());

        envProperties.setPrimaryColor("#3b82f6; background:url(javascript:alert(1))");
        envProperties.setLogoUrl("javascript:alert(1)");

        OrganizationConfig effective = service.getEffectiveConfig();

        assertThat(effective.getPrimaryColor()).isEqualTo("#3b82f6");
        assertThat(effective.getLogoUrl()).isNull();
    }
}
