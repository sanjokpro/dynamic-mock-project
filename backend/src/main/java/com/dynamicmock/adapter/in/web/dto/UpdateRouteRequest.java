package com.dynamicmock.adapter.in.web.dto;

/**
 * ADAPTER LAYER - Input DTO
 * Clean Architecture: DTO for updating existing routes.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRouteRequest {
    
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
    private Integer version;
    private Boolean active;
    private String scenarioName;
}

