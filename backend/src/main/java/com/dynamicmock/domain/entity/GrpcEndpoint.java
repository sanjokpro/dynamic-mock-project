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
 * Clean Architecture: gRPC service configuration entity.
 * Encapsulates protobuf service and method definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrpcEndpoint {
    
    private String id;
    
    private String name;
    private String description;
    
    /**
     * Service name (fully qualified, e.g., "com.example.UserService")
     */
    private String serviceName;
    
    /**
     * Protobuf schema definition
     */
    private String protoSchema;
    
    /**
     * Method configurations
     */
    private List<MethodConfig> methods;
    
    /**
     * Port to run this gRPC service on
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
        private String methodName;
        private String methodType;
        private String responseTemplate;
        private List<String> streamResponses;
        private String script;
        private String scriptLanguage;
        private Integer delayMs;
        private String statusCode;
        private String errorMessage;
        private Map<String, Object> matchers;
    }
}
