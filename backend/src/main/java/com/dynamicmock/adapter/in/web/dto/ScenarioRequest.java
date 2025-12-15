package com.dynamicmock.adapter.in.web.dto;

/**
 * ADAPTER LAYER - Input DTO
 */

import com.dynamicmock.domain.entity.Scenario.ScenarioState;
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
public class ScenarioRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Initial state is required")
    private String initialState;
    
    private List<ScenarioState> states;
    
    private Integer maxExecutions;
    private Boolean autoReset;
    private Boolean active;
}

