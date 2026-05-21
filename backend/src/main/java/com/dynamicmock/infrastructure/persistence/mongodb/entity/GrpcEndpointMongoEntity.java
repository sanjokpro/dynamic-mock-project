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
 * Infrastructure Layer - MongoDB Entity for gRPC Endpoint.
 */
@Document(collection = "grpc_endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrpcEndpointMongoEntity {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    
    private String serviceName;
    
    private String protoSchema;
    
    private List<MethodConfigEntity> methods;
    
    private Integer port;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MethodConfigEntity {
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
