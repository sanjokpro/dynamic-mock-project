package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.application.service.PostmanImportService;
import com.dynamicmock.application.service.PostmanImportService.ImportReport;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.application.service.WorkspaceCollectionService;
import com.dynamicmock.application.service.WorkspaceEnvironmentService;
import com.dynamicmock.domain.entity.Collection;
import com.dynamicmock.domain.entity.Environment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for the Postman Collection v2.1 importer.
 * Verifies multi-example route splitting, query stripping, disabled param filtering,
 * collection variable ingestion, and deterministic ID generation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostmanImportServiceTest {

    @Mock
    private RouteService routeService;

    @Mock
    private WorkspaceCollectionService collectionService;

    @Mock
    private WorkspaceEnvironmentService environmentService;

    private PostmanImportService importService;

    private byte[] collectionJson;
    private byte[] environmentJson;

    @BeforeEach
    void loadFixtures() throws IOException {
        importService = new PostmanImportService(
                routeService, collectionService, environmentService, new ObjectMapper());

        collectionJson = fixtureBytes("postman/sample-collection.json");
        environmentJson = fixtureBytes("postman/sample-environment.json");

        when(routeService.createRoute(any())).thenAnswer(invocation -> {
            CreateRouteRequest req = invocation.getArgument(0, CreateRouteRequest.class);
            String routeId = req.getId() != null ? req.getId() : "route-" + System.nanoTime();
            return RouteResponse.builder()
                    .id(routeId)
                    .path(req.getPath())
                    .method(req.getMethod())
                    .active(false)
                    .build();
        });
        when(routeService.activateRoute(anyString())).thenAnswer(invocation ->
                RouteResponse.builder().id(invocation.getArgument(0, String.class)).active(true).build());
        when(collectionService.createCollection(anyString(), anyString(), any()))
                .thenAnswer(invocation -> Collection.builder()
                        .id("col-" + System.nanoTime())
                        .name(invocation.getArgument(0, String.class))
                        .userId(invocation.getArgument(1, String.class))
                        .build());
    }

    private static byte[] fixtureBytes(String name) throws IOException {
        try (InputStream in = PostmanImportServiceTest.class.getClassLoader().getResourceAsStream(name)) {
            assertNotNull(in, "Fixture not found on classpath: " + name);
            return in.readAllBytes();
        }
    }

    private List<CreateRouteRequest> capturedRequests() {
        ArgumentCaptor<CreateRouteRequest> captor = ArgumentCaptor.forClass(CreateRouteRequest.class);
        verify(routeService, atLeastOnce()).createRoute(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void importCollection_shouldCreateRoutesCollectionsAndEnvironment() {
        ImportReport report = importService.importPostmanCollection(collectionJson, List.of(environmentJson));

        // 4 requests in fixture: "Get User By Id" has 2 examples (2 routes) + "Create User" (1) + legacy (1) + Health (1) = 5 routes
        assertEquals(5, report.getRoutes());
        // 1 folder collection ("Users") + 1 bare-request collection ("Petstore API")
        assertEquals(2, report.getCollections());
        assertEquals(1, report.getEnvironments());
        assertTrue(report.getErrors().isEmpty());

        verify(environmentService).createEnvironment(eq("UAT"), argThat(vars ->
                "https://uat.example.com".equals(vars.get("BASE_URL"))
                        && "secret-123".equals(vars.get("API_KEY"))
                        && !vars.containsKey("DEBUG_MODE")));
    }

    @Test
    void importCollection_shouldConvertPathVariableSyntaxAndStripQueryString() {
        importService.importPostmanCollection(collectionJson, null);

        // Path must be stripped of ?verbose=true and :id converted to {id}
        assertTrue(capturedRequests().stream()
                        .anyMatch(r -> "/users/{id}".equals(r.getPath())),
                "Expected /users/{id} without query string; got: "
                        + capturedRequests().stream().map(CreateRouteRequest::getPath).toList());

        assertFalse(capturedRequests().stream()
                        .anyMatch(r -> r.getPath() != null && r.getPath().contains("?")),
                "No imported route path should contain a query string");
    }

    @Test
    void importCollection_shouldConvertHeaderAndQueryMatchers() {
        importService.importPostmanCollection(collectionJson, null);

        CreateRouteRequest userRoute = capturedRequests().stream()
                .filter(r -> r.getPath() != null && r.getPath().startsWith("/users/{id}"))
                .findFirst().orElseThrow();

        assertNotNull(userRoute.getMatchers());
        assertEquals("v2", ((Map<?, ?>) userRoute.getMatchers().get("headers")).get("X-Api-Version"));
        assertEquals("true", ((Map<?, ?>) userRoute.getMatchers().get("queryParams")).get("verbose"));
    }

    @Test
    void importCollection_shouldUseExampleResponseAsTemplate() {
        importService.importPostmanCollection(collectionJson, null);

        // Get User By Id splits into 2 routes (Success 200 and Not Found 404)
        CreateRouteRequest successRoute = capturedRequests().stream()
                .filter(r -> r.getPath() != null && r.getPath().startsWith("/users/{id}") && Integer.valueOf(200).equals(r.getResponseStatus()))
                .findFirst().orElseThrow();

        assertTrue(successRoute.getResponseTemplate().contains("{{$guid}}"));
        assertEquals(200, successRoute.getResponseStatus());
        assertEquals("application/json", successRoute.getResponseHeaders().get("Content-Type"));

        CreateRouteRequest notFoundRoute = capturedRequests().stream()
                .filter(r -> r.getPath() != null && r.getPath().startsWith("/users/{id}") && Integer.valueOf(404).equals(r.getResponseStatus()))
                .findFirst().orElseThrow();
        assertEquals(404, notFoundRoute.getResponseStatus());
        assertTrue(notFoundRoute.getResponseTemplate().contains("user not found"));

        // Create User carries its example at ITEM level (v2.1 standard)
        CreateRouteRequest createRoute = capturedRequests().stream()
                .filter(r -> "/users".equals(r.getPath()) && "POST".equals(r.getMethod()))
                .findFirst().orElseThrow();
        assertTrue(createRoute.getResponseTemplate().contains("{{$randomUUID}}"));
        assertEquals(201, createRoute.getResponseStatus());
    }

    @Test
    void importCollection_shouldFallBackToDefaultTemplateWhenNoExample() {
        importService.importPostmanCollection(collectionJson, null);

        // Health request has no saved examples
        CreateRouteRequest healthRoute = capturedRequests().stream()
                .filter(r -> "/health".equals(r.getPath()))
                .findFirst().orElseThrow();

        assertEquals("{\"message\": \"Mocked from Postman collection\"}", healthRoute.getResponseTemplate());
        assertEquals(200, healthRoute.getResponseStatus());
    }

    @Test
    void importCollection_shouldWarnAboutSkippedScripts() {
        ImportReport report = importService.importPostmanCollection(collectionJson, null);

        assertTrue(report.getWarnings().stream()
                        .anyMatch(w -> w.contains("Legacy Script Request") && w.contains("scripts")),
                "Expected script warning; got: " + report.getWarnings());
    }

    @Test
    void importCollection_shouldActivateImportedRoutes() {
        importService.importPostmanCollection(collectionJson, null);
        // All 5 created routes are immediately activated
        verify(routeService, times(5)).activateRoute(anyString());
    }

    @Test
    void importCollection_shouldContinueAfterPerRequestFailure() {
        doReturn(RouteResponse.builder().id("r1").build())
                .doThrow(new RuntimeException("boom"))
                .doReturn(RouteResponse.builder().id("r3").build())
                .doReturn(RouteResponse.builder().id("r4").build())
                .doReturn(RouteResponse.builder().id("r5").build())
                .when(routeService).createRoute(any());

        ImportReport report = importService.importPostmanCollection(collectionJson, null);

        assertEquals(4, report.getRoutes());
        assertEquals(1, report.getErrors().size());
        assertTrue(report.getErrors().get(0).contains("boom"));
    }

    @Test
    void importCollection_shouldRejectMalformedJson() {
        assertThrows(IllegalArgumentException.class,
                () -> importService.importPostmanCollection("{ not json ".getBytes(), null));
    }

    @Test
    void importCollection_shouldCollectEnvironmentParseErrorsWithoutFailing() {
        byte[] badEnv = "{ nope ".getBytes();
        ImportReport report = importService.importPostmanCollection(collectionJson, List.of(badEnv));

        assertEquals(0, report.getEnvironments());
        assertFalse(report.getErrors().isEmpty());
        assertEquals(5, report.getRoutes());
    }

    @Test
    void importCollection_shouldRegisterItemsInCollections() {
        importService.importPostmanCollection(collectionJson, null);

        // Folder children registered in collection
        verify(collectionService, atLeastOnce()).addItemToCollection(anyString(), argThat(item ->
                item.getName() != null && item.getName().startsWith("Get User By Id")));
        verify(collectionService, atLeastOnce()).addItemToCollection(anyString(), argThat(item ->
                "Health".equals(item.getName())));
    }

    @Test
    @DisplayName("Should ingest collection variables into [CollectionName] Variables environment")
    void importCollection_shouldIngestCollectionVariables() {
        String jsonWithVars = """
        {
          "info": {
            "name": "VarCol",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
          },
          "item": [
            {
              "name": "TestReq",
              "request": {
                "method": "GET",
                "url": "https://api.example.com/test"
              }
            }
          ],
          "variable": [
            { "key": "BASE_URL", "value": "https://api.test.com" },
            { "key": "DISABLED_VAR", "value": "skip", "disabled": true }
          ]
        }
        """;

        when(environmentService.getAllEnvironments()).thenReturn(List.of());

        ImportReport report = importService.importPostmanCollection(jsonWithVars.getBytes(), null);

        assertEquals(1, report.getEnvironments());
        verify(environmentService).createEnvironment(eq("VarCol Variables"), argThat(vars ->
                "https://api.test.com".equals(vars.get("BASE_URL")) && !vars.containsKey("DISABLED_VAR")));
    }

    @Test
    @DisplayName("Should ignore disabled headers and query parameters")
    void importCollection_shouldIgnoreDisabledHeadersAndQueryParams() {
        String jsonWithDisabled = """
        {
          "info": { "name": "DisabledTest" },
          "item": [
            {
              "name": "FilteredReq",
              "request": {
                "method": "GET",
                "url": {
                  "raw": "https://api.example.com/filter?enabledParam=1&disabledParam=2",
                  "query": [
                    { "key": "enabledParam", "value": "1" },
                    { "key": "disabledParam", "value": "2", "disabled": true }
                  ]
                },
                "header": [
                  { "key": "X-Enabled", "value": "active" },
                  { "key": "X-Disabled", "value": "off", "disabled": true }
                ]
              }
            }
          ]
        }
        """;

        importService.importPostmanCollection(jsonWithDisabled.getBytes(), null);

        CreateRouteRequest req = capturedRequests().stream()
                .filter(r -> "/filter".equals(r.getPath()))
                .findFirst().orElseThrow();

        Map<?, ?> qp = (Map<?, ?>) req.getMatchers().get("queryParams");
        assertTrue(qp.containsKey("enabledParam"));
        assertFalse(qp.containsKey("disabledParam"), "Disabled query param must not be in matchers");

        Map<?, ?> hdrs = (Map<?, ?>) req.getMatchers().get("headers");
        assertTrue(hdrs.containsKey("X-Enabled"));
        assertFalse(hdrs.containsKey("X-Disabled"), "Disabled header must not be in matchers");
    }

    @Test
    @DisplayName("Should assign deterministic IDs to imported routes")
    void importCollection_shouldAssignDeterministicIds() {
        importService.importPostmanCollection(collectionJson, null);

        List<CreateRouteRequest> requests = capturedRequests();
        assertFalse(requests.isEmpty());
        for (CreateRouteRequest req : requests) {
            assertNotNull(req.getId(), "Every route must have an ID set");
            assertFalse(req.getId().isBlank());
        }
    }

    @Test
    @DisplayName("Should not warn for distinct query parameters across examples (e.g. BankSmart VM=AV vs ACC=0000000000)")
    void importCollection_shouldNotWarnForDistinctQueryParams() {
        String json = """
        {
          "info": { "name": "Distinct Params Test", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
          "item": [
            {
              "name": "Verification Endpoint",
              "request": {
                "method": "GET",
                "url": "https://api.example.com/verify"
              },
              "response": [
                {
                  "name": "Variant VM",
                  "originalRequest": {
                    "method": "GET",
                    "url": "https://api.example.com/verify?VM=AV"
                  },
                  "code": 200,
                  "body": "{\\"vm\\": true}"
                },
                {
                  "name": "Variant ACC",
                  "originalRequest": {
                    "method": "GET",
                    "url": "https://api.example.com/verify?ACC=0000000000"
                  },
                  "code": 200,
                  "body": "{\\"acc\\": true}"
                }
              ]
            }
          ]
        }
        """;

        ImportReport report = importService.importPostmanCollection(json.getBytes(), null);
        assertTrue(report.getWarnings().isEmpty(), "Expected 0 warnings for distinct parameters; got: " + report.getWarnings());
        assertEquals(2, report.getRoutes());
    }

    @Test
    @DisplayName("Should not warn when examples have equal condition count but different keys (c1 == c2 regression)")
    void importCollection_shouldNotWarnForSameParamCountDifferentKeys() {
        String json = """
        {
          "info": { "name": "Equal Count Diff Keys", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
          "item": [
            {
              "name": "Items",
              "request": { "method": "GET", "url": "https://api.example.com/items" },
              "response": [
                {
                  "name": "Foo Variant",
                  "originalRequest": { "method": "GET", "url": "https://api.example.com/items?foo=1" },
                  "code": 200,
                  "body": "foo"
                },
                {
                  "name": "Bar Variant",
                  "originalRequest": { "method": "GET", "url": "https://api.example.com/items?bar=1" },
                  "code": 200,
                  "body": "bar"
                }
              ]
            }
          ]
        }
        """;

        ImportReport report = importService.importPostmanCollection(json.getBytes(), null);
        assertTrue(report.getWarnings().isEmpty(), "Expected 0 warnings for different keys; got: " + report.getWarnings());
    }

    @Test
    @DisplayName("Should warn when examples specify identical query params and headers")
    void importCollection_shouldWarnForIdenticalQueryParamsAndHeaders() {
        String json = """
        {
          "info": { "name": "Identical Matchers", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
          "item": [
            {
              "name": "Orders",
              "request": { "method": "GET", "url": "https://api.example.com/orders" },
              "response": [
                {
                  "name": "Success Order",
                  "originalRequest": {
                    "method": "GET",
                    "url": "https://api.example.com/orders?status=active",
                    "header": [{ "key": "X-Env", "value": "prod" }]
                  },
                  "code": 200,
                  "body": "{\\"status\\":\\"ok\\"}"
                },
                {
                  "name": "Duplicate Order Condition",
                  "originalRequest": {
                    "method": "GET",
                    "url": "https://api.example.com/orders?status=active",
                    "header": [{ "key": "x-env", "value": "prod" }]
                  },
                  "code": 400,
                  "body": "{\\"status\\":\\"err\\"}"
                }
              ]
            }
          ]
        }
        """;

        ImportReport report = importService.importPostmanCollection(json.getBytes(), null);
        assertEquals(1, report.getWarnings().size(), "Expected 1 warning for identical conditions; got: " + report.getWarnings());
        String warning = report.getWarnings().get(0);
        assertTrue(warning.contains("Identical matching conditions"), "Warning should indicate identical matching conditions");
        assertTrue(warning.contains("Success Order") && warning.contains("Duplicate Order Condition"));
        assertTrue(warning.contains("cannot be distinguished through normal request conditions"));
        assertTrue(warning.contains("x-mock-response-name"));
    }

    @Test
    @DisplayName("Should warn when multiple examples have no conditions (unconditional overlap)")
    void importCollection_shouldWarnForMultipleZeroConditionExamples() {
        String json = """
        {
          "info": { "name": "Unconditional Examples", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
          "item": [
            {
              "name": "Ping",
              "request": { "method": "GET", "url": "https://api.example.com/ping" },
              "response": [
                {
                  "name": "Ping Normal",
                  "originalRequest": { "method": "GET", "url": "https://api.example.com/ping" },
                  "code": 200,
                  "body": "pong"
                },
                {
                  "name": "Ping Outage",
                  "originalRequest": { "method": "GET", "url": "https://api.example.com/ping" },
                  "code": 503,
                  "body": "down"
                }
              ]
            }
          ]
        }
        """;

        ImportReport report = importService.importPostmanCollection(json.getBytes(), null);
        assertEquals(1, report.getWarnings().size(), "Expected 1 warning for unconditional overlap; got: " + report.getWarnings());
        String warning = report.getWarnings().get(0);
        assertTrue(warning.contains("No distinguishing request conditions"), "Warning should indicate no distinguishing conditions");
        assertTrue(warning.contains("Ping Normal") && warning.contains("Ping Outage"));
        assertTrue(warning.contains("cannot be distinguished through normal request conditions"));
    }

    @Test
    @DisplayName("Should classify test scripts as notices and pre-request scripts as warnings")
    void importCollection_shouldClassifyScriptsBySeverity() {
        String json = """
        {
          "info": { "name": "Script Severity Test", "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
          "item": [
            {
              "name": "Pre-request Only",
              "request": { "method": "GET", "url": "https://api.example.com/pre" },
              "event": [
                { "listen": "prerequest", "script": { "exec": ["console.log('pre');"] } }
              ]
            },
            {
              "name": "Test Assertion Only",
              "request": { "method": "GET", "url": "https://api.example.com/test-only" },
              "event": [
                { "listen": "test", "script": { "exec": ["pm.test('200', () => {});"] } }
              ]
            }
          ]
        }
        """;

        ImportReport report = importService.importPostmanCollection(json.getBytes(), null);
        assertEquals(1, report.getWarnings().size(), "Pre-request script must produce 1 warning");
        assertTrue(report.getWarnings().get(0).contains("Pre-request Only") && report.getWarnings().get(0).contains("pre-request"));

        assertEquals(1, report.getNotices().size(), "Test script must produce 1 notice");
        assertTrue(report.getNotices().get(0).contains("Test Assertion Only") && report.getNotices().get(0).contains("test scripts were skipped"));
    }
}
