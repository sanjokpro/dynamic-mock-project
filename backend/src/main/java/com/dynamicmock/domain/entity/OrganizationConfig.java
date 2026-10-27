package com.dynamicmock.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Pure domain entity representing instance-wide organization branding configuration.
 * Clean Architecture compliant: zero Spring Data, Jackson, or Jakarta annotations.
 * Only Lombok is permitted for boilerplate reduction.
 *
 * This entity is scoped to a single deployment instance using SINGLETON_ID = "instance".
 * It is NOT multi-tenant. No tenant routing, subdomain, or workspace hierarchy exists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationConfig {

    /**
     * Fixed singleton document identifier. Always "instance".
     * Single-tenant architecture: one config per deployment.
     */
    public static final String SINGLETON_ID = "instance";

    private String id;

    /** Product name displayed in the UI header and browser tab title. */
    private String productName;

    /** Full legal organization or company name. */
    private String organizationName;

    /** URL of the custom logo image (HTTPS URL or base64 data URI). Null = use default icon. */
    private String logoUrl;

    /** URL of the custom favicon. Null = use default /favicon.ico. */
    private String faviconUrl;

    /** Hex color string for the primary brand color, e.g. "#3b82f6". */
    private String primaryColor;

    /** Hex color string for the secondary brand accent, e.g. "#f1f5f9". */
    private String secondaryColor;

    /** Hex color string for tertiary accent (optional, may be null). */
    private String accentColor;

    /** Optional URL for a branded login page background image. */
    private String loginBackgroundUrl;

    /** Footer/legal text displayed on the login page. */
    private String footerText;

    private LocalDateTime updatedAt;

    /**
     * Returns the hardcoded default configuration matching the original Dynamic Mock branding.
     * Used as the ultimate fallback when no database record or environment variable overrides are set.
     */
    public static OrganizationConfig createDefault() {
        return OrganizationConfig.builder()
                .id(SINGLETON_ID)
                .productName("Dynamic Mock")
                .organizationName("Dynamic Mock Open Source")
                .logoUrl(null)
                .faviconUrl("/favicon.ico")
                .primaryColor("#3b82f6")
                .secondaryColor("#f1f5f9")
                .accentColor("#e2e8f0")
                .loginBackgroundUrl(null)
                .footerText("Dynamic Mock — Open Source Enterprise Integration Simulation Platform")
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
