package com.dynamicmock.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: Core business entity representing a mock HTTP route.
 * Independent of frameworks and external concerns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockRoute {
    
    private String id;
    
    private String path;
    private String method; // GET, POST, PUT, DELETE, etc.
    
    private Map<String, Object> matchers; // Request matching rules (headers, query params, body)
    
    private String responseTemplate; // Handlebars template
    private Integer responseStatus;
    private Map<String, String> responseHeaders;
    
    private String preScript; // JavaScript or Python script
    private String postScript;
    private String scriptLanguage; // 'js' or 'python'
    
    private Integer delayMs;
    private Integer version;
    
    private Boolean active; // Whether route is published to registry
    
    /**
     * Name of the scenario this route is linked to (optional)
     * When set, the route will use the scenario's current state to determine response
     */
    private String scenarioName;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
