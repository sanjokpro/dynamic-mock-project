package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.GrpcEndpoint.MethodConfig;
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
public class GrpcEndpointRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String description;
    @NotBlank(message = "Service name is required")
    private String serviceName;
    private String protoSchema;
    private List<MethodConfig> methods;
    private Integer port;
    private Boolean active;
}

