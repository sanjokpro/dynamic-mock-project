package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.OrganizationConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Public response body for the effective instance branding configuration.
 * Contains presentation data only — never credentials or infrastructure details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingConfigResponse {

    private String productName;
    private String organizationName;
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String loginBackgroundUrl;
    private String footerText;
    private LocalDateTime updatedAt;

    public static BrandingConfigResponse from(OrganizationConfig domain) {
        if (domain == null) return null;
        return BrandingConfigResponse.builder()
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
