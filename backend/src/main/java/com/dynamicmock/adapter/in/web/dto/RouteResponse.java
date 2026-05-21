package com.dynamicmock.adapter.in.web.dto;

/**
 * ADAPTER LAYER - Output DTO
 * Clean Architecture: DTO for returning route data via REST API.
 * Maps domain entity to external REST response.
 */

import com.dynamicmock.domain.entity.MockRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponse {
    
    private String id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static RouteResponse from(MockRoute route) {
        return RouteResponse.builder()
                .id(route.getId())
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
                .version(route.getVersion())
                .active(route.getActive())
                .scenarioName(route.getScenarioName())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}

