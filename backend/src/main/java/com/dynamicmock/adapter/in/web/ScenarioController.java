package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.ScenarioRequest;
import com.dynamicmock.adapter.in.web.dto.ScenarioResponse;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.Scenario;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for managing scenarios (stateful mocking workflows)
 */
@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {
    
    private final ScenarioService scenarioService;
    
    @PostMapping
    public ResponseEntity<ScenarioResponse> createScenario(@Valid @RequestBody ScenarioRequest request) {
        Scenario scenario = Scenario.builder()
            .name(request.getName())
            .description(request.getDescription())
            .initialState(request.getInitialState())
            .states(request.getStates())
            .maxExecutions(request.getMaxExecutions())
            .autoReset(request.getAutoReset())
            .active(request.getActive())
            .build();
        
        Scenario created = scenarioService.createScenario(scenario);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }
    
    @GetMapping
    public ResponseEntity<List<ScenarioResponse>> listScenarios() {
        List<ScenarioResponse> scenarios = scenarioService.listScenarios().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(scenarios);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ScenarioResponse> getScenario(@PathVariable String id) {
        try {
            Scenario scenario = scenarioService.getScenario(id);
            // Get current state from Redis
            String currentState = scenarioService.getCurrentState(scenario.getName());
            scenario.setCurrentState(currentState);
            return ResponseEntity.ok(toResponse(scenario));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ScenarioResponse> updateScenario(
            @PathVariable String id,
            @RequestBody ScenarioRequest request) {
        Scenario updates = Scenario.builder()
            .name(request.getName())
            .description(request.getDescription())
            .initialState(request.getInitialState())
            .states(request.getStates())
            .maxExecutions(request.getMaxExecutions())
            .autoReset(request.getAutoReset())
            .active(request.getActive())
            .build();
        
        Scenario updated = scenarioService.updateScenario(id, updates);
        return ResponseEntity.ok(toResponse(updated));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable String id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/activate")
    public ResponseEntity<ScenarioResponse> activateScenario(@PathVariable String id) {
        Scenario activated = scenarioService.activateScenario(id);
        return ResponseEntity.ok(toResponse(activated));
    }
    
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ScenarioResponse> deactivateScenario(@PathVariable String id) {
        Scenario deactivated = scenarioService.deactivateScenario(id);
        return ResponseEntity.ok(toResponse(deactivated));
    }
    
    @PostMapping("/{id}/reset")
    public ResponseEntity<ScenarioResponse> resetScenario(@PathVariable String id) {
        Scenario reset = scenarioService.resetScenario(id);
        return ResponseEntity.ok(toResponse(reset));
    }
    
    @GetMapping("/{id}/state")
    public ResponseEntity<Map<String, String>> getCurrentState(@PathVariable String id) {
        Scenario scenario = scenarioService.getScenario(id);
        String currentState = scenarioService.getCurrentState(scenario.getName());
        return ResponseEntity.ok(Map.of(
            "scenarioName", scenario.getName(),
            "currentState", currentState != null ? currentState : scenario.getInitialState()
        ));
    }
    
    @PostMapping("/{id}/transition")
    public ResponseEntity<Map<String, String>> triggerTransition(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> context) {
        Scenario scenario = scenarioService.getScenario(id);
        String previousState = scenarioService.getCurrentState(scenario.getName());
        String newState = scenarioService.processTransition(
            scenario.getName(), 
            context != null ? context : Map.of()
        );
        return ResponseEntity.ok(Map.of(
            "scenarioName", scenario.getName(),
            "previousState", previousState,
            "currentState", newState
        ));
    }
    
    private ScenarioResponse toResponse(Scenario scenario) {
        return ScenarioResponse.builder()
            .id(scenario.getId())
            .name(scenario.getName())
            .description(scenario.getDescription())
            .initialState(scenario.getInitialState())
            .currentState(scenario.getCurrentState())
            .states(scenario.getStates())
            .active(scenario.getActive())
            .maxExecutions(scenario.getMaxExecutions())
            .executionCount(scenario.getExecutionCount())
            .autoReset(scenario.getAutoReset())
            .createdAt(scenario.getCreatedAt())
            .updatedAt(scenario.getUpdatedAt())
            .build();
    }
}

