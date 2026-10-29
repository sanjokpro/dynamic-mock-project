package com.dynamicmock.core.dispatcher;

import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.infrastructure.filter.DynamicRouteDispatcher;
import com.dynamicmock.infrastructure.filter.RequestMatcher;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class DynamicRouteDispatcherTest {

    @Mock
    private RouteRegistry routeRegistry;

    @Mock
    private ResponseTemplateEngine templateEngine;

    @Mock
    private ScriptEngine scriptEngine;

    @Mock
    private ScenarioService scenarioService;

    @Mock
    private RequestMatcher requestMatcher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private com.dynamicmock.application.service.TrafficLogger trafficLogger;

    @Mock
    private com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private DynamicRouteDispatcher dispatcher;

    private MockRoute testRoute;
    private RouteRegistry.RouteMatch baseMatch;

    @BeforeEach
    void setUp() {
        testRoute = MockRoute.builder()
                .id("test-id")
                .path("/mock/test")
                .method("GET")
                .responseTemplate("{\"message\": \"success\"}")
                .responseStatus(200)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        baseMatch = RouteRegistry.RouteMatch.builder()
                .route(testRoute)
                .pathVariables(Map.of())
                .build();

        // Default mocks
        when(request.getHeaderNames()).thenReturn(java.util.Collections.emptyEnumeration());
        when(request.getParameterMap()).thenReturn(java.util.Collections.emptyMap());
        when(request.getContentLength()).thenReturn(0);
    }

    @Test
    void preHandle_shouldReturnTrueForNonMockPath() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/routes");
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // When
        boolean result = dispatcher.preHandle(request, response, new Object());

        // Then
        assertTrue(result);
        verify(routeRegistry, never()).findRoutes(anyString(), anyString());
    }

    @Test
    void preHandle_shouldHandleMockRoute() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/mock/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(routeRegistry.findRoutes("GET", "/test")).thenReturn(new java.util.ArrayList<>(java.util.List.of(baseMatch)));
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        when(templateEngine.render(anyString(), any())).thenReturn("{\"message\": \"success\"}");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = dispatcher.preHandle(request, response, new Object());

        // Then
        assertFalse(result); // False because we handled the request
        verify(response).setStatus(200);
        verify(response).setContentType("application/json");
    }

    @Test
    void preHandle_shouldReturn404WhenRouteNotFound() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/mock/notfound");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(routeRegistry.findRoutes("GET", "/notfound")).thenReturn(java.util.List.of());
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // When
        boolean result = dispatcher.preHandle(request, response, new Object());

        // Then
        assertFalse(result);
        verify(response).setStatus(404);
    }

    @Test
    void preHandle_shouldApplyDelay() throws Exception {
        // Given
        testRoute.setDelayMs(100);
        when(request.getRequestURI()).thenReturn("/mock/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(routeRegistry.findRoutes("GET", "/test")).thenReturn(new java.util.ArrayList<>(java.util.List.of(baseMatch)));
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        when(templateEngine.render(anyString(), any())).thenReturn("{\"message\": \"success\"}");
        
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        long startTime = System.currentTimeMillis();

        // When
        dispatcher.preHandle(request, response, new Object());

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Then
        assertTrue(elapsedTime >= 100);
    }

    @Test
    void preHandle_shouldApplyResponseHeaders() throws Exception {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Header", "custom-value");
        headers.put("Cache-Control", "no-cache");
        testRoute.setResponseHeaders(headers);
        
        when(request.getRequestURI()).thenReturn("/mock/test");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(routeRegistry.findRoutes("GET", "/test")).thenReturn(new java.util.ArrayList<>(java.util.List.of(baseMatch)));
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        when(templateEngine.render(anyString(), any())).thenReturn("{\"message\": \"success\"}");
        
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // When
        dispatcher.preHandle(request, response, new Object());

        // Then
        verify(response, atLeastOnce()).setHeader("X-Custom-Header", "custom-value");
        verify(response, atLeastOnce()).setHeader("Cache-Control", "no-cache");
    }

    @Test
    void preHandle_shouldExtractPathVariables() throws Exception {
        // Given
        testRoute.setPath("/mock/users/{id}");
        testRoute.setResponseTemplate("{\"userId\": \"{{pathVariables.id}}\"}");

        RouteRegistry.RouteMatch matchWithVars = RouteRegistry.RouteMatch.builder()
                .route(testRoute)
                .pathVariables(Map.of("id", "123"))
                .build();
        when(request.getRequestURI()).thenReturn("/mock/users/123");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(routeRegistry.findRoutes("GET", "/users/123")).thenReturn(new java.util.ArrayList<>(java.util.List.of(matchWithVars)));
        when(templateEngine.render(anyString(), any())).thenReturn("{\"userId\": \"123\"}");
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // When
        dispatcher.preHandle(request, response, new Object());

        // Then
        verify(templateEngine).render(anyString(), argThat(context -> {
            Map<String, Object> requestCtx = (Map<String, Object>) context.get("request");
            if (requestCtx == null) {
                return false;
            }
            Map<String, String> pathVars = (Map<String, String>) requestCtx.get("pathVariables");
            return pathVars != null && "123".equals(pathVars.get("id"));
        }));
    }

    @Test
    void preHandle_postScriptCanReadAndMutateRenderedTemplate() throws Exception {
        // Given
        testRoute.setPath("/mock/orders");
        testRoute.setResponseTemplate("{\"trackingCode\": \"ABC\"}");
        testRoute.setPostScript("response.body = response.body.replace('ABC', 'ABCX');");

        when(request.getRequestURI()).thenReturn("/mock/orders");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(routeRegistry.findRoutes("GET", "/orders")).thenReturn(new java.util.ArrayList<>(java.util.List.of(baseMatch)));
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        when(templateEngine.render(anyString(), any())).thenReturn("{\"trackingCode\": \"ABC\"}");

        doAnswer(invocation -> {
            com.dynamicmock.adapter.out.script.ScriptContext ctx = invocation.getArgument(2);
            org.junit.jupiter.api.Assertions.assertEquals("{\"trackingCode\": \"ABC\"}", ctx.getResponseBody());
            ctx.setResponseBody("{\"trackingCode\": \"ABCX\"}");
            return com.dynamicmock.adapter.out.script.ScriptEngine.ScriptExecutionResult.builder()
                    .context(ctx)
                    .build();
        }).when(scriptEngine).execute(anyString(), any(), any());

        StringWriter responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // When
        dispatcher.preHandle(request, response, new Object());

        // Then
        org.junit.jupiter.api.Assertions.assertEquals("{\"trackingCode\": \"ABCX\"}", responseWriter.toString());
    }

    @Test
    void preHandle_shouldCaptureQueryStringAndResponseTelemetryAndSetCorsHeaders() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/mock/mobile-banking-login");
        when(request.getQueryString()).thenReturn("action=login&returnUrl=http://localhost/mock/dest");
        when(request.getParameterMap()).thenReturn(Map.of(
                "action", new String[]{"login"},
                "returnUrl", new String[]{"http://localhost/mock/dest"}
        ));
        when(request.getMethod()).thenReturn("GET");
        when(routeRegistry.findRoutes("GET", "/mobile-banking-login"))
                .thenReturn(new java.util.ArrayList<>(java.util.List.of(baseMatch)));
        when(requestMatcher.matches(any(), any(), any())).thenReturn(true);
        when(templateEngine.render(anyString(), any())).thenReturn("<html><body>OK</body></html>");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // When
        boolean result = dispatcher.preHandle(request, response, new Object());

        // Then
        assertFalse(result);
        verify(response).setHeader("Access-Control-Allow-Origin", "*");
        org.mockito.ArgumentCaptor<com.dynamicmock.application.service.TrafficLogger.ExecutionEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.class);
        verify(trafficLogger).log(captor.capture());

        com.dynamicmock.application.service.TrafficLogger.ExecutionEvent event = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("/mobile-banking-login", event.getPath());
        org.junit.jupiter.api.Assertions.assertEquals("action=login&returnUrl=http://localhost/mock/dest", event.getQueryString());
        org.junit.jupiter.api.Assertions.assertEquals("login", event.getQueryParams().get("action"));
        org.junit.jupiter.api.Assertions.assertEquals(200, event.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("<html><body>OK</body></html>", event.getResponseBody());
        assertTrue(event.getDurationMs() >= 0.0);
    }

    @Test
    void preHandle_shouldReturn200ForOptionsPreflightOnMockRoutes() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/mock/mobile-banking-login");
        when(request.getMethod()).thenReturn("OPTIONS");
        when(routeRegistry.findRoutes("OPTIONS", "/mobile-banking-login")).thenReturn(java.util.List.of());

        // When
        boolean result = dispatcher.preHandle(request, response, new Object());

        // Then
        assertFalse(result);
        verify(response).setHeader("Access-Control-Allow-Origin", "*");
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }
}


