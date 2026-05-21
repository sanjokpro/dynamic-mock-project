package com.dynamicmock.infrastructure.persistence.mongodb.entity;

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
 * Infrastructure Layer - MongoDB Entity
 * persistence-specific representation of MockRoute.
 */
@Document(collection = "mock_routes")
@CompoundIndex(name = "path_method_version_idx", def = "{'path': 1, 'method': 1, 'version': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockRouteMongoEntity {
    
    @Id
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
}
