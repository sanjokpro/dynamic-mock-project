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
    
    @NotBlank(message = "Path is required")
    private String path;
    
    @NotBlank(message = "Method is required")
    private String method;
    
    private Map<String, Object> matchers;
    
    private String responseTemplate;
    private Integer responseStatus;
    private Map<String, String> responseHeaders;
    
    private String preScript;
    private String postScript;
    private String scriptLanguage;
    
    private Integer delayMs;
    private Integer version;
    
    private String scenarioName;
}

