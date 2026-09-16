package com.dynamicmock.infrastructure.filter;

import com.dynamicmock.domain.entity.MockRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory registry for active mock routes
 * Supports pattern matching for path variables
 */
@Slf4j
@Component
public class RouteRegistry {
    
    // Map: method -> path -> List<MockRoute>
    private final Map<String, Map<String, List<MockRoute>>> routes = new ConcurrentHashMap<>();
    
    // Map: method -> compiled path patterns -> MockRoute
    private final Map<String, List<PatternRoute>> patternRoutes = new ConcurrentHashMap<>();
    
    /**
     * Register a route in the registry
     */
    public void register(MockRoute route) {
        if (route == null || route.getPath() == null || route.getMethod() == null) {
            throw new IllegalArgumentException("Route must have path and method");
        }
        
        String method = route.getMethod().toUpperCase();
        String path = normalizePath(route.getPath());
        
        // Check if path contains variables (e.g., /users/{id})
        if (path.contains("{") && path.contains("}")) {
            registerPatternRoute(method, path, route);
        } else {
            registerExactRoute(method, path, route);
        }
        
        log.info("Registered route: {} {}", method, path);
    }
    
    /**
     * Unregister a route from the registry
     */
    public void unregister(MockRoute route) {
        if (route == null || route.getMethod() == null || route.getPath() == null) {
            return;
        }
        
        String normalizedMethod = route.getMethod().toUpperCase();
        String normalizedPath = normalizePath(route.getPath());
        
        routes.computeIfPresent(normalizedMethod, (m, pathMap) -> {
            List<MockRoute> routeList = pathMap.get(normalizedPath);
            if (routeList != null) {
                routeList.removeIf(r -> r.getId().equals(route.getId()));
                if (routeList.isEmpty()) {
                    pathMap.remove(normalizedPath);
                }
            }
            return pathMap.isEmpty() ? null : pathMap;
        });
        
        // Remove from pattern routes
        patternRoutes.computeIfPresent(normalizedMethod, (m, patternList) -> {
            patternList.removeIf(pr -> pr.route.getId().equals(route.getId()));
            return patternList.isEmpty() ? null : patternList;
        });
        
        log.info("Unregistered route: {} {} ({})", normalizedMethod, normalizedPath, route.getId());
    }
    
    /**
     * Find routes matching the given method and path
     * Returns a list of matching routes and extracted path variables
     */
    public List<RouteMatch> findRoutes(String method, String path) {
        if (method == null || path == null) {
            return List.of();
        }
        
        String normalizedMethod = method.toUpperCase();
        String normalizedPath = normalizePath(path);
        List<RouteMatch> matches = new ArrayList<>();
        
        // Try exact match first
        Map<String, List<MockRoute>> methodRoutes = routes.get(normalizedMethod);
        if (methodRoutes != null) {
            List<MockRoute> exactRoutes = methodRoutes.get(normalizedPath);
            if (exactRoutes != null) {
                exactRoutes.forEach(route -> matches.add(RouteMatch.builder()
                    .route(route)
                    .pathVariables(Map.of())
                    .build()));
            }
        }
        
        // Try pattern matching
        List<PatternRoute> patterns = patternRoutes.get(normalizedMethod);
        if (patterns != null) {
            for (PatternRoute patternRoute : patterns) {
                java.util.regex.Matcher matcher = patternRoute.pattern.matcher(normalizedPath);
                if (matcher.matches()) {
                    Map<String, String> pathVars = extractPathVariables(patternRoute.path, normalizedPath);
                    matches.add(RouteMatch.builder()
                        .route(patternRoute.route)
                        .pathVariables(pathVars)
                        .build());
                }
            }
        }
        
        return matches;
    }
    
    public List<MockRoute> getAllRoutes() {
        List<MockRoute> allRoutes = new ArrayList<>();
        routes.values().stream()
            .flatMap(pathMap -> pathMap.values().stream())
            .flatMap(List::stream)
            .forEach(allRoutes::add);
            
        patternRoutes.values().stream()
            .flatMap(List::stream)
            .map(pr -> pr.route)
            .forEach(allRoutes::add);
            
        return allRoutes.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * Clear all routes
     */
    public void clear() {
        routes.clear();
        patternRoutes.clear();
        log.info("Cleared all routes from registry");
    }
    
    /**
     * Load routes from database (called on startup)
     */
    public void loadRoutes(List<MockRoute> routes) {
        clear();
        routes.stream()
            .filter(MockRoute::getActive)
            .forEach(this::register);
        log.info("Loaded {} active routes into registry", routes.size());
    }
    
    private void registerExactRoute(String method, String path, MockRoute route) {
        routes.computeIfAbsent(method, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(path, k -> new ArrayList<>())
            .add(route);
    }
    
    private void registerPatternRoute(String method, String path, MockRoute route) {
        // Convert path pattern to regex (e.g., /users/{id} -> /users/([^/]+))
        String regex = path.replaceAll("\\{([^}]+)\\}", "([^/]+)");
        Pattern pattern = Pattern.compile("^" + regex + "$");
        
        patternRoutes.computeIfAbsent(method, k -> new ArrayList<>())
            .add(new PatternRoute(path, pattern, route));
    }
    
    private Map<String, String> extractPathVariables(String pattern, String actualPath) {
        Map<String, String> vars = new HashMap<>();
        
        // Extract variable names from pattern
        List<String> varNames = new ArrayList<>();
        Pattern varPattern = Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher matcher = varPattern.matcher(pattern);
        while (matcher.find()) {
            varNames.add(matcher.group(1));
        }
        
        // Extract values from actual path
        String[] patternParts = pattern.split("/");
        String[] actualParts = actualPath.split("/");
        
        if (patternParts.length == actualParts.length) {
            for (int i = 0; i < patternParts.length; i++) {
                String patternPart = patternParts[i];
                if (patternPart.startsWith("{") && patternPart.endsWith("}")) {
                    String varName = patternPart.substring(1, patternPart.length() - 1);
                    vars.put(varName, actualParts[i]);
                }
            }
        }
        
        return vars;
    }
    
    private String normalizePath(String path) {
        if (path == null) {
            return "/";
        }
        // Ensure path starts with /
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        // Remove trailing / (except root)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class RouteMatch {
        private MockRoute route;
        private Map<String, String> pathVariables;
    }
    
    private static class PatternRoute {
        final String path;
        final Pattern pattern;
        final MockRoute route;
        
        PatternRoute(String path, Pattern pattern, MockRoute route) {
            this.path = path;
            this.pattern = pattern;
            this.route = route;
        }
    }
}

