package com.dynamicmock.infrastructure.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Binds the {@code white-label.*} configuration namespace, populated from the
 * {@code WHITE_LABEL_*} container environment variables (see application.yml).
 *
 * <p>Every property defaults to empty. Only non-blank values are treated as
 * operator overrides, which take precedence over the stored database configuration.
 * Platform defaults live in {@code OrganizationConfig.createDefault()}, not here, so
 * that an unset variable never masks values saved through the admin UI.</p>
 *
 * <p>Values are validated with the same rules as the admin API. An invalid override
 * (e.g. a malformed colour that could inject CSS) is ignored with a warning instead of
 * being served to browsers.</p>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "white-label")
public class BrandingEnvironmentProperties {

    static final Pattern HEX_COLOR = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    static final Pattern ASSET_URL = Pattern.compile("^(https?://|data:image/).+");
    /** Favicon additionally allows a local absolute path such as {@code /favicon.ico}. */
    static final Pattern FAVICON_URL = Pattern.compile("^(https?://|data:image/|/(?!/)).+");

    static final int MAX_PRODUCT_NAME = 64;
    static final int MAX_ORG_NAME = 100;
    static final int MAX_FOOTER_TEXT = 255;

    private String productName;
    private String orgName;
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor;
    private String secondaryColor;
    private String footerText;

    public String effectiveProductName() {
        return maxLength("product-name", productName, MAX_PRODUCT_NAME);
    }

    public String effectiveOrgName() {
        return maxLength("org-name", orgName, MAX_ORG_NAME);
    }

    public String effectiveLogoUrl() {
        return matching("logo-url", logoUrl, ASSET_URL);
    }

    public String effectiveFaviconUrl() {
        return matching("favicon-url", faviconUrl, FAVICON_URL);
    }

    public String effectivePrimaryColor() {
        return matching("primary-color", primaryColor, HEX_COLOR);
    }

    public String effectiveSecondaryColor() {
        return matching("secondary-color", secondaryColor, HEX_COLOR);
    }

    public String effectiveFooterText() {
        return maxLength("footer-text", footerText, MAX_FOOTER_TEXT);
    }

    /** Returns the names of the properties currently supplying a valid override. */
    public List<String> activeOverrides() {
        List<String> active = new ArrayList<>();
        if (effectiveProductName() != null) active.add("product-name");
        if (effectiveOrgName() != null) active.add("org-name");
        if (effectiveLogoUrl() != null) active.add("logo-url");
        if (effectiveFaviconUrl() != null) active.add("favicon-url");
        if (effectivePrimaryColor() != null) active.add("primary-color");
        if (effectiveSecondaryColor() != null) active.add("secondary-color");
        if (effectiveFooterText() != null) active.add("footer-text");
        return Collections.unmodifiableList(active);
    }

    public boolean hasOverrides() {
        return !activeOverrides().isEmpty();
    }

    private static String maxLength(String name, String value, int max) {
        String trimmed = trimToNull(value);
        if (trimmed != null && trimmed.length() > max) {
            log.warn("Ignoring white-label.{} override: exceeds {} characters", name, max);
            return null;
        }
        return trimmed;
    }

    private static String matching(String name, String value, Pattern pattern) {
        String trimmed = trimToNull(value);
        if (trimmed != null && !pattern.matcher(trimmed).matches()) {
            log.warn("Ignoring white-label.{} override: invalid format", name);
            return null;
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
