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
 * Clean Architecture: Stateful scenario entity for workflow-based mocking.
 * Contains state machine logic independent of infrastructure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {
    
    private String id;
    
    private String name;
    
    private String description;
    
    /**
     * The starting state for this scenario
     */
    private String initialState;
    
    /**
     * Current state of this scenario (runtime)
     */
    private String currentState;
    
    /**
     * List of all possible states in this scenario
     */
    private List<ScenarioState> states;
    
    /**
     * Whether this scenario is active
     */
    private Boolean active;
    
    /**
     * Maximum number of times this scenario can be executed before reset
     */
    private Integer maxExecutions;
    
    /**
     * Current execution count
     */
    private Integer executionCount;
    
    /**
     * Whether to auto-reset after completing the scenario
     */
    private Boolean autoReset;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Represents a single state in the scenario
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioState {
        private String name;
        private String description;
        private String responseTemplate;
        private Integer responseStatus;
        private Map<String, String> responseHeaders;
        private Integer delayMs;
        private List<StateTransition> transitions;
        private String preScript;
        private String postScript;
        private String scriptLanguage;
    }
    
    /**
     * Represents a transition between states
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateTransition {
        private String name;
        private String condition;
        private String targetState;
        private Integer priority;
    }
}
