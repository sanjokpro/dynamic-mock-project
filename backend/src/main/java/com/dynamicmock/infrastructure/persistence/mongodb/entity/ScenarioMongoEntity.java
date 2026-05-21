package com.dynamicmock.infrastructure.persistence.mongodb.entity;

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
 * Infrastructure Layer - MongoDB Entity for Scenario.
 */
@Document(collection = "scenarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioMongoEntity {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    private String description;
    
    private String initialState;
    
    private String currentState;
    
    private List<ScenarioStateEntity> states;
    
    private Boolean active;
    
    private Integer maxExecutions;
    
    private Integer executionCount;
    
    private Boolean autoReset;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScenarioStateEntity {
        private String name;
        private String description;
        private String responseTemplate;
        private Integer responseStatus;
        private Map<String, String> responseHeaders;
        private Integer delayMs;
        private List<StateTransitionEntity> transitions;
        private String preScript;
        private String postScript;
        private String scriptLanguage;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateTransitionEntity {
        private String name;
        private String condition;
        private String targetState;
        private Integer priority;
    }
}
