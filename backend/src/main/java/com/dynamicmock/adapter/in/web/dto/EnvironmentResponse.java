package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.Environment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentResponse {
    private String id;
    private String name;
    private Map<String, String> variables;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EnvironmentResponse from(Environment domain) {
        return EnvironmentResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .variables(domain.getVariables())
                .isDefault(domain.getIsDefault())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
