package com.dynamicmock.domain.entity;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: gRPC service configuration entity.
 * Encapsulates protobuf service and method definitions.
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
 * Model for gRPC mock endpoints
 * Supports protobuf service definition and method mocking
 */
@Document(collection = "grpc_endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrpcEndpoint {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    
    /**
     * Service name (fully qualified, e.g., "com.example.UserService")
     */
    private String serviceName;
    
    /**
     * Protobuf schema definition
     * Example:
     * syntax = "proto3";
     * package com.example;
     * service UserService {
     *   rpc GetUser (GetUserRequest) returns (User);
     *   rpc ListUsers (ListUsersRequest) returns (ListUsersResponse);
     * }
     */
    private String protoSchema;
    
    /**
     * Method configurations
     */
    private List<MethodConfig> methods;
    
    /**
     * Port to run this gRPC service on (optional, uses default if not set)
     */
    private Integer port;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodConfig {
        /**
         * Method name in the service (e.g., "GetUser", "ListUsers")
         */
        private String methodName;
        
        /**
         * Method type: UNARY, SERVER_STREAMING, CLIENT_STREAMING, BIDI_STREAMING
         */
        private String methodType;
        
        /**
         * Response template (JSON format that will be converted to protobuf)
         * Supports Handlebars templating
         */
        private String responseTemplate;
        
        /**
         * For streaming responses, list of response templates
         */
        private List<String> streamResponses;
        
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
         * gRPC status code to return (default: OK)
         */
        private String statusCode;
        
        /**
         * Error message if status is not OK
         */
        private String errorMessage;
        
        /**
         * Request matchers for conditional responses
         */
        private Map<String, Object> matchers;
    }
}

