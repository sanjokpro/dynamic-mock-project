package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.OrganizationConfig;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validated request body for {@code PUT /api/config/branding}.
 *
 * <p>Represents a full replacement of the stored branding. Omitted (null) or blank
 * fields fall back to platform defaults when the effective configuration is resolved.</p>
 *
 * <p>Security: colours are restricted to hex notation (no {@code ; { } ( )} characters,
 * preventing CSS injection) and asset URLs to {@code http(s)://} or {@code data:image/}
 * schemes, rejecting {@code javascript:}, {@code vbscript:} and {@code data:text/html}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandingConfigRequest {

    public static final String HEX_COLOR_REGEX = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    public static final String ASSET_URL_REGEX = "^(https?://|data:image/).+";
    /** Favicons may also reference a same-origin absolute path such as {@code /favicon.ico}. */
    public static final String FAVICON_URL_REGEX = "^(https?://|data:image/|/(?!/)).+";

    @Size(max = 64, message = "Product name must be 64 characters or fewer")
    private String productName;

    @Size(max = 100, message = "Organization name must be 100 characters or fewer")
    private String organizationName;

    @Pattern(regexp = ASSET_URL_REGEX, message = "Logo URL must be an http(s) URL or a data:image/ URI")
    private String logoUrl;

    @Pattern(regexp = FAVICON_URL_REGEX, message = "Favicon URL must be an http(s) URL, a data:image/ URI, or an absolute path")
    private String faviconUrl;

    @Pattern(regexp = HEX_COLOR_REGEX, message = "Primary color must be a hex color such as #3b82f6")
    private String primaryColor;

    @Pattern(regexp = HEX_COLOR_REGEX, message = "Secondary color must be a hex color such as #f1f5f9")
    private String secondaryColor;

    @Pattern(regexp = HEX_COLOR_REGEX, message = "Accent color must be a hex color such as #e2e8f0")
    private String accentColor;

    @Pattern(regexp = ASSET_URL_REGEX, message = "Login background URL must be an http(s) URL or a data:image/ URI")
    private String loginBackgroundUrl;

    @Size(max = 255, message = "Footer text must be 255 characters or fewer")
    private String footerText;

    public OrganizationConfig toDomain() {
        return OrganizationConfig.builder()
                .id(OrganizationConfig.SINGLETON_ID)
                .productName(trimToNull(productName))
                .organizationName(trimToNull(organizationName))
                .logoUrl(trimToNull(logoUrl))
                .faviconUrl(trimToNull(faviconUrl))
                .primaryColor(trimToNull(primaryColor))
                .secondaryColor(trimToNull(secondaryColor))
                .accentColor(trimToNull(accentColor))
                .loginBackgroundUrl(trimToNull(loginBackgroundUrl))
                .footerText(trimToNull(footerText))
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
