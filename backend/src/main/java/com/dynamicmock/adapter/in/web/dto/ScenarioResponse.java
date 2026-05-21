package com.dynamicmock.adapter.in.web.dto;

/**
 * ADAPTER LAYER - Output DTO
 */

import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
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
public class ScenarioResponse {
    
    private String id;
    private String name;
    private String description;
    private String initialState;
    private String currentState;
    private List<ScenarioState> states;
    private Integer maxExecutions;
    private Integer executionCount;
    private Boolean autoReset;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static ScenarioResponse from(Scenario scenario) {
        return ScenarioResponse.builder()
                .id(scenario.getId())
                .name(scenario.getName())
                .description(scenario.getDescription())
                .initialState(scenario.getInitialState())
                .currentState(scenario.getCurrentState())
                .states(scenario.getStates())
                .maxExecutions(scenario.getMaxExecutions())
                .executionCount(scenario.getExecutionCount())
                .autoReset(scenario.getAutoReset())
                .active(scenario.getActive())
                .createdAt(scenario.getCreatedAt())
                .updatedAt(scenario.getUpdatedAt())
                .build();
    }
}

