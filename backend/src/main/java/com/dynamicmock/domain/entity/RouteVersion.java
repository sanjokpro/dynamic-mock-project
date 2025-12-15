package com.dynamicmock.domain.entity;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: Version history entity for route evolution tracking.
 * Immutable snapshot of route state.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Stores historical versions of mock routes for rollback and diff functionality.
 * Each time a route is updated, a new version record is created.
 */
@Document(collection = "route_versions")
@CompoundIndex(name = "route_version_idx", def = "{'routeId': 1, 'versionNumber': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteVersion {
    
    @Id
    private String id;
    
    /**
     * Reference to the original route
     */
    private String routeId;
    
    /**
     * Version number (incremental)
     */
    private Integer versionNumber;
    
    /**
     * Snapshot of the route at this version
     */
    private String path;
    private String method;
    private Map<String, Object> matchers;
    private String responseTemplate;
    private Integer responseStatus;
    private Map<String, String> responseHeaders;
    private String preScript;
    private String postScript;
    private String scriptLanguage;
    private Integer delayMs;
    private String scenarioName;
    
    /**
     * Description of what changed in this version
     */
    private String changeDescription;
    
    /**
     * Who made this change (for audit purposes)
     */
    private String changedBy;
    
    /**
     * When this version was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Create a version from a MockRoute
     */
    public static RouteVersion fromRoute(MockRoute route, int versionNumber, String changeDescription) {
        return RouteVersion.builder()
            .routeId(route.getId())
            .versionNumber(versionNumber)
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
            .scenarioName(route.getScenarioName())
            .changeDescription(changeDescription)
            .createdAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Apply this version's data to a MockRoute
     */
    public void applyTo(MockRoute route) {
        route.setPath(this.path);
        route.setMethod(this.method);
        route.setMatchers(this.matchers);
        route.setResponseTemplate(this.responseTemplate);
        route.setResponseStatus(this.responseStatus);
        route.setResponseHeaders(this.responseHeaders);
        route.setPreScript(this.preScript);
        route.setPostScript(this.postScript);
        route.setScriptLanguage(this.scriptLanguage);
        route.setDelayMs(this.delayMs);
        route.setScenarioName(this.scenarioName);
    }
}

