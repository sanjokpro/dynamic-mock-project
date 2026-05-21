package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.RouteController;
import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.in.web.dto.UpdateRouteRequest;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.application.service.VersionService;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.entity.RouteVersion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    @Mock
    private RouteService routeService;

    @Mock
    private VersionService versionService;

    @InjectMocks
    private RouteController routeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(routeController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllRoutes_shouldReturnRoutes() throws Exception {
        // Given
        List<RouteResponse> routes = Arrays.asList(
            createRouteResponse("1", "/api/users"),
            createRouteResponse("2", "/api/posts")
        );
        when(routeService.listRoutes()).thenReturn(routes);

        // When & Then
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));

        verify(routeService).listRoutes();
    }

    @Test
    void getRoute_shouldReturnRoute() throws Exception {
        // Given
        RouteResponse route = createRouteResponse("1", "/api/users");
        when(routeService.getRoute("1")).thenReturn(route);

        // When & Then
        mockMvc.perform(get("/api/routes/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.path").value("/api/users"));

        verify(routeService).getRoute("1");
    }

    @Test
    void createRoute_shouldCreateAndReturnRoute() throws Exception {
        // Given
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/api/users");
        request.setMethod("GET");
        request.setResponseTemplate("{\"message\": \"success\"}");

        RouteResponse createdRoute = createRouteResponse("1", "/api/users");
        when(routeService.createRoute(any(CreateRouteRequest.class))).thenReturn(createdRoute);

        // When & Then
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.path").value("/api/users"));

        verify(routeService).createRoute(any(CreateRouteRequest.class));
    }

    @Test
    void updateRoute_shouldUpdateAndReturnRoute() throws Exception {
        // Given
        UpdateRouteRequest request = new UpdateRouteRequest();
        request.setPath("/api/users");
        request.setMethod("POST");

        RouteResponse updatedRoute = createRouteResponse("1", "/api/users");
        when(routeService.updateRoute(eq("1"), any(UpdateRouteRequest.class))).thenReturn(updatedRoute);

        // When & Then
        mockMvc.perform(put("/api/routes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"));

        verify(routeService).updateRoute(eq("1"), any(UpdateRouteRequest.class));
    }

    @Test
    void deleteRoute_shouldDeleteRoute() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/routes/1"))
                .andExpect(status().isNoContent());

        verify(routeService).deleteRoute("1");
    }

    @Test
    void activateRoute_shouldActivateRoute() throws Exception {
        // Given
        RouteResponse activatedRoute = createRouteResponse("1", "/api/users");
        activatedRoute.setActive(true);
        when(routeService.activateRoute("1")).thenReturn(activatedRoute);

        // When & Then
        mockMvc.perform(post("/api/routes/1/activate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(true));

        verify(routeService).activateRoute("1");
    }

    @Test
    void deactivateRoute_shouldDeactivateRoute() throws Exception {
        // Given
        RouteResponse deactivatedRoute = createRouteResponse("1", "/api/users");
        deactivatedRoute.setActive(false);
        when(routeService.deactivateRoute("1")).thenReturn(deactivatedRoute);

        // When & Then
        mockMvc.perform(post("/api/routes/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(false));

        verify(routeService).deactivateRoute("1");
    }

    @Test
    void getRouteHistory_shouldReturnHistory() throws Exception {
        // Given
        List<RouteVersion> history = Arrays.asList(
            RouteVersion.builder().id("v1").routeId("1").versionNumber(1).build(),
            RouteVersion.builder().id("v2").routeId("1").versionNumber(2).build()
        );
        when(versionService.getVersionHistory("1")).thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/routes/1/versions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));

        verify(versionService).getVersionHistory("1");
    }

    @Test
    void rollbackRoute_shouldRollbackAndReturnRoute() throws Exception {
        // Given
        RouteResponse rolledBackRoute = createRouteResponse("1", "/api/users");
        when(versionService.rollbackToVersion("1", 1)).thenReturn(MockRoute.builder().id("1").build());
        when(routeService.getRoute("1")).thenReturn(rolledBackRoute);

        // When & Then
        mockMvc.perform(post("/api/routes/1/versions/1/rollback")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"));

        verify(versionService).rollbackToVersion("1", 1);
    }

    @Test
    void compareVersions_shouldReturnDiff() throws Exception {
        // Given
        VersionService.VersionDiff diff = VersionService.VersionDiff.builder()
                .routeId("1")
                .version1(1)
                .version2(2)
                .totalChanges(1)
                .build();
        when(versionService.diffVersions("1", 1, 2)).thenReturn(diff);

        // When & Then
        mockMvc.perform(get("/api/routes/1/versions/diff")
                        .param("v1", "1")
                        .param("v2", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.version1").value(1))
                .andExpect(jsonPath("$.version2").value(2));

        verify(versionService).diffVersions("1", 1, 2);
    }

    private RouteResponse createRouteResponse(String id, String path) {
        RouteResponse response = new RouteResponse();
        response.setId(id);
        response.setPath(path);
        response.setMethod("GET");
        response.setResponseTemplate("{\"message\": \"success\"}");
        response.setResponseStatus(200);
        response.setActive(false);
        return response;
    }
}
