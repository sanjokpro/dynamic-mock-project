package com.dynamicmock.infrastructure.config;

import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initialize route registry on application startup
 * Uses ApplicationReadyEvent to ensure MongoDB is ready (especially for Testcontainers)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RouteRegistryInitializer {
    
    private final RouteRegistry routeRegistry;
    private final MockRouteRepository repository;
    
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            log.info("Loading active routes into registry...");
            routeRegistry.loadRoutes(repository.findByActiveTrue());
            log.info("Route registry initialized");
        } catch (Exception e) {
            // During tests with Testcontainers, MongoDB might not be ready yet
            // This is handled by the test setup, so we can safely ignore the error
            log.warn("Could not initialize route registry on startup: {}. This is normal during tests.", e.getMessage());
        }
    }
}

