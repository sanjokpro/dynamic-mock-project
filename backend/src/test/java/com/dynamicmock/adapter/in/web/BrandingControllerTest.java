package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.BrandingConfigRequest;
import com.dynamicmock.application.port.in.OrganizationConfigUseCase;
import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.infrastructure.filter.ApiKeyAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BrandingControllerTest {

    private OrganizationConfigUseCase brandingUseCase;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        brandingUseCase = mock(OrganizationConfigUseCase.class);
        BrandingController rawController = new BrandingController(brandingUseCase);

        // Enforce @PreAuthorize on controller methods matching SecurityConfig's @EnableMethodSecurity
        ProxyFactory proxyFactory = new ProxyFactory(rawController);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice((MethodBeforeAdvice) (method, args, target) -> {
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
            if (preAuthorize != null) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth == null || !auth.isAuthenticated()) {
                    throw new AccessDeniedException("Authentication required");
                }
                boolean isAdminName = "admin".equals(auth.getName());
                boolean hasAdminRole = auth.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                if (!isAdminName && !hasAdminRole) {
                    throw new AccessDeniedException("Forbidden: Admin privileges required");
                }
            }
        });
        BrandingController proxiedController = (BrandingController) proxyFactory.getProxy();

        HandlerExceptionResolver securityExceptionResolver = (request, response, handler, ex) -> {
            if (ex instanceof AccessDeniedException) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                int status = (auth == null) ? 401 : 403;
                response.setStatus(status);
                return new ModelAndView();
            }
            return null;
        };

        mockMvc = MockMvcBuilders.standaloneSetup(proxiedController)
                .setHandlerExceptionResolvers(
                        securityExceptionResolver,
                        new DefaultHandlerExceptionResolver()
                )
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("GET /api/config/branding is publicly accessible without authentication")
    void testGetBranding_PublicOk() throws Exception {
        OrganizationConfig defaults = OrganizationConfig.createDefault();
        when(brandingUseCase.getEffectiveConfig()).thenReturn(defaults);

        mockMvc.perform(get("/api/config/branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Dynamic Mock"))
                .andExpect(jsonPath("$.primaryColor").value("#3b82f6"))
                .andExpect(jsonPath("$.secondaryColor").value("#f1f5f9"));
    }

    @Test
    @DisplayName("PUT /api/config/branding rejects unauthenticated caller with 401")
    void testUpdateBranding_UnauthenticatedRejected() throws Exception {
        BrandingConfigRequest req = BrandingConfigRequest.builder()
                .productName("Hacked")
                .primaryColor("#000000")
                .build();

        mockMvc.perform(put("/api/config/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verify(brandingUseCase, never()).updateConfig(any());
    }

    @Test
    @DisplayName("PUT /api/config/branding rejects authenticated non-admin user with 403 Forbidden")
    void testUpdateBranding_NonAdminRejected() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("developer", null, Collections.emptyList())
        );

        BrandingConfigRequest req = BrandingConfigRequest.builder()
                .productName("Unauthorized Brand")
                .primaryColor("#7c3aed")
                .build();

        mockMvc.perform(put("/api/config/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(brandingUseCase, never()).updateConfig(any());
    }

    @Test
    @DisplayName("PUT /api/config/branding allows authenticated admin user and returns updated config")
    void testUpdateBranding_AdminAllowed() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        BrandingConfigRequest req = BrandingConfigRequest.builder()
                .productName("Acme Test Hub")
                .organizationName("Acme Corp")
                .logoUrl("https://cdn.acme.com/logo.svg")
                .faviconUrl("/favicon.ico")
                .primaryColor("#8b5cf6")
                .secondaryColor("#f1f5f9")
                .footerText("Acme Integration Platform")
                .build();

        OrganizationConfig saved = req.toDomain();
        when(brandingUseCase.updateConfig(any(OrganizationConfig.class))).thenReturn(saved);

        mockMvc.perform(put("/api/config/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Acme Test Hub"))
                .andExpect(jsonPath("$.primaryColor").value("#8b5cf6"))
                .andExpect(jsonPath("$.logoUrl").value("https://cdn.acme.com/logo.svg"));
    }

    @Test
    @DisplayName("PUT /api/config/branding rejects CSS injection in primaryColor with 400 Bad Request")
    void testUpdateBranding_RejectsInvalidHexColor() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, Collections.emptyList())
        );

        BrandingConfigRequest req = BrandingConfigRequest.builder()
                .productName("Acme")
                .primaryColor("#3b82f6; background: red")
                .build();

        mockMvc.perform(put("/api/config/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(brandingUseCase, never()).updateConfig(any());
    }

    @Test
    @DisplayName("PUT /api/config/branding rejects javascript: URI in logoUrl with 400 Bad Request")
    void testUpdateBranding_RejectsJavascriptLogoUrl() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, Collections.emptyList())
        );

        BrandingConfigRequest req = BrandingConfigRequest.builder()
                .productName("Acme")
                .logoUrl("javascript:alert(document.cookie)")
                .primaryColor("#3b82f6")
                .build();

        mockMvc.perform(put("/api/config/branding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(brandingUseCase, never()).updateConfig(any());
    }

    @Test
    @DisplayName("POST /api/config/branding/reset allows admin and rejects non-admin")
    void testResetBranding_AdminGuard() throws Exception {
        when(brandingUseCase.resetToDefault()).thenReturn(OrganizationConfig.createDefault());

        // Non-admin rejected
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("viewer", null, Collections.emptyList())
        );
        mockMvc.perform(post("/api/config/branding/reset"))
                .andExpect(status().isForbidden());

        // Admin allowed
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, Collections.emptyList())
        );
        mockMvc.perform(post("/api/config/branding/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Dynamic Mock"));
    }

    @Test
    @DisplayName("ApiKeyAuthFilter permits unauthenticated GET /api/config/branding but blocks unauthenticated PUT")
    void testApiKeyFilter_PermitsPublicGetOnly() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest getReq = new MockHttpServletRequest("GET", "/api/config/branding");
        MockHttpServletResponse getRes = new MockHttpServletResponse();
        filter.doFilter(getReq, getRes, chain);
        verify(chain, times(1)).doFilter(getReq, getRes);
        assertThat(getRes.getStatus()).isEqualTo(200);

        FilterChain putChain = mock(FilterChain.class);
        MockHttpServletRequest putReq = new MockHttpServletRequest("PUT", "/api/config/branding");
        MockHttpServletResponse putRes = new MockHttpServletResponse();
        filter.doFilter(putReq, putRes, putChain);
        verify(putChain, never()).doFilter(putReq, putRes);
        assertThat(putRes.getStatus()).isEqualTo(401);
    }
}
