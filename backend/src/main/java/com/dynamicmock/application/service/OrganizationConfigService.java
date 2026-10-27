package com.dynamicmock.application.service;

import com.dynamicmock.application.port.in.OrganizationConfigUseCase;
import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.domain.port.out.OrganizationConfigRepository;
import com.dynamicmock.infrastructure.cache.RedisBrandingCacheService;
import com.dynamicmock.infrastructure.config.BrandingEnvironmentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * Application service implementing {@link OrganizationConfigUseCase}.
 *
 * <p>Effective branding is resolved field by field with deterministic precedence:</p>
 * <ol>
 *   <li>Non-blank {@code WHITE_LABEL_*} environment overrides (highest)</li>
 *   <li>Values stored in MongoDB via the admin API</li>
 *   <li>{@link OrganizationConfig#createDefault()} platform defaults (lowest)</li>
 * </ol>
 *
 * <p>The resolved result is cached in Redis; every mutation evicts the cache.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationConfigService implements OrganizationConfigUseCase {

    private final OrganizationConfigRepository repository;
    private final RedisBrandingCacheService cache;
    private final BrandingEnvironmentProperties envProperties;

    @Override
    public OrganizationConfig getEffectiveConfig() {
        return cache.get().orElseGet(() -> {
            OrganizationConfig effective = resolveEffective(repository.findConfig().orElse(null));
            cache.set(effective);
            return effective;
        });
    }

    @Override
    public OrganizationConfig updateConfig(OrganizationConfig config) {
        config.setId(OrganizationConfig.SINGLETON_ID);
        config.setUpdatedAt(LocalDateTime.now());
        OrganizationConfig saved = repository.save(config);
        cache.evict();
        log.info("Instance branding updated (productName='{}')", saved.getProductName());
        return resolveEffective(saved);
    }

    @Override
    public OrganizationConfig resetToDefault() {
        OrganizationConfig saved = repository.save(OrganizationConfig.createDefault());
        cache.evict();
        log.info("Instance branding reset to platform defaults");
        return resolveEffective(saved);
    }

    /**
     * Layers stored values and environment overrides on top of platform defaults.
     *
     * @param stored the persisted configuration, or {@code null} when none exists
     */
    OrganizationConfig resolveEffective(OrganizationConfig stored) {
        OrganizationConfig defaults = OrganizationConfig.createDefault();
        OrganizationConfig base = stored != null ? stored : defaults;

        return OrganizationConfig.builder()
                .id(OrganizationConfig.SINGLETON_ID)
                .productName(pick(envProperties.effectiveProductName(), base, defaults, OrganizationConfig::getProductName))
                .organizationName(pick(envProperties.effectiveOrgName(), base, defaults, OrganizationConfig::getOrganizationName))
                .logoUrl(pick(envProperties.effectiveLogoUrl(), base, defaults, OrganizationConfig::getLogoUrl))
                .faviconUrl(pick(envProperties.effectiveFaviconUrl(), base, defaults, OrganizationConfig::getFaviconUrl))
                .primaryColor(pick(envProperties.effectivePrimaryColor(), base, defaults, OrganizationConfig::getPrimaryColor))
                .secondaryColor(pick(envProperties.effectiveSecondaryColor(), base, defaults, OrganizationConfig::getSecondaryColor))
                .accentColor(pick(null, base, defaults, OrganizationConfig::getAccentColor))
                .loginBackgroundUrl(pick(null, base, defaults, OrganizationConfig::getLoginBackgroundUrl))
                .footerText(pick(envProperties.effectiveFooterText(), base, defaults, OrganizationConfig::getFooterText))
                .updatedAt(base.getUpdatedAt())
                .build();
    }

    private static String pick(String override,
                               OrganizationConfig stored,
                               OrganizationConfig defaults,
                               Function<OrganizationConfig, String> field) {
        if (override != null) return override;
        String storedValue = field.apply(stored);
        if (storedValue != null && !storedValue.isBlank()) return storedValue;
        return field.apply(defaults);
    }
}
