package com.dynamicmock.core.dispatcher;

import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.infrastructure.filter.RequestMatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class RequestMatcherTest {

    @Mock
    private HttpServletRequest request;

    private RequestMatcher requestMatcher;
    private MockRoute route;

    @BeforeEach
    void setUp() {
        requestMatcher = new RequestMatcher();
        route = MockRoute.builder()
                .id("route")
                .path("/test")
                .method("GET")
                .matchers(new HashMap<>())
                .build();
    }

    private boolean matches(Map<String, String> headers, Map<String, String> query, String bodyType, String bodyPattern, String body) {
        Map<String, Object> matchers = new HashMap<>();
        if (headers != null) matchers.put("headers", headers);
        if (query != null) matchers.put("queryParams", query);
        if (bodyType != null) {
            matchers.put("bodyMatchType", bodyType);
            matchers.put("bodyMatchPattern", bodyPattern);
        }
        route.setMatchers(matchers);
        return requestMatcher.matches(request, route, body);
    }

    @Test
    void matchHeaders_shouldMatchExactHeader() {
        Map<String, String> headerMatchers = new HashMap<>();
        headerMatchers.put("Content-Type", "application/json");
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        assertTrue(matches(headerMatchers, null, null, null, null));
    }

    @Test
    void matchHeaders_shouldMatchHeaderWithRegex() {
        Map<String, String> headerMatchers = new HashMap<>();
        headerMatchers.put("Authorization", "Bearer .*");
        when(request.getHeader("Authorization")).thenReturn("Bearer abc123token");
        assertTrue(matches(headerMatchers, null, null, null, null));
    }

    @Test
    void matchHeaders_shouldFailWhenHeaderMissing() {
        Map<String, String> headerMatchers = new HashMap<>();
        headerMatchers.put("X-Custom-Header", "value");
        when(request.getHeader("X-Custom-Header")).thenReturn(null);
        assertFalse(matches(headerMatchers, null, null, null, null));
    }

    @Test
    void matchHeaders_shouldFailWhenHeaderDoesNotMatch() {
        Map<String, String> headerMatchers = new HashMap<>();
        headerMatchers.put("Content-Type", "application/json");
        when(request.getHeader("Content-Type")).thenReturn("text/plain");
        assertFalse(matches(headerMatchers, null, null, null, null));
    }

    @Test
    void matchHeaders_shouldReturnTrueWhenNoMatchers() {
        assertTrue(matches(new HashMap<>(), null, null, null, null));
    }

    @Test
    void matchQueryParams_shouldMatchExactParam() {
        Map<String, String> queryMatchers = new HashMap<>();
        queryMatchers.put("page", "1");
        when(request.getParameter("page")).thenReturn("1");
        assertTrue(matches(null, queryMatchers, null, null, null));
    }

    @Test
    void matchQueryParams_shouldMatchParamWithRegex() {
        Map<String, String> queryMatchers = new HashMap<>();
        queryMatchers.put("id", "\\d+");
        when(request.getParameter("id")).thenReturn("12345");
        assertTrue(matches(null, queryMatchers, null, null, null));
    }

    @Test
    void matchQueryParams_shouldFailWhenParamMissing() {
        Map<String, String> queryMatchers = new HashMap<>();
        queryMatchers.put("required", "value");
        when(request.getParameter("required")).thenReturn(null);
        assertFalse(matches(null, queryMatchers, null, null, null));
    }

    @Test
    void matchQueryParams_shouldReturnTrueWhenNoMatchers() {
        assertTrue(matches(null, null, null, null, null));
    }

    @Test
    void matchBody_shouldMatchJsonPath() {
        assertTrue(matches(null, null, "jsonpath", "$.name", "{\"name\": \"John\", \"age\": 30}"));
    }

    @Test
    void matchBody_shouldMatchJsonPathWithRegex() {
        assertTrue(matches(null, null, "jsonpath", "$.email", "{\"email\": \"user@example.com\"}"));
    }

    @Test
    void matchBody_shouldFailWhenJsonPathNotFound() {
        assertFalse(matches(null, null, "jsonpath", "$.nonexistent", "{\"name\": \"John\"}"));
    }

    @Test
    void matchBody_shouldReturnFalseWhenBodyMissing() {
        assertFalse(matches(null, null, "equals", "{}", null));
    }

    @Test
    void matchBody_shouldHandleInvalidJson() {
        assertFalse(matches(null, null, "jsonpath", "$.name == John", "not json"));
    }

    @Test
    void matchAll_shouldMatchAllCriteria() {
        Map<String, String> headerMatchers = new HashMap<>();
        headerMatchers.put("Content-Type", "application/json");
        Map<String, String> queryMatchers = new HashMap<>();
        queryMatchers.put("page", "\\d+");
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        when(request.getParameter("page")).thenReturn("1");
        assertTrue(matches(headerMatchers, queryMatchers, "jsonpath", "$.name == John", "{\"name\": \"John\"}"));
    }

    @Test
    void matchAll_shouldFailWhenAnyMatchFails() {
        Map<String, String> headerMatchers = new HashMap<>();
        headerMatchers.put("Content-Type", "application/json");
        Map<String, String> queryMatchers = new HashMap<>();
        queryMatchers.put("page", "\\d+");
        when(request.getHeader("Content-Type")).thenReturn("text/plain");
        when(request.getParameter("page")).thenReturn("1");
        // Do not stub matchers for body to avoid unnecessary stubbing
        assertFalse(matches(headerMatchers, queryMatchers, null, null, "{\"name\": \"John\"}"));
    }
}

