package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.OrganizationConfigMongoEntity;
import org.springframework.stereotype.Component;

/**
 * Bidirectional mapper between OrganizationConfig domain entity and
 * OrganizationConfigMongoEntity persistence entity.
 * Null-safe for both directions.
 */
@Component
public class OrganizationConfigMapper {

    public OrganizationConfig toDomain(OrganizationConfigMongoEntity entity) {
        if (entity == null) return null;
        return OrganizationConfig.builder()
                .id(entity.getId())
                .productName(entity.getProductName())
                .organizationName(entity.getOrganizationName())
                .logoUrl(entity.getLogoUrl())
                .faviconUrl(entity.getFaviconUrl())
                .primaryColor(entity.getPrimaryColor())
                .secondaryColor(entity.getSecondaryColor())
                .accentColor(entity.getAccentColor())
                .loginBackgroundUrl(entity.getLoginBackgroundUrl())
                .footerText(entity.getFooterText())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public OrganizationConfigMongoEntity toEntity(OrganizationConfig domain) {
        if (domain == null) return null;
        return OrganizationConfigMongoEntity.builder()
                .id(domain.getId())
                .productName(domain.getProductName())
                .organizationName(domain.getOrganizationName())
                .logoUrl(domain.getLogoUrl())
                .faviconUrl(domain.getFaviconUrl())
                .primaryColor(domain.getPrimaryColor())
                .secondaryColor(domain.getSecondaryColor())
                .accentColor(domain.getAccentColor())
                .loginBackgroundUrl(domain.getLoginBackgroundUrl())
                .footerText(domain.getFooterText())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
