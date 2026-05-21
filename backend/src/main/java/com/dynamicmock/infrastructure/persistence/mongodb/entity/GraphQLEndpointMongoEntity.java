package com.dynamicmock.infrastructure.persistence.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure Layer - MongoDB Entity for GraphQL Endpoint.
 */
@Document(collection = "graphql_endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLEndpointMongoEntity {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    
    private String schema;
    
    private List<ResolverConfigEntity> resolvers;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolverConfigEntity {
        private String operationType;
        private String fieldName;
        private String responseTemplate;
        private String script;
        private String scriptLanguage;
        private Integer delayMs;
        private Map<String, Object> matchers;
    }
}
