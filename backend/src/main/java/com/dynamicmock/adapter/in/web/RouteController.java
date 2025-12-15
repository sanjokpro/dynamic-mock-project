package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.in.web.dto.UpdateRouteRequest;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.application.service.VersionService;
import com.dynamicmock.application.service.VersionService.VersionDiff;
import com.dynamicmock.domain.entity.RouteVersion;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing mock routes
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {
    
    private final RouteService routeService;
    private final VersionService versionService;
    
    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    public ResponseEntity<List<RouteResponse>> listRoutes() {
        List<RouteResponse> routes = routeService.listRoutes();
        return ResponseEntity.ok(routes);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable String id) {
        try {
            RouteResponse route = routeService.getRoute(id);
            return ResponseEntity.ok(route);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RouteResponse> updateRoute(
            @PathVariable String id,
            @RequestBody UpdateRouteRequest request) {
        RouteResponse route = routeService.updateRoute(id, request);
        return ResponseEntity.ok(route);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable String id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/activate")
    public ResponseEntity<RouteResponse> activateRoute(@PathVariable String id) {
        RouteResponse route = routeService.activateRoute(id);
        return ResponseEntity.ok(route);
    }
    
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<RouteResponse> deactivateRoute(@PathVariable String id) {
        RouteResponse route = routeService.deactivateRoute(id);
        return ResponseEntity.ok(route);
    }
    
    // ============== Version Management Endpoints ==============
    
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<RouteVersion>> getVersionHistory(@PathVariable String id) {
        List<RouteVersion> versions = versionService.getVersionHistory(id);
        return ResponseEntity.ok(versions);
    }
    
    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<RouteVersion> getVersion(
            @PathVariable String id,
            @PathVariable int versionNumber) {
        try {
            RouteVersion version = versionService.getVersion(id, versionNumber);
            return ResponseEntity.ok(version);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{id}/versions/{versionNumber}/rollback")
    public ResponseEntity<RouteResponse> rollbackToVersion(
            @PathVariable String id,
            @PathVariable int versionNumber) {
        try {
            var route = versionService.rollbackToVersion(id, versionNumber);
            return ResponseEntity.ok(routeService.getRoute(route.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/versions/diff")
    public ResponseEntity<VersionDiff> diffVersions(
            @PathVariable String id,
            @RequestParam int v1,
            @RequestParam int v2) {
        try {
            VersionDiff diff = versionService.diffVersions(id, v1, v2);
            return ResponseEntity.ok(diff);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{id}/versions/{versionNumber}/diff")
    public ResponseEntity<VersionDiff> diffWithCurrent(
            @PathVariable String id,
            @PathVariable int versionNumber) {
        try {
            VersionDiff diff = versionService.diffWithCurrent(id, versionNumber);
            return ResponseEntity.ok(diff);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

