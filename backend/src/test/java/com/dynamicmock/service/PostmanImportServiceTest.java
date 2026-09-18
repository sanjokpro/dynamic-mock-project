package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.application.service.PostmanImportService;
import com.dynamicmock.application.service.PostmanImportService.ImportReport;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.application.service.WorkspaceCollectionService;
import com.dynamicmock.application.service.WorkspaceEnvironmentService;
import com.dynamicmock.domain.entity.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for the Postman Collection v2.1 importer.
 * Uses real fixture JSON files from src/test/resources/postman/.
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
        // Explicit construction (not @InjectMocks): the service's ObjectMapper is a
        // real collaborator, and Mockito would otherwise inject null for it.
        importService = new PostmanImportService(
                routeService, collectionService, environmentService, new ObjectMapper());

        collectionJson = fixtureBytes("postman/sample-collection.json");
        environmentJson = fixtureBytes("postman/sample-environment.json");

        when(routeService.createRoute(any())).thenAnswer(invocation -> {
            CreateRouteRequest req = invocation.getArgument(0, CreateRouteRequest.class);
            return RouteResponse.builder()
                    .id("route-" + System.nanoTime())
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

        // 4 requests in fixture: 2 in Users folder + 1 legacy + 1 top-level Health
        assertEquals(4, report.getRoutes());
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
    void importCollection_shouldConvertPathVariableSyntax() {
        importService.importPostmanCollection(collectionJson, null);

        assertTrue(capturedRequests().stream()
                        .anyMatch(r -> "/users/{id}?verbose=true".equals(r.getPath())),
                "Expected :id converted to {id}; got: "
                        + capturedRequests().stream().map(CreateRouteRequest::getPath).toList());
    }

    @Test
    void importCollection_shouldConvertHeaderAndQueryMatchers() {
        importService.importPostmanCollection(collectionJson, null);

        CreateRouteRequest userRoute = capturedRequests().stream()
                .filter(r -> r.getPath() != null && r.getPath().startsWith("/users/{id}"))
                .findFirst().orElseThrow();

        assertNotNull(userRoute.getMatchers());
        assertEquals("v2", ((java.util.Map<?, ?>) userRoute.getMatchers().get("headers")).get("X-Api-Version"));
        assertEquals("true", ((java.util.Map<?, ?>) userRoute.getMatchers().get("queryParams")).get("verbose"));
    }

    @Test
    void importCollection_shouldUseExampleResponseAsTemplate() {
        importService.importPostmanCollection(collectionJson, null);

        CreateRouteRequest userRoute = capturedRequests().stream()
                .filter(r -> r.getPath() != null && r.getPath().startsWith("/users/{id}"))
                .findFirst().orElseThrow();

        assertTrue(userRoute.getResponseTemplate().contains("{{$guid}}"));
        assertEquals(200, userRoute.getResponseStatus());
        // example header becomes response header
        assertEquals("application/json", userRoute.getResponseHeaders().get("Content-Type"));

        // Create User carries its example at ITEM level (v2.1 standard) - also resolved
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
        // every created route is immediately activated (registered in RouteRegistry)
        verify(routeService, times(4)).activateRoute(anyString());
    }

    @Test
    void importCollection_shouldContinueAfterPerRequestFailure() {
        // doReturn/doThrow style: re-stubbing with when(mock.method(any())) would
        // re-invoke the existing thenAnswer stub with a null argument and NPE
        doReturn(RouteResponse.builder().id("r1").build())
                .doThrow(new RuntimeException("boom"))
                .doReturn(RouteResponse.builder().id("r3").build())
                .doReturn(RouteResponse.builder().id("r4").build())
                .when(routeService).createRoute(any());

        ImportReport report = importService.importPostmanCollection(collectionJson, null);

        assertEquals(3, report.getRoutes());
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
        assertEquals(4, report.getRoutes());
    }

    @Test
    void importCollection_shouldRegisterItemsInCollections() {
        importService.importPostmanCollection(collectionJson, null);

        // Folder children get their plain item name; top-level bare requests keep theirs
        verify(collectionService, atLeastOnce()).addItemToCollection(anyString(), argThat(item ->
                "Get User By Id".equals(item.getName())));
        verify(collectionService, atLeastOnce()).addItemToCollection(anyString(), argThat(item ->
                "Health".equals(item.getName())));
    }
}
