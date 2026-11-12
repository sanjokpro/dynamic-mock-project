package com.dynamicmock.acceptance;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.application.service.PostmanImportService;
import com.dynamicmock.application.service.PostmanImportService.ImportReport;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.application.service.WorkspaceCollectionService;
import com.dynamicmock.application.service.WorkspaceEnvironmentService;
import com.dynamicmock.domain.entity.Collection;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.infrastructure.filter.DynamicRouteDispatcher;
import com.dynamicmock.infrastructure.filter.RequestMatcher;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * End-to-End Acceptance Test Suite for v0.6.2 Zero-Touch Postman Import & Dispatch Fidelity.
 * Validates against the authoritative fixture: postman/esewa-banksmart-mock.json
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("v0.6.2 Zero-Touch Postman Compatibility Acceptance Test")
class PostmanZeroTouchAcceptanceTest {

    @Mock
    private RouteService routeService;

    @Mock
    private WorkspaceCollectionService collectionService;

    @Mock
    private WorkspaceEnvironmentService environmentService;

    @Mock
    private ScriptEngine scriptEngine;

    @Mock
    private com.dynamicmock.application.service.ScenarioService scenarioService;

    @Mock
    private com.dynamicmock.application.service.TrafficLogger trafficLogger;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private com.dynamicmock.application.service.AsyncWebhookService asyncWebhookService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private PostmanImportService importService;
    private ResponseTemplateEngine templateEngine;
    private byte[] bankSmartFixtureBytes;

    private final Map<String, MockRoute> routeDatabase = new LinkedHashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        templateEngine = new ResponseTemplateEngine(scriptEngine, new ObjectMapper());
        importService = new PostmanImportService(routeService, collectionService, environmentService, new ObjectMapper());

        try (InputStream in = getClass().getClassLoader().getResourceAsStream("postman/esewa-banksmart-mock.json")) {
            assertNotNull(in, "BankSmart fixture must exist at postman/esewa-banksmart-mock.json");
            bankSmartFixtureBytes = in.readAllBytes();
        }

        routeDatabase.clear();
        when(routeService.createRoute(any(CreateRouteRequest.class))).thenAnswer(invocation -> {
            CreateRouteRequest req = invocation.getArgument(0, CreateRouteRequest.class);
            String id = req.getId() != null ? req.getId() : "route-" + UUID.randomUUID();
            MockRoute r = MockRoute.builder()
                    .id(id)
                    .path(req.getPath())
                    .method(req.getMethod())
                    .matchers(req.getMatchers())
                    .queryParams(req.getQueryParams())
                    .responseTemplate(req.getResponseTemplate())
                    .responseStatus(req.getResponseStatus())
                    .responseHeaders(req.getResponseHeaders())
                    .priority(req.getPriority() != null ? req.getPriority() : 0)
                    .metadata(req.getMetadata())
                    .active(true)
                    .build();
            routeDatabase.put(id, r);
            return RouteResponse.builder().id(id).path(r.getPath()).method(r.getMethod()).active(true).build();
        });

        when(routeService.activateRoute(anyString())).thenAnswer(invocation ->
                RouteResponse.builder().id(invocation.getArgument(0, String.class)).active(true).build());

