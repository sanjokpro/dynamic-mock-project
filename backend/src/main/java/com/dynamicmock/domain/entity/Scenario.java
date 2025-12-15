package com.dynamicmock.domain.entity;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: Stateful scenario entity for workflow-based mocking.
 * Contains state machine logic independent of infrastructure.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Scenario model for stateful mocking workflows.
 * A scenario defines a sequence of states that determine mock responses.
 * 
 * Example usage:
 * - E-commerce checkout flow (cart → payment → confirmation)
 * - User authentication states (logged-out → logged-in → session-expired)
 * - API pagination (first-page → middle-pages → last-page)
 */
@Document(collection = "scenarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scenario {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
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
        /**
         * Unique name for this state
         */
        private String name;
        
        /**
         * Description of this state
         */
        private String description;
        
        /**
         * Response template to use when in this state
         */
        private String responseTemplate;
        
        /**
         * Response status code for this state
         */
        private Integer responseStatus;
        
        /**
         * Response headers for this state
         */
        private Map<String, String> responseHeaders;
        
        /**
         * Delay in milliseconds for this state
         */
        private Integer delayMs;
        
        /**
         * Transitions to other states
         * Map of: trigger condition -> next state name
         */
        private List<StateTransition> transitions;
        
        /**
         * Pre-script to execute in this state
         */
        private String preScript;
        
        /**
         * Post-script to execute in this state
         */
        private String postScript;
        
        /**
         * Script language (js or python)
         */
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
        /**
         * Name of this transition
         */
        private String name;
        
        /**
         * Condition to trigger this transition (evaluated as expression)
         * Examples:
         * - "request.method == 'POST'" 
         * - "request.path.contains('/checkout')"
         * - "state.counter > 3"
         * - "always" (unconditional transition after response)
         */
        private String condition;
        
        /**
         * The state to transition to
         */
        private String targetState;
        
        /**
         * Priority (lower = higher priority, checked first)
         */
        private Integer priority;
    }
}

