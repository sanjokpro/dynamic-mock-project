package com.dynamicmock.application.service;

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
import java.util.*;

/**
 * Service for managing route versions, providing rollback and diff functionality.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {
    
    private final RouteVersionRepository versionRepository;
    private final MockRouteRepository routeRepository;
    private final RouteRegistry routeRegistry;
    
    /**
     * Save a new version of a route before making changes
     */
    public RouteVersion saveVersion(MockRoute route, String changeDescription) {
        // Get the next version number
        int nextVersion = versionRepository.findTopByRouteIdOrderByVersionNumberDesc(route.getId())
            .map(v -> v.getVersionNumber() + 1)
            .orElse(1);
        
        RouteVersion version = RouteVersion.fromRoute(route, nextVersion, changeDescription);
        version.setId(UUID.randomUUID().toString());
        version = versionRepository.save(version);
        
        log.info("Saved version {} for route {}", nextVersion, route.getId());
        return version;
    }
    
    /**
     * Get all versions of a route
     */
    public List<RouteVersion> getVersionHistory(String routeId) {
        return versionRepository.findByRouteIdOrderByVersionNumberDesc(routeId);
    }
    
    /**
     * Get a specific version of a route
     */
    public RouteVersion getVersion(String routeId, int versionNumber) {
        return versionRepository.findByRouteIdAndVersionNumber(routeId, versionNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + versionNumber + " not found for route " + routeId));
    }
    
    /**
     * Rollback a route to a specific version
     */
    @Transactional
    public MockRoute rollbackToVersion(String routeId, int versionNumber) {
        MockRoute route = routeRepository.findById(routeId)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + routeId));
        
        RouteVersion targetVersion = versionRepository.findByRouteIdAndVersionNumber(routeId, versionNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + versionNumber + " not found for route " + routeId));
        
        // Save current state as a new version before rollback
        saveVersion(route, "Auto-save before rollback to version " + versionNumber);
        
        boolean wasActive = Boolean.TRUE.equals(route.getActive());
        
        // Apply the target version
        targetVersion.applyTo(route);
        route.setUpdatedAt(LocalDateTime.now());
        
        // Increment the user-facing version number
        route.setVersion(route.getVersion() != null ? route.getVersion() + 1 : versionNumber);
        
        route = routeRepository.save(route);
        
        // Update registry if active
        if (wasActive) {
            routeRegistry.unregister(route.getMethod(), route.getPath());
            routeRegistry.register(route);
        }
        
        log.info("Rolled back route {} to version {}", routeId, versionNumber);
        return route;
    }
    
    /**
     * Compare two versions of a route and return the differences
     */
    public VersionDiff diffVersions(String routeId, int version1, int version2) {
        RouteVersion v1 = versionRepository.findByRouteIdAndVersionNumber(routeId, version1)
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + version1 + " not found for route " + routeId));
        
        RouteVersion v2 = versionRepository.findByRouteIdAndVersionNumber(routeId, version2)
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + version2 + " not found for route " + routeId));
        
        return compareVersions(v1, v2);
    }
    
    /**
     * Compare current route state with a specific version
     */
    public VersionDiff diffWithCurrent(String routeId, int versionNumber) {
        MockRoute currentRoute = routeRepository.findById(routeId)
            .orElseThrow(() -> new IllegalArgumentException("Route not found: " + routeId));
        
        RouteVersion version = versionRepository.findByRouteIdAndVersionNumber(routeId, versionNumber)
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + versionNumber + " not found for route " + routeId));
        
        // Create a "pseudo-version" from current state for comparison
        RouteVersion currentVersion = RouteVersion.fromRoute(currentRoute, 0, "Current");
        
        return compareVersions(version, currentVersion);
    }
    
    /**
     * Delete all versions for a route
     */
    public void deleteVersionHistory(String routeId) {
        versionRepository.deleteByRouteId(routeId);
        log.info("Deleted version history for route {}", routeId);
    }
    
    /**
     * Compare two versions and return the differences
     */
    private VersionDiff compareVersions(RouteVersion v1, RouteVersion v2) {
        List<FieldDiff> changes = new ArrayList<>();
        
        compareField(changes, "path", v1.getPath(), v2.getPath());
        compareField(changes, "method", v1.getMethod(), v2.getMethod());
        compareField(changes, "responseTemplate", v1.getResponseTemplate(), v2.getResponseTemplate());
        compareField(changes, "responseStatus", v1.getResponseStatus(), v2.getResponseStatus());
        compareField(changes, "preScript", v1.getPreScript(), v2.getPreScript());
        compareField(changes, "postScript", v1.getPostScript(), v2.getPostScript());
        compareField(changes, "scriptLanguage", v1.getScriptLanguage(), v2.getScriptLanguage());
        compareField(changes, "delayMs", v1.getDelayMs(), v2.getDelayMs());
        compareField(changes, "scenarioName", v1.getScenarioName(), v2.getScenarioName());
        
        // Compare maps
        compareMapField(changes, "matchers", v1.getMatchers(), v2.getMatchers());
        compareMapField(changes, "responseHeaders", v1.getResponseHeaders(), v2.getResponseHeaders());
        
        return VersionDiff.builder()
            .routeId(v1.getRouteId())
            .version1(v1.getVersionNumber())
            .version2(v2.getVersionNumber())
            .changes(changes)
            .totalChanges(changes.size())
            .build();
    }
    
    private void compareField(List<FieldDiff> changes, String fieldName, Object value1, Object value2) {
        boolean v1Null = value1 == null || (value1 instanceof String && ((String) value1).isEmpty());
        boolean v2Null = value2 == null || (value2 instanceof String && ((String) value2).isEmpty());
        
        if (v1Null && v2Null) {
            return; // Both null/empty, no change
        }
        
        if (v1Null != v2Null || !Objects.equals(value1, value2)) {
            changes.add(FieldDiff.builder()
                .field(fieldName)
                .oldValue(value1 != null ? value1.toString() : null)
                .newValue(value2 != null ? value2.toString() : null)
                .changeType(determineChangeType(value1, value2))
                .build());
        }
    }
    
    private void compareMapField(List<FieldDiff> changes, String fieldName, Map<String, ?> map1, Map<String, ?> map2) {
        map1 = map1 != null ? map1 : Map.of();
        map2 = map2 != null ? map2 : Map.of();
        
        if (!map1.equals(map2)) {
            changes.add(FieldDiff.builder()
                .field(fieldName)
                .oldValue(map1.toString())
                .newValue(map2.toString())
                .changeType(map1.isEmpty() ? "ADDED" : (map2.isEmpty() ? "REMOVED" : "MODIFIED"))
                .build());
        }
    }
    
    private String determineChangeType(Object old, Object newVal) {
        if (old == null || (old instanceof String && ((String) old).isEmpty())) {
            return "ADDED";
        }
        if (newVal == null || (newVal instanceof String && ((String) newVal).isEmpty())) {
            return "REMOVED";
        }
        return "MODIFIED";
    }
    
    /**
     * Represents the diff between two versions
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VersionDiff {
        private String routeId;
        private Integer version1;
        private Integer version2;
        private List<FieldDiff> changes;
        private int totalChanges;
    }
    
    /**
     * Represents a single field difference
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FieldDiff {
        private String field;
        private String oldValue;
        private String newValue;
        private String changeType; // ADDED, REMOVED, MODIFIED
    }
}

