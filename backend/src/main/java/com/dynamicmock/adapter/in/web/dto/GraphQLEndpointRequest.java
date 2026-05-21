package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.GraphQLEndpoint.ResolverConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphQLEndpointRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @NotBlank(message = "Schema is required")
    private String schema;
    private List<ResolverConfig> resolvers;
    private Boolean active;
}

