package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.in.web.dto.UpdateRouteRequest;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.entity.RouteVersion;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.domain.port.out.RouteVersionRepository;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing mock routes (CRUD operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteService {
    
    private final MockRouteRepository repository;
    private final RouteRegistry routeRegistry;
    private final RouteVersionRepository versionRepository;
    
    /**
     * Create a new mock route
     */
    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        MockRoute route = MockRoute.builder()
            .id(UUID.randomUUID().toString())
            .path(request.getPath())
            .method(request.getMethod().toUpperCase())
            .matchers(request.getMatchers())
            .responseTemplate(request.getResponseTemplate())
            .responseStatus(request.getResponseStatus() != null ? request.getResponseStatus() : 200)
            .responseHeaders(request.getResponseHeaders())
            .preScript(request.getPreScript())
            .postScript(request.getPostScript())
            .scriptLanguage(request.getScriptLanguage() != null ? request.getScriptLanguage() : "js")
            .delayMs(request.getDelayMs() != null ? request.getDelayMs() : 0)
            .version(request.getVersion() != null ? request.getVersion() : 1)
            .scenarioName(request.getScenarioName())
            .active(false) // Routes are inactive by default
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        route = repository.save(route);
        log.info("Created route: {} {} {}", route.getMethod(), route.getPath(), route.getId());
        
        return toResponse(route);
    }
    
    /**
     * Update an existing route
     */
    @Transactional
    public RouteResponse updateRoute(String id, UpdateRouteRequest request) {
        MockRoute route = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        
        // Save current state as a version before updating
        saveRouteVersion(route, "Route updated");
        
        boolean wasActive = Boolean.TRUE.equals(route.getActive());
        
        // Update fields
        if (request.getPath() != null) {
            route.setPath(request.getPath());
        }
        if (request.getMethod() != null) {
            route.setMethod(request.getMethod().toUpperCase());
        }
        if (request.getMatchers() != null) {
            route.setMatchers(request.getMatchers());
        }
        if (request.getResponseTemplate() != null) {
            route.setResponseTemplate(request.getResponseTemplate());
        }
        if (request.getResponseStatus() != null) {
            route.setResponseStatus(request.getResponseStatus());
        }
        if (request.getResponseHeaders() != null) {
            route.setResponseHeaders(request.getResponseHeaders());
        }
        if (request.getPreScript() != null) {
            route.setPreScript(request.getPreScript());
        }
        if (request.getPostScript() != null) {
            route.setPostScript(request.getPostScript());
        }
        if (request.getScriptLanguage() != null) {
            route.setScriptLanguage(request.getScriptLanguage());
        }
        if (request.getDelayMs() != null) {
            route.setDelayMs(request.getDelayMs());
        }
        if (request.getVersion() != null) {
            route.setVersion(request.getVersion());
        }
        if (request.getActive() != null) {
            route.setActive(request.getActive());
        }
        if (request.getScenarioName() != null) {
            route.setScenarioName(request.getScenarioName());
        }
        
        route.setUpdatedAt(LocalDateTime.now());
        route = repository.save(route);
        
        // Update registry if active status changed
        if (wasActive != Boolean.TRUE.equals(route.getActive())) {
            if (route.getActive()) {
                routeRegistry.register(route);
            } else {
                routeRegistry.unregister(route.getMethod(), route.getPath());
            }
        } else if (route.getActive()) {
            // Re-register if active to update the route
            routeRegistry.unregister(route.getMethod(), route.getPath());
            routeRegistry.register(route);
        }
        
        log.info("Updated route: {} {} {}", route.getMethod(), route.getPath(), route.getId());
        return toResponse(route);
    }
    
    /**
     * Get a route by ID
     */
    public RouteResponse getRoute(String id) {
        MockRoute route = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        return toResponse(route);
    }
    
    /**
     * List all routes
     */
    public List<RouteResponse> listRoutes() {
        return repository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Delete a route
     */
    @Transactional
    public void deleteRoute(String id) {
        MockRoute route = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        
        // Remove from registry if active
        if (Boolean.TRUE.equals(route.getActive())) {
            routeRegistry.unregister(route.getMethod(), route.getPath());
        }
        
        // Delete version history
        versionRepository.deleteByRouteId(id);
        
        repository.deleteById(id);
        log.info("Deleted route: {} {} {}", route.getMethod(), route.getPath(), route.getId());
    }
    
    /**
     * Activate a route (publish to registry)
     */
    @Transactional
    public RouteResponse activateRoute(String id) {
        MockRoute route = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        
        route.setActive(true);
        route.setUpdatedAt(LocalDateTime.now());
        route = repository.save(route);
        
        routeRegistry.register(route);
        log.info("Activated route: {} {} {}", route.getMethod(), route.getPath(), route.getId());
        
        return toResponse(route);
    }
    
    /**
     * Deactivate a route (remove from registry)
     */
    @Transactional
    public RouteResponse deactivateRoute(String id) {
        MockRoute route = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + id));
        
        route.setActive(false);
        route.setUpdatedAt(LocalDateTime.now());
        route = repository.save(route);
        
        routeRegistry.unregister(route.getMethod(), route.getPath());
        log.info("Deactivated route: {} {} {}", route.getMethod(), route.getPath(), route.getId());
        
        return toResponse(route);
    }
    
    /**
     * Save a version of the route before changes
     */
    private void saveRouteVersion(MockRoute route, String description) {
        try {
            int nextVersion = versionRepository.findTopByRouteIdOrderByVersionNumberDesc(route.getId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);
            
            RouteVersion version = RouteVersion.fromRoute(route, nextVersion, description);
            version.setId(UUID.randomUUID().toString());
            versionRepository.save(version);
            
            log.debug("Saved version {} for route {}", nextVersion, route.getId());
        } catch (Exception e) {
            log.warn("Failed to save route version: {}", e.getMessage());
            // Don't fail the update if versioning fails
        }
    }
    
    private RouteResponse toResponse(MockRoute route) {
        return RouteResponse.builder()
            .id(route.getId())
            .path(route.getPath())
            .method(route.getMethod())
            .matchers(route.getMatchers())
            .responseTemplate(route.getResponseTemplate())
            .responseStatus(route.getResponseStatus())
            .responseHeaders(route.getResponseHeaders())
            .preScript(route.getPreScript())
            .postScript(route.getPostScript())
            .scriptLanguage(route.getScriptLanguage())
            .delayMs(route.getDelayMs())
            .version(route.getVersion())
            .active(route.getActive())
            .scenarioName(route.getScenarioName())
            .createdAt(route.getCreatedAt())
            .updatedAt(route.getUpdatedAt())
            .build();
    }
}

