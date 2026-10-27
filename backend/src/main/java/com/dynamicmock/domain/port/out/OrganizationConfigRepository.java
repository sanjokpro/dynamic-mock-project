package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.OrganizationConfig;

import java.util.Optional;

/**
 * Outbound repository port for organization configuration persistence.
 * Domain layer interface — implementations reside in the infrastructure layer.
 */
public interface OrganizationConfigRepository {

    /**
     * Retrieves the singleton instance branding configuration.
     * Returns empty if no record has been persisted yet.
     */
    Optional<OrganizationConfig> findConfig();

    /**
     * Persists the organization configuration. Upserts by SINGLETON_ID.
     */
    OrganizationConfig save(OrganizationConfig config);

    /**
     * Deletes the stored configuration, effectively resetting to defaults on next request.
     */
    void deleteConfig();
}
