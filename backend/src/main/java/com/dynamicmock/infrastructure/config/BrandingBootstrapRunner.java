package com.dynamicmock.infrastructure.config;

import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.domain.port.out.OrganizationConfigRepository;
import com.dynamicmock.infrastructure.cache.RedisBrandingCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Seeds the singleton instance branding document on application boot.
 *
 * <p>If no {@code organization_config} record exists, one is created from the active
 * {@code WHITE_LABEL_*} overrides layered on platform defaults. The Redis branding cache
 * is always evicted on boot so that changed environment variables take effect after a
 * container restart, without rebuilding images.</p>
 *
 * <p>Failures are logged and never block startup; the API then serves platform defaults.</p>
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class BrandingBootstrapRunner implements CommandLineRunner {

    private final OrganizationConfigRepository repository;
    private final RedisBrandingCacheService cache;
    private final BrandingEnvironmentProperties envProperties;

    @Override
    public void run(String... args) {
        cache.evict();
        try {
            if (repository.findConfig().isPresent()) {
                log.info("[BrandingBootstrapRunner] Instance branding configuration present (source: database{}).",
                        envProperties.hasOverrides() ? ", env overrides " + envProperties.activeOverrides() : "");
                return;
            }
            repository.save(seedConfig());
            log.info("[BrandingBootstrapRunner] Initialized instance branding configuration. (source: {})",
                    envProperties.hasOverrides() ? "environment " + envProperties.activeOverrides() : "defaults");
        } catch (Exception e) {
            log.warn("[BrandingBootstrapRunner] Could not initialize branding configuration; serving defaults: {}",
                    e.getMessage());
        }
    }

    OrganizationConfig seedConfig() {
        OrganizationConfig seed = OrganizationConfig.createDefault();
        apply(envProperties.effectiveProductName(), seed::setProductName);
        apply(envProperties.effectiveOrgName(), seed::setOrganizationName);
        apply(envProperties.effectiveLogoUrl(), seed::setLogoUrl);
        apply(envProperties.effectiveFaviconUrl(), seed::setFaviconUrl);
        apply(envProperties.effectivePrimaryColor(), seed::setPrimaryColor);
        apply(envProperties.effectiveSecondaryColor(), seed::setSecondaryColor);
        apply(envProperties.effectiveFooterText(), seed::setFooterText);
        seed.setUpdatedAt(LocalDateTime.now());
        return seed;
    }

    private static void apply(String value, Consumer<String> setter) {
        if (value != null) setter.accept(value);
    }
}
