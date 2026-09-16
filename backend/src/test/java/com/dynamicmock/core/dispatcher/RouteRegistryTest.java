package com.dynamicmock.core.dispatcher;

import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RouteRegistryTest {
    
    private RouteRegistry routeRegistry;
    
    @BeforeEach
    void setUp() {
        routeRegistry = new RouteRegistry();
    }
    
    @Test
    void testRegisterExactRoute() {
        MockRoute route = createMockRoute("GET", "/users", "1");
        routeRegistry.register(route);
        
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes("GET", "/users");
        assertFalse(matches.isEmpty());
        RouteRegistry.RouteMatch match = matches.get(0);
        assertEquals(route, match.getRoute());
        assertTrue(match.getPathVariables().isEmpty());
    }
    
    @Test
    void testRegisterPatternRoute() {
        MockRoute route = createMockRoute("GET", "/users/{id}", "1");
        routeRegistry.register(route);
        
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes("GET", "/users/123");
        assertFalse(matches.isEmpty());
        RouteRegistry.RouteMatch match = matches.get(0);
        assertEquals(route, match.getRoute());
        assertEquals("123", match.getPathVariables().get("id"));
    }
    
    @Test
    void testRegisterMultiplePathVariables() {
        MockRoute route = createMockRoute("GET", "/users/{userId}/posts/{postId}", "1");
        routeRegistry.register(route);
        
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes("GET", "/users/123/posts/456");
        assertFalse(matches.isEmpty());
        RouteRegistry.RouteMatch match = matches.get(0);
        assertEquals("123", match.getPathVariables().get("userId"));
        assertEquals("456", match.getPathVariables().get("postId"));
    }
    
    @Test
    void testFindRouteNotFound() {
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes("GET", "/nonexistent");
        assertTrue(matches.isEmpty());
    }
    
    @Test
    void testFindRouteWrongMethod() {
        MockRoute route = createMockRoute("GET", "/users", "1");
        routeRegistry.register(route);
        
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes("POST", "/users");
        assertTrue(matches.isEmpty());
    }
    
    @Test
    void testUnregisterRoute() {
        MockRoute route = createMockRoute("GET", "/users", "1");
        routeRegistry.register(route);
        
        assertFalse(routeRegistry.findRoutes("GET", "/users").isEmpty());
        
        routeRegistry.unregister(route);
        
        assertTrue(routeRegistry.findRoutes("GET", "/users").isEmpty());
    }
    
    @Test
    void testUnregisterPatternRoute() {
        MockRoute route = createMockRoute("GET", "/users/{id}", "1");
        routeRegistry.register(route);
        
        routeRegistry.unregister(route);
        
        assertTrue(routeRegistry.findRoutes("GET", "/users/123").isEmpty());
    }
    
    @Test
    void testGetAllRoutes() {
        MockRoute route1 = createMockRoute("GET", "/users", "1");
        MockRoute route2 = createMockRoute("POST", "/users", "2");
        
        routeRegistry.register(route1);
        routeRegistry.register(route2);
        
        assertEquals(2, routeRegistry.getAllRoutes().size());
    }
    
    @Test
    void testClear() {
        MockRoute route = createMockRoute("GET", "/users", "1");
        routeRegistry.register(route);
        
        routeRegistry.clear();
        
        assertTrue(routeRegistry.findRoutes("GET", "/users").isEmpty());
        assertTrue(routeRegistry.getAllRoutes().isEmpty());
    }
    
    @Test
    void testLoadRoutes() {
        MockRoute route1 = createMockRoute("GET", "/users", "1");
        route1.setActive(true);
        MockRoute route2 = createMockRoute("POST", "/posts", "2");
        route2.setActive(true);
        MockRoute route3 = createMockRoute("GET", "/inactive", "3");
        route3.setActive(false);
        
        routeRegistry.loadRoutes(java.util.Arrays.asList(route1, route2, route3));
        
        assertFalse(routeRegistry.findRoutes("GET", "/users").isEmpty());
        assertFalse(routeRegistry.findRoutes("POST", "/posts").isEmpty());
        assertTrue(routeRegistry.findRoutes("GET", "/inactive").isEmpty());
    }
    
    @Test
    void testNormalizePath() {
        MockRoute route1 = createMockRoute("GET", "users", "1"); // No leading slash
        MockRoute route2 = createMockRoute("GET", "/users/", "2"); // Trailing slash
        
        routeRegistry.register(route1);
        routeRegistry.register(route2);
        
        assertFalse(routeRegistry.findRoutes("GET", "/users").isEmpty());
    }
    
    @Test
    void testCaseInsensitiveMethod() {
        MockRoute route = createMockRoute("get", "/users", "1"); // lowercase
        routeRegistry.register(route);
        
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes("GET", "/users");
        assertFalse(matches.isEmpty());
        RouteRegistry.RouteMatch match = matches.get(0);
    }
    
    @Test
    void testRegisterThrowsExceptionForNullRoute() {
        assertThrows(IllegalArgumentException.class, () -> {
            routeRegistry.register(null);
        });
    }
    
    @Test
    void testRegisterThrowsExceptionForNullPath() {
        MockRoute route = createMockRoute("GET", null, "1");
        assertThrows(IllegalArgumentException.class, () -> {
            routeRegistry.register(route);
        });
    }
    
    @Test
    void testRegisterThrowsExceptionForNullMethod() {
        MockRoute route = createMockRoute(null, "/users", "1");
        assertThrows(IllegalArgumentException.class, () -> {
            routeRegistry.register(route);
        });
    }
    
    private MockRoute createMockRoute(String method, String path, String id) {
        return MockRoute.builder()
            .id(id)
            .method(method)
            .path(path)
            .responseStatus(200)
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}

