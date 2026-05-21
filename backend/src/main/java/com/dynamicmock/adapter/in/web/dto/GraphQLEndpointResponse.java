package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.GraphQLEndpoint;
import com.dynamicmock.domain.entity.GraphQLEndpoint.ResolverConfig;
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
public class GraphQLEndpointResponse {
    private String id;
    private String name;
    private String description;
    private String schema;
    private List<ResolverConfig> resolvers;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static GraphQLEndpointResponse from(GraphQLEndpoint endpoint) {
        return GraphQLEndpointResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .description(endpoint.getDescription())
                .schema(endpoint.getSchema())
                .resolvers(endpoint.getResolvers())
                .active(endpoint.getActive())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}

