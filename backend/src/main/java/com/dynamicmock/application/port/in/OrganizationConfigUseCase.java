package com.dynamicmock.application.port.in;

import com.dynamicmock.domain.entity.OrganizationConfig;

/**
 * Inbound use case port for organization branding configuration management.
 * Defines the operations available to web adapter callers.
 */
public interface OrganizationConfigUseCase {

    /**
     * Returns the effective branding configuration, resolving priority:
     * 1. Environment variable overrides (WHITE_LABEL_*)
     * 2. Database stored configuration
     * 3. Hardcoded platform defaults
     */
    OrganizationConfig getEffectiveConfig();

    /**
     * Persists an admin-submitted branding configuration update.
     * Evicts Redis cache on successful persistence.
     *
     * @param config The new branding configuration (id will be set to SINGLETON_ID).
     * @return The saved effective configuration after applying env var overrides.
     */
    OrganizationConfig updateConfig(OrganizationConfig config);

    /**
     * Resets the branding configuration to platform defaults.
     * Removes the stored document and evicts the Redis cache.
     *
     * @return The default configuration.
     */
    OrganizationConfig resetToDefault();
}
