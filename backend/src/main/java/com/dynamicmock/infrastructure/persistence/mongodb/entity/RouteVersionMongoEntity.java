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
 * Infrastructure Layer - MongoDB Entity for Route Version.
 */
@Document(collection = "route_versions")
@CompoundIndex(name = "route_version_idx", def = "{'routeId': 1, 'versionNumber': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteVersionMongoEntity {
    
    @Id
    private String id;
    
    private String routeId;
    
    private Integer versionNumber;
    
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
    
    private String changeDescription;
    
    private String changedBy;
    
    private LocalDateTime createdAt;
}
