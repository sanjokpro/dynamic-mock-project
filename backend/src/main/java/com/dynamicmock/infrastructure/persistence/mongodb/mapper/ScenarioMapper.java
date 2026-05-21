package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.ScenarioMongoEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper between Domain Scenario and MongoDB ScenarioMongoEntity.
 */
@Component
public class ScenarioMapper {

    public Scenario toDomain(ScenarioMongoEntity entity) {
        if (entity == null) return null;
        return Scenario.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .initialState(entity.getInitialState())
                .currentState(entity.getCurrentState())
                .active(entity.getActive())
                .maxExecutions(entity.getMaxExecutions())
                .executionCount(entity.getExecutionCount())
                .autoReset(entity.getAutoReset())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .states(entity.getStates().stream().map(this::stateToDomain).collect(Collectors.toList()))
                .build();
    }

    private Scenario.ScenarioState stateToDomain(ScenarioMongoEntity.ScenarioStateEntity entity) {
        if (entity == null) return null;
        return Scenario.ScenarioState.builder()
                .name(entity.getName())
                .description(entity.getDescription())
                .responseTemplate(entity.getResponseTemplate())
                .responseStatus(entity.getResponseStatus())
                .responseHeaders(entity.getResponseHeaders())
                .delayMs(entity.getDelayMs())
                .preScript(entity.getPreScript())
                .postScript(entity.getPostScript())
                .scriptLanguage(entity.getScriptLanguage())
                .transitions(entity.getTransitions().stream().map(this::transitionToDomain).collect(Collectors.toList()))
                .build();
    }

    private Scenario.StateTransition transitionToDomain(ScenarioMongoEntity.StateTransitionEntity entity) {
        if (entity == null) return null;
        return Scenario.StateTransition.builder()
                .name(entity.getName())
                .condition(entity.getCondition())
                .targetState(entity.getTargetState())
                .priority(entity.getPriority())
                .build();
    }

    public ScenarioMongoEntity toEntity(Scenario domain) {
        if (domain == null) return null;
        return ScenarioMongoEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .initialState(domain.getInitialState())
                .currentState(domain.getCurrentState())
                .active(domain.getActive())
                .maxExecutions(domain.getMaxExecutions())
                .executionCount(domain.getExecutionCount())
                .autoReset(domain.getAutoReset())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .states(domain.getStates().stream().map(this::stateToEntity).collect(Collectors.toList()))
                .build();
    }

    private ScenarioMongoEntity.ScenarioStateEntity stateToEntity(Scenario.ScenarioState domain) {
        if (domain == null) return null;
        return ScenarioMongoEntity.ScenarioStateEntity.builder()
                .name(domain.getName())
                .description(domain.getDescription())
                .responseTemplate(domain.getResponseTemplate())
                .responseStatus(domain.getResponseStatus())
                .responseHeaders(domain.getResponseHeaders())
                .delayMs(domain.getDelayMs())
                .preScript(domain.getPreScript())
                .postScript(domain.getPostScript())
                .scriptLanguage(domain.getScriptLanguage())
                .transitions(domain.getTransitions().stream().map(this::transitionToEntity).collect(Collectors.toList()))
                .build();
    }

    private ScenarioMongoEntity.StateTransitionEntity transitionToEntity(Scenario.StateTransition domain) {
        if (domain == null) return null;
        return ScenarioMongoEntity.StateTransitionEntity.builder()
                .name(domain.getName())
                .condition(domain.getCondition())
                .targetState(domain.getTargetState())
                .priority(domain.getPriority())
                .build();
    }
}
