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
        
        RouteRegistry.RouteMatch match = routeRegistry.findRoute("GET", "/users");
        assertNotNull(match);
        assertEquals(route, match.getRoute());
        assertTrue(match.getPathVariables().isEmpty());
    }
    
    @Test
    void testRegisterPatternRoute() {
        MockRoute route = createMockRoute("GET", "/users/{id}", "1");
        routeRegistry.register(route);
        
        RouteRegistry.RouteMatch match = routeRegistry.findRoute("GET", "/users/123");
        assertNotNull(match);
        assertEquals(route, match.getRoute());
        assertEquals("123", match.getPathVariables().get("id"));
    }
    
    @Test
    void testRegisterMultiplePathVariables() {
        MockRoute route = createMockRoute("GET", "/users/{userId}/posts/{postId}", "1");
        routeRegistry.register(route);
        
        RouteRegistry.RouteMatch match = routeRegistry.findRoute("GET", "/users/123/posts/456");
        assertNotNull(match);
        assertEquals("123", match.getPathVariables().get("userId"));
        assertEquals("456", match.getPathVariables().get("postId"));
    }
    
    @Test
    void testFindRouteNotFound() {
        RouteRegistry.RouteMatch match = routeRegistry.findRoute("GET", "/nonexistent");
        assertNull(match);
    }
    
    @Test
    void testFindRouteWrongMethod() {
        MockRoute route = createMockRoute("GET", "/users", "1");
        routeRegistry.register(route);
        
        RouteRegistry.RouteMatch match = routeRegistry.findRoute("POST", "/users");
        assertNull(match);
    }
    
    @Test
    void testUnregisterRoute() {
        MockRoute route = createMockRoute("GET", "/users", "1");
        routeRegistry.register(route);
        
        assertNotNull(routeRegistry.findRoute("GET", "/users"));
        
        routeRegistry.unregister("GET", "/users");
        
        assertNull(routeRegistry.findRoute("GET", "/users"));
    }
    
    @Test
    void testUnregisterPatternRoute() {
        MockRoute route = createMockRoute("GET", "/users/{id}", "1");
        routeRegistry.register(route);
        
        routeRegistry.unregister("GET", "/users/{id}");
        
        assertNull(routeRegistry.findRoute("GET", "/users/123"));
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
        
        assertNull(routeRegistry.findRoute("GET", "/users"));
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
        
        assertNotNull(routeRegistry.findRoute("GET", "/users"));
        assertNotNull(routeRegistry.findRoute("POST", "/posts"));
        assertNull(routeRegistry.findRoute("GET", "/inactive"));
    }
    
    @Test
    void testNormalizePath() {
        MockRoute route1 = createMockRoute("GET", "users", "1"); // No leading slash
        MockRoute route2 = createMockRoute("GET", "/users/", "2"); // Trailing slash
        
        routeRegistry.register(route1);
        routeRegistry.register(route2);
        
        assertNotNull(routeRegistry.findRoute("GET", "/users"));
    }
    
    @Test
    void testCaseInsensitiveMethod() {
        MockRoute route = createMockRoute("get", "/users", "1"); // lowercase
        routeRegistry.register(route);
        
        RouteRegistry.RouteMatch match = routeRegistry.findRoute("GET", "/users");
        assertNotNull(match);
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

