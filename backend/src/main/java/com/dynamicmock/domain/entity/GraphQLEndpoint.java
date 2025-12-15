package com.dynamicmock.domain.entity;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: GraphQL endpoint configuration entity.
 * Encapsulates GraphQL schema and resolver definitions.
 */

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
 * Model for GraphQL mock endpoints
 * Supports schema definition and resolver configuration
 */
@Document(collection = "graphql_endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLEndpoint {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    
    /**
     * GraphQL schema definition (SDL format)
     * Example:
     * type Query {
     *   user(id: ID!): User
     *   users: [User]
     * }
     * type User {
     *   id: ID!
     *   name: String
     *   email: String
     * }
     */
    private String schema;
    
    /**
     * Resolver configurations for queries and mutations
     * Maps operation names to response templates or scripts
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
        /**
         * Operation type: QUERY, MUTATION, SUBSCRIPTION
         */
        private String operationType;
        
        /**
         * Field name in the schema (e.g., "user", "users", "createUser")
         */
        private String fieldName;
        
        /**
         * Response template (Handlebars format)
         * Can use request variables, arguments, etc.
         */
        private String responseTemplate;
        
        /**
         * Optional script for dynamic response generation
         */
        private String script;
        private String scriptLanguage;
        
        /**
         * Fixed delay in milliseconds
         */
        private Integer delayMs;
        
        /**
         * Request matchers for conditional responses
         */
        private Map<String, Object> matchers;
    }
}