        when(collectionService.createCollection(anyString(), anyString(), anyString())).thenAnswer(invocation ->
                Collection.builder()
                        .id("col-banksmart")
                        .name(invocation.getArgument(0, String.class))
                        .userId(invocation.getArgument(1, String.class))
                        .items(new ArrayList<>())
                        .build());
    }

    private List<CreateRouteRequest> captureCreatedRoutes() {
        ArgumentCaptor<CreateRouteRequest> captor = ArgumentCaptor.forClass(CreateRouteRequest.class);
        verify(routeService, atLeastOnce()).createRoute(captor.capture());
        return captor.getAllValues();
    }

    // =========================================================================
    // Gate 5: Multi-Example Splitting & Idempotent Re-Import
    // =========================================================================

    @Test
    @DisplayName("05 & 13. Multi-Example Splitting & Idempotent Re-Import produces exactly 3 routes with 0 duplicates")
    void testMultiExampleSplittingAndIdempotentReimport() {
        // First import
        ImportReport report1 = importService.importPostmanCollection(bankSmartFixtureBytes, null);
        assertEquals(3, report1.getRoutes(), "eSewa-BankSmart-Mock must produce exactly 3 routes from its 3 examples");
        assertEquals(3, routeDatabase.size());

        List<CreateRouteRequest> routes = captureCreatedRoutes();
        assertEquals(3, routes.size());

        // Verify deterministic IDs are present
        Set<String> initialIds = new LinkedHashSet<>();
        for (CreateRouteRequest r : routes) {
            assertNotNull(r.getId(), "Every route must have a deterministic UUIDv5 ID");
            assertFalse(r.getId().isBlank());
            initialIds.add(r.getId());
        }
        assertEquals(3, initialIds.size(), "All 3 routes must have distinct deterministic IDs");

        // Re-import the exact same collection
        reset(routeService);
        when(routeService.createRoute(any(CreateRouteRequest.class))).thenAnswer(invocation -> {
            CreateRouteRequest req = invocation.getArgument(0, CreateRouteRequest.class);
            // In-place update emulation
            MockRoute existing = routeDatabase.get(req.getId());
            if (existing != null) {
                existing.setResponseTemplate(req.getResponseTemplate());
                return RouteResponse.builder().id(existing.getId()).path(existing.getPath()).method(existing.getMethod()).build();
            }
            routeDatabase.put(req.getId(), MockRoute.builder().id(req.getId()).build());
            return RouteResponse.builder().id(req.getId()).build();
        });

        ImportReport report2 = importService.importPostmanCollection(bankSmartFixtureBytes, null);
        assertEquals(3, report2.getRoutes(), "Re-import must produce exactly 3 routes");
        assertEquals(3, routeDatabase.size(), "Re-import must NOT produce duplicate routes");
    }

    // =========================================================================
    // Gate 6: URL Path Normalization & Query String Stripping
    // =========================================================================

    @Test
    @DisplayName("04. URL Path Normalization strips trailing query strings and slashes")
    void testUrlPathNormalization() {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);
        List<CreateRouteRequest> routes = captureCreatedRoutes();

        for (CreateRouteRequest r : routes) {
            assertEquals("/mbank-verification", r.getPath(),
                    "Path must be cleanly normalized to /mbank-verification without query or trailing slash");
            assertFalse(r.getPath().contains("?"), "Path must not contain query parameters");
            assertFalse(r.getPath().contains("#"), "Path must not contain hash fragments");
        }
    }

    // =========================================================================
    // Gate 6: Disabled Parameter Filtering
    // =========================================================================

    @Test
    @DisplayName("03. Disabled Parameter Filtering omits disabled Content-Type from Example 1 matchers")
    void testDisabledParameterFiltering() {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);
        List<CreateRouteRequest> routes = captureCreatedRoutes();

        // Find the portal web view route (VM=AC)
        CreateRouteRequest portalRoute = routes.stream()
                .filter(r -> r.getMatchers() != null && "AC".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();

        // In the fixture, Example 0 has Content-Type header with disabled: true
        // It must NOT appear in the route matchers
        if (portalRoute.getMatchers().containsKey("headers")) {
            Map<?, ?> headers = (Map<?, ?>) portalRoute.getMatchers().get("headers");
            assertFalse(headers.containsKey("Content-Type"),
                    "Disabled Content-Type header in Example 0 must be omitted from route matchers");
        }
    }

    // =========================================================================
    // Header Prefix Sanitization & Content-Type Integrity
    // =========================================================================

    @Test
    @DisplayName("10 & 11. Header Prefix Sanitization strips redundant 'Content-Type:' prefix and preserves HTML type")
    void testHeaderPrefixSanitizationAndHtmlIntegrity() {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);
        List<CreateRouteRequest> routes = captureCreatedRoutes();

        CreateRouteRequest portalRoute = routes.stream()
                .filter(r -> r.getMatchers() != null && "AC".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();

        assertNotNull(portalRoute.getResponseHeaders());
        String contentType = portalRoute.getResponseHeaders().get("Content-Type");
        assertNotNull(contentType);
        assertFalse(contentType.startsWith("Content-Type:"),
                "Redundant 'Content-Type:' prefix must be stripped by sanitizer");
        assertTrue(contentType.contains("text/html"), "Portal response must be text/html");
        assertTrue(portalRoute.getResponseTemplate().contains("<!DOCTYPE html>"), "Portal body must contain DOCTYPE html");
    }

    // =========================================================================
    // Gate 6: Precedence & Dispatcher Proof Case (?VM=AV&ACC=0000000000)
    // =========================================================================

    @Test
    @DisplayName("07. Precedence Proof: ?VM=AV&ACC=0000000000 deterministically selects Dormant Account (<response_code>1</response_code>)")
    void testBankSmartPrecedenceProof() throws Exception {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);

        // Prepare the 3 routes from the database
        MockRoute portalRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "AC".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();
        MockRoute successRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "AV".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();
        MockRoute dormantRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "0000000000".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("ACC")))
                .findFirst().orElseThrow();

        RouteRegistry.RouteMatch mPortal = RouteRegistry.RouteMatch.builder().route(portalRoute).pathVariables(Map.of()).build();
        RouteRegistry.RouteMatch mSuccess = RouteRegistry.RouteMatch.builder().route(successRoute).pathVariables(Map.of()).build();
        RouteRegistry.RouteMatch mDormant = RouteRegistry.RouteMatch.builder().route(dormantRoute).pathVariables(Map.of()).build();

        RouteRegistry mockRegistry = mock(RouteRegistry.class);
        // Registry returns all 3 candidates for /mbank-verification
        when(mockRegistry.findRoutes("GET", "/mbank-verification")).thenReturn(
                new ArrayList<>(List.of(mPortal, mSuccess, mDormant)));

        RequestMatcher realMatcher = new RequestMatcher();
        DynamicRouteDispatcher dispatcher = new DynamicRouteDispatcher(
                mockRegistry, scriptEngine, templateEngine, realMatcher, scenarioService,
                new ObjectMapper(), trafficLogger, environmentService, redisTemplate, asyncWebhookService);

        // Incoming client request with both VM=AV and ACC=0000000000
        Map<String, String> paramMap = Map.of("VM", "AV", "ACC", "0000000000");
        when(request.getRequestURI()).thenReturn("/mock/mbank-verification");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("VM=AV&ACC=0000000000");
        when(request.getParameter(anyString())).thenAnswer(inv -> paramMap.get(inv.getArgument(0, String.class)));
        when(request.getParameterMap()).thenReturn(Map.of(
                "VM", new String[]{"AV"},
                "ACC", new String[]{"0000000000"}
        ));
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getContentLength()).thenReturn(0);

        StringWriter out = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(out));

        // Execute dispatch
        boolean handled = dispatcher.preHandle(request, response, new Object());
        assertFalse(handled, "preHandle should handle the mock request and return false");

        // Verify Dormant Account route was selected
        ArgumentCaptor<com.dynamicmock.application.service.TrafficLogger.ExecutionEvent> captor =
                ArgumentCaptor.forClass(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.class);
        verify(trafficLogger).log(captor.capture());

        assertEquals(dormantRoute.getId(), captor.getValue().getResourceId(),
                "Deterministic tie-breaking (ACC < VM) must select the Dormant Account route");
        assertTrue(out.toString().contains("<response_code>1</response_code>"),
                "Output response must contain response_code 1 (Dormant Account)");
    }

    @Test
    @DisplayName("07b. Zero-Touch Dispatch: ?VM=AV without Content-Type header matches normal account route")
    void testBankSmartNormalAccountDispatchWithoutContentTypeHeader() throws Exception {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);

        MockRoute successRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "AV".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();

        // Verify that Content-Type was NOT retained as a header matcher for this GET route
        if (successRoute.getMatchers().containsKey("headers")) {
            Map<?, ?> headers = (Map<?, ?>) successRoute.getMatchers().get("headers");
            assertFalse(headers.containsKey("Content-Type"),
                    "Bodyless GET route must not require Content-Type header matcher");
        }

        RouteRegistry.RouteMatch mSuccess = RouteRegistry.RouteMatch.builder().route(successRoute).pathVariables(Map.of()).build();
        RouteRegistry mockRegistry = mock(RouteRegistry.class);
        when(mockRegistry.findRoutes("GET", "/mbank-verification")).thenReturn(new ArrayList<>(List.of(mSuccess)));

        RequestMatcher realMatcher = new RequestMatcher();
        DynamicRouteDispatcher dispatcher = new DynamicRouteDispatcher(
                mockRegistry, scriptEngine, templateEngine, realMatcher, scenarioService,
                new ObjectMapper(), trafficLogger, environmentService, redisTemplate, asyncWebhookService);

        // Incoming GET request with ONLY ?VM=AV and NO headers
        when(request.getRequestURI()).thenReturn("/mock/mbank-verification");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("VM=AV");
        when(request.getParameter(anyString())).thenAnswer(inv -> "VM".equals(inv.getArgument(0, String.class)) ? "AV" : null);
        when(request.getParameterMap()).thenReturn(Map.of("VM", new String[]{"AV"}));
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getContentLength()).thenReturn(0);

        StringWriter out = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(out));

        boolean handled = dispatcher.preHandle(request, response, new Object());
        assertFalse(handled, "preHandle should handle the mock request and return false");

        ArgumentCaptor<com.dynamicmock.application.service.TrafficLogger.ExecutionEvent> captor =
                ArgumentCaptor.forClass(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.class);
        verify(trafficLogger).log(captor.capture());

        assertEquals(successRoute.getId(), captor.getValue().getResourceId(),
                "Route matching ?VM=AV must succeed without requiring Content-Type header");
        assertTrue(out.toString().contains("<response_code>0</response_code>"),
                "Output response must contain response_code 0 (Success)");
    }

    // =========================================================================
    // Gate 6: XML Dynamic Echo Execution & 10-Digit Epoch Seconds Timestamp
    // =========================================================================

    @Test
    @DisplayName("08 & 12. XML Dynamic Echo echoes ACC, UID, and renders 10-digit epoch seconds timestamp")
    void testXmlDynamicEchoExecution() {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);

        MockRoute successRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "AV".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();

        // Verify template translation occurred: {{$timestamp}} was rewritten to {{$timestampSeconds}}
        assertTrue(successRoute.getResponseTemplate().contains("{{$timestampSeconds}}"),
                "Postman {{$timestamp}} must be translated to {{$timestampSeconds}} during import");
        assertFalse(successRoute.getResponseTemplate().matches("(?s).*\\{\\{\\s*\\$timestamp\\s*\\}\\}.*"),
                "No raw {{$timestamp}} should remain in the imported template");

        // Render template with realistic context
        Map<String, Object> ctx = Map.of(
                "request", Map.of(
                        "queryParams", Map.of(
                                "ACC", "9801879262",
                                "UID", "VkZTQVE=",
                                "NAME", "RAM SHRESTHA",
                                "MOBILE", "9800000000"
                        )
                )
        );

        String rendered = templateEngine.render(successRoute.getResponseTemplate(), ctx);
        assertTrue(rendered.contains("<response_code>0</response_code>"));
        assertTrue(rendered.contains("<account_number>9801879262</account_number>"));
        assertTrue(rendered.contains("<unique_id>VkZTQVE=</unique_id>"));
        assertTrue(rendered.contains("<name>RAM SHRESTHA</name>"));
        assertTrue(rendered.contains("<mobile_number>9800000000</mobile_number>"));

        // Extract <requested_date>...</requested_date> and verify it is a 10-digit timestamp
        String timestamp = rendered.replaceAll("(?s).*<requested_date>([^<]+)</requested_date>.*", "$1").trim();
        assertEquals(10, timestamp.length(), "Imported Postman timestamp must render 10-digit epoch seconds: " + timestamp);
        assertDoesNotThrow(() -> Long.parseLong(timestamp));
    }

    // =========================================================================
    // Postman Routing Header Overrides
    // =========================================================================

    @Test
    @DisplayName("14 & 15. x-mock-response-name overrides matchers and takes precedence over response-code")
    void testRoutingHeaderOverrides() throws Exception {
        importService.importPostmanCollection(bankSmartFixtureBytes, null);

        MockRoute portalRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "AC".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("VM")))
                .findFirst().orElseThrow();
        MockRoute dormantRoute = routeDatabase.values().stream()
                .filter(r -> r.getMatchers() != null && "0000000000".equals(((Map<?, ?>) r.getMatchers().get("queryParams")).get("ACC")))
                .findFirst().orElseThrow();

        RouteRegistry.RouteMatch mPortal = RouteRegistry.RouteMatch.builder().route(portalRoute).pathVariables(Map.of()).build();
        RouteRegistry.RouteMatch mDormant = RouteRegistry.RouteMatch.builder().route(dormantRoute).pathVariables(Map.of()).build();

        RouteRegistry mockRegistry = mock(RouteRegistry.class);
        when(mockRegistry.findRoutes("GET", "/mbank-verification")).thenReturn(new ArrayList<>(List.of(mDormant, mPortal)));

        RequestMatcher realMatcher = new RequestMatcher();
        DynamicRouteDispatcher dispatcher = new DynamicRouteDispatcher(
                mockRegistry, scriptEngine, templateEngine, realMatcher, scenarioService,
                new ObjectMapper(), trafficLogger, environmentService, redisTemplate, asyncWebhookService);

        // Client supplies conflicting headers: x-mock-response-name specifies portal, while x-mock-response-code specifies 404
        when(request.getRequestURI()).thenReturn("/mock/mbank-verification");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("x-mock-response-name")).thenReturn("BankSmart Portal Success Page");
        when(request.getHeader("x-mock-response-code")).thenReturn("404");
        when(request.getParameterMap()).thenReturn(Collections.emptyMap());
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getContentLength()).thenReturn(0);

        StringWriter out = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(out));

        dispatcher.preHandle(request, response, new Object());

        ArgumentCaptor<com.dynamicmock.application.service.TrafficLogger.ExecutionEvent> captor =
                ArgumentCaptor.forClass(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.class);
        verify(trafficLogger).log(captor.capture());

        assertEquals(portalRoute.getId(), captor.getValue().getResourceId(),
                "x-mock-response-name must override query parameters and take precedence over x-mock-response-code");
        assertTrue(out.toString().contains("<!DOCTYPE html>"), "Selected portal route must return HTML");
    }
}

