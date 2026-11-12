package com.dynamicmock.adapter.in.web.dto;

/**
 * ADAPTER LAYER - Input DTO
 * Clean Architecture: Data Transfer Object for creating routes via REST API.
 * Maps external REST request to domain use case input.
 */

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRouteRequest {

    /** Optional explicit route ID (e.g. deterministic ID during import). */
    private String id;
    
    @NotBlank(message = "Path is required")
    private String path;
    
    @NotBlank(message = "Method is required")
    private String method;
    
    private Map<String, Object> matchers;
    private Map<String, String> queryParams;
    
    private String responseTemplate;
    private Integer responseStatus;
    private Map<String, String> responseHeaders;
    
    private String preScript;
    private String postScript;
    private String scriptLanguage;
    
    private Integer delayMs;
    private Integer version;
    
    private String scenarioName;

    /** Priority for deterministic route matching (higher wins; default 0). */
    private Integer priority;

    /** Generic metadata for provenance and tracking (e.g. source, postmanExampleId). */
    private Map<String, String> metadata;
}

