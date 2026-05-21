package com.dynamicmock.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: GraphQL endpoint configuration entity.
 * Encapsulates GraphQL schema and resolver definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLEndpoint {
    
    private String id;
    
    private String name;
    private String description;
    
    /**
     * GraphQL schema definition (SDL format)
     */
    private String schema;
    
    /**
     * Resolver configurations for queries and mutations
     */
    private List<ResolverConfig> resolvers;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResolverConfig {
        private String operationType;
        private String fieldName;
        private String responseTemplate;
        private String script;
        private String scriptLanguage;
        private Integer delayMs;
        private Map<String, Object> matchers;
    }
}
