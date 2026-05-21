package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.dynamicmock.domain.entity.GrpcEndpoint.MethodConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrpcEndpointResponse {
    private String id;
    private String name;
    private String description;
    private String serviceName;
    private String protoSchema;
    private List<MethodConfig> methods;
    private Integer port;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static GrpcEndpointResponse from(GrpcEndpoint endpoint) {
        return GrpcEndpointResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .description(endpoint.getDescription())
                .serviceName(endpoint.getServiceName())
                .protoSchema(endpoint.getProtoSchema())
                .methods(endpoint.getMethods())
                .port(endpoint.getPort())
                .active(endpoint.getActive())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}

