package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.BrandingConfigRequest;
import com.dynamicmock.adapter.in.web.dto.BrandingConfigResponse;
import com.dynamicmock.application.port.in.OrganizationConfigUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Instance-wide (single-tenant) branding configuration API.
 *
 * <ul>
 *   <li>{@code GET /api/config/branding} — public, so the login screen can be branded
 *       before authentication.</li>
 *   <li>{@code PUT /api/config/branding} — administrator only.</li>
 *   <li>{@code POST /api/config/branding/reset} — administrator only.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/config/branding")
@RequiredArgsConstructor
public class BrandingController {

    static final String ADMIN_GUARD = "hasRole('ADMIN') or authentication.name == 'admin'";

    private final OrganizationConfigUseCase brandingUseCase;

    @GetMapping
    public ResponseEntity<BrandingConfigResponse> getBranding() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(BrandingConfigResponse.from(brandingUseCase.getEffectiveConfig()));
    }

    @PutMapping
    @PreAuthorize(ADMIN_GUARD)
    public ResponseEntity<BrandingConfigResponse> updateBranding(@Valid @RequestBody BrandingConfigRequest request) {
        return ResponseEntity.ok(BrandingConfigResponse.from(brandingUseCase.updateConfig(request.toDomain())));
    }

    @PostMapping("/reset")
    @PreAuthorize(ADMIN_GUARD)
    public ResponseEntity<BrandingConfigResponse> resetBranding() {
        return ResponseEntity.ok(BrandingConfigResponse.from(brandingUseCase.resetToDefault()));
    }
}
