package com.dynamicmock.application.service;

import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
import com.dynamicmock.domain.entity.Scenario.StateTransition;
import com.dynamicmock.domain.port.out.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing scenarios and their state transitions.
 * Scenarios enable stateful mocking where responses depend on previous requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {
    
    private final ScenarioRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Cache for active scenarios
    private final Map<String, Scenario> scenarioCache = new ConcurrentHashMap<>();
    
    private static final String SCENARIO_STATE_PREFIX = "scenario:state:";
    private static final String SCENARIO_EXEC_PREFIX = "scenario:exec:";
    
    /**
     * Create a new scenario
     */
    public Scenario createScenario(Scenario scenario) {
        if (repository.existsByName(scenario.getName())) {
            throw new IllegalArgumentException("Scenario with name '" + scenario.getName() + "' already exists");
        }
        
        scenario.setId(UUID.randomUUID().toString());
        scenario.setCurrentState(scenario.getInitialState());
        scenario.setExecutionCount(0);
        scenario.setCreatedAt(LocalDateTime.now());
        scenario.setUpdatedAt(LocalDateTime.now());
        
        if (scenario.getActive() == null) {
            scenario.setActive(false);
        }
        if (scenario.getAutoReset() == null) {
            scenario.setAutoReset(true);
        }
        
        validateScenario(scenario);
        
        Scenario saved = repository.save(scenario);
        log.info("Created scenario: {} ({})", saved.getName(), saved.getId());
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            scenarioCache.put(saved.getName(), saved);
        }
        
        return saved;
    }
    
    /**
     * Update an existing scenario
     */
    public Scenario updateScenario(String id, Scenario updates) {
        Scenario scenario = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
        
        if (updates.getName() != null && !updates.getName().equals(scenario.getName())) {
            if (repository.existsByName(updates.getName())) {
                throw new IllegalArgumentException("Scenario with name '" + updates.getName() + "' already exists");
            }
            scenarioCache.remove(scenario.getName());
            scenario.setName(updates.getName());
        }
        
        if (updates.getDescription() != null) scenario.setDescription(updates.getDescription());
        if (updates.getInitialState() != null) scenario.setInitialState(updates.getInitialState());
        if (updates.getStates() != null) scenario.setStates(updates.getStates());
        if (updates.getMaxExecutions() != null) scenario.setMaxExecutions(updates.getMaxExecutions());
        if (updates.getAutoReset() != null) scenario.setAutoReset(updates.getAutoReset());
        if (updates.getActive() != null) scenario.setActive(updates.getActive());
        
        scenario.setUpdatedAt(LocalDateTime.now());
        
        validateScenario(scenario);
        
        Scenario saved = repository.save(scenario);
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            scenarioCache.put(saved.getName(), saved);
        } else {
            scenarioCache.remove(saved.getName());
        }
        
        log.info("Updated scenario: {} ({})", saved.getName(), saved.getId());
        return saved;
    }
    
    /**
     * Get scenario by ID
     */
    public Scenario getScenario(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
    }
    
    /**
     * Get scenario by name
     */
    public Optional<Scenario> getScenarioByName(String name) {
        // Check cache first
        if (scenarioCache.containsKey(name)) {
            return Optional.of(scenarioCache.get(name));
        }
        return repository.findByName(name);
    }
    
    /**
     * List all scenarios
     */
    public List<Scenario> listScenarios() {
        return repository.findAll();
    }
    
    /**
     * Delete a scenario
     */
    public void deleteScenario(String id) {
        Scenario scenario = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
        
        scenarioCache.remove(scenario.getName());
        clearScenarioState(scenario.getName());
        repository.deleteById(id);
        
        log.info("Deleted scenario: {} ({})", scenario.getName(), id);
    }
    
    /**
     * Activate a scenario
     */
    public Scenario activateScenario(String id) {
        Scenario scenario = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
        
        scenario.setActive(true);
        scenario.setUpdatedAt(LocalDateTime.now());
        scenario = repository.save(scenario);
        
        scenarioCache.put(scenario.getName(), scenario);
        log.info("Activated scenario: {} ({})", scenario.getName(), id);
        
        return scenario;
    }
    
    /**
     * Deactivate a scenario
     */
    public Scenario deactivateScenario(String id) {
        Scenario scenario = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
        
        scenario.setActive(false);
        scenario.setUpdatedAt(LocalDateTime.now());
        scenario = repository.save(scenario);
        
        scenarioCache.remove(scenario.getName());
        log.info("Deactivated scenario: {} ({})", scenario.getName(), id);
        
        return scenario;
    }
    
    /**
     * Reset a scenario to its initial state
     */
    public Scenario resetScenario(String id) {
        Scenario scenario = repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + id));
        
        scenario.setCurrentState(scenario.getInitialState());
        scenario.setExecutionCount(0);
        scenario.setUpdatedAt(LocalDateTime.now());
        scenario = repository.save(scenario);
        
        clearScenarioState(scenario.getName());
        
        if (Boolean.TRUE.equals(scenario.getActive())) {
            scenarioCache.put(scenario.getName(), scenario);
        }
        
        log.info("Reset scenario: {} ({})", scenario.getName(), id);
        return scenario;
    }
    
    /**
     * Get the current state for a scenario (from Redis for distributed consistency)
     */
    public String getCurrentState(String scenarioName) {
        String key = SCENARIO_STATE_PREFIX + scenarioName;
        Object state = redisTemplate.opsForValue().get(key);
        
        if (state != null) {
            return state.toString();
        }
        
        // Fall back to cached/db scenario
        Scenario scenario = scenarioCache.get(scenarioName);
        if (scenario == null) {
            scenario = repository.findByName(scenarioName).orElse(null);
        }
        
        if (scenario != null) {
            String initialState = scenario.getInitialState();
            setCurrentState(scenarioName, initialState);
            return initialState;
        }
        
        return null;
    }
    
    /**
     * Set the current state for a scenario
     */
    public void setCurrentState(String scenarioName, String state) {
        String key = SCENARIO_STATE_PREFIX + scenarioName;
        redisTemplate.opsForValue().set(key, state, Duration.ofHours(24));
        log.debug("Set scenario '{}' state to '{}'", scenarioName, state);
    }
    
    /**
     * Get the current state object (full state definition) for a scenario
     */
    public ScenarioState getCurrentStateObject(String scenarioName) {
        String currentStateName = getCurrentState(scenarioName);
        if (currentStateName == null) {
            return null;
        }
        
        Scenario scenario = scenarioCache.get(scenarioName);
        if (scenario == null) {
            scenario = repository.findByName(scenarioName).orElse(null);
        }
        
        if (scenario != null && scenario.getStates() != null) {
            return scenario.getStates().stream()
                .filter(s -> currentStateName.equals(s.getName()))
                .findFirst()
                .orElse(null);
        }
        
        return null;
    }
    
    /**
     * Process a state transition based on the current context
     */
    public String processTransition(String scenarioName, Map<String, Object> context) {
        String currentStateName = getCurrentState(scenarioName);
        if (currentStateName == null) {
            return null;
        }
        
        Scenario scenario = scenarioCache.get(scenarioName);
        if (scenario == null) {
            scenario = repository.findByName(scenarioName).orElse(null);
        }
        
        if (scenario == null || scenario.getStates() == null) {
            return currentStateName;
        }
        
        ScenarioState currentState = scenario.getStates().stream()
            .filter(s -> currentStateName.equals(s.getName()))
            .findFirst()
            .orElse(null);
        
        if (currentState == null || currentState.getTransitions() == null) {
            return currentStateName;
        }
        
        // Sort transitions by priority
        List<StateTransition> transitions = new ArrayList<>(currentState.getTransitions());
        transitions.sort(Comparator.comparingInt(t -> t.getPriority() != null ? t.getPriority() : 100));
        
        // Find first matching transition
        for (StateTransition transition : transitions) {
            if (evaluateCondition(transition.getCondition(), context)) {
                String nextState = transition.getTargetState();
                setCurrentState(scenarioName, nextState);
                incrementExecutionCount(scenarioName);
                
                log.info("Scenario '{}' transitioned: {} -> {} (condition: {})", 
                    scenarioName, currentStateName, nextState, transition.getCondition());
                
                // Check for auto-reset
                checkAutoReset(scenario, nextState);
                
                return nextState;
            }
        }
        
        return currentStateName;
    }
    
    /**
     * Increment execution count for a scenario
     */
    private void incrementExecutionCount(String scenarioName) {
        String key = SCENARIO_EXEC_PREFIX + scenarioName;
        redisTemplate.opsForValue().increment(key);
    }
    
    /**
     * Check if scenario should auto-reset
     */
    private void checkAutoReset(Scenario scenario, String currentState) {
        if (!Boolean.TRUE.equals(scenario.getAutoReset())) {
            return;
        }
        
        // Check if we're at a terminal state (no outgoing transitions)
        if (scenario.getStates() != null) {
            ScenarioState state = scenario.getStates().stream()
                .filter(s -> currentState.equals(s.getName()))
                .findFirst()
                .orElse(null);
            
            if (state != null && (state.getTransitions() == null || state.getTransitions().isEmpty())) {
                // Terminal state reached, reset to initial
                log.info("Scenario '{}' reached terminal state '{}', auto-resetting", scenario.getName(), currentState);
                setCurrentState(scenario.getName(), scenario.getInitialState());
            }
        }
    }
    
    /**
     * Evaluate a transition condition
     */
    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        if (condition == null || condition.isEmpty() || "always".equalsIgnoreCase(condition)) {
            return true;
        }
        
        try {
            // Simple expression evaluation for common patterns
            // For complex conditions, we could integrate with the ScriptEngine
            
            if (condition.contains("==")) {
                String[] parts = condition.split("==", 2);
                String left = evaluateExpression(parts[0].trim(), context);
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                return left.equals(right);
            }
            
            if (condition.contains("!=")) {
                String[] parts = condition.split("!=", 2);
                String left = evaluateExpression(parts[0].trim(), context);
                String right = parts[1].trim().replace("'", "").replace("\"", "");
                return !left.equals(right);
            }
            
            if (condition.contains(".contains(")) {
                int dotIndex = condition.indexOf(".contains(");
                String varPath = condition.substring(0, dotIndex);
                String searchStr = condition.substring(dotIndex + 10, condition.length() - 1)
                    .replace("'", "").replace("\"", "");
                String value = evaluateExpression(varPath, context);
                return value.contains(searchStr);
            }
            
            if (condition.contains(">")) {
                String[] parts = condition.split(">", 2);
                int left = Integer.parseInt(evaluateExpression(parts[0].trim(), context));
                int right = Integer.parseInt(parts[1].trim());
                return left > right;
            }
            
            if (condition.contains("<")) {
                String[] parts = condition.split("<", 2);
                int left = Integer.parseInt(evaluateExpression(parts[0].trim(), context));
                int right = Integer.parseInt(parts[1].trim());
                return left < right;
            }
            
            // Default: treat as boolean check
            String value = evaluateExpression(condition, context);
            return "true".equalsIgnoreCase(value) || !value.isEmpty();
            
        } catch (Exception e) {
            log.warn("Failed to evaluate condition '{}': {}", condition, e.getMessage());
            return false;
        }
    }
    
    /**
     * Evaluate a simple expression like "request.method" or "state.counter"
     */
    @SuppressWarnings("unchecked")
    private String evaluateExpression(String expression, Map<String, Object> context) {
        String[] parts = expression.split("\\.");
        Object current = context;
        
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return "";
            }
            if (current == null) {
                return "";
            }
        }
        
        return current.toString();
    }
    
    /**
     * Clear all state data for a scenario
     */
    private void clearScenarioState(String scenarioName) {
        redisTemplate.delete(SCENARIO_STATE_PREFIX + scenarioName);
        redisTemplate.delete(SCENARIO_EXEC_PREFIX + scenarioName);
    }
    
    /**
     * Validate a scenario definition
     */
    private void validateScenario(Scenario scenario) {
        if (scenario.getName() == null || scenario.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Scenario name is required");
        }
        
        if (scenario.getInitialState() == null || scenario.getInitialState().trim().isEmpty()) {
            throw new IllegalArgumentException("Initial state is required");
        }
        
        if (scenario.getStates() == null || scenario.getStates().isEmpty()) {
            throw new IllegalArgumentException("At least one state is required");
        }
        
        // Verify initial state exists
        boolean initialStateExists = scenario.getStates().stream()
            .anyMatch(s -> scenario.getInitialState().equals(s.getName()));
        
        if (!initialStateExists) {
            throw new IllegalArgumentException("Initial state '" + scenario.getInitialState() + "' not found in states");
        }
        
        // Verify all transition targets exist
        Set<String> stateNames = new HashSet<>();
        scenario.getStates().forEach(s -> stateNames.add(s.getName()));
        
        for (ScenarioState state : scenario.getStates()) {
            if (state.getTransitions() != null) {
                for (StateTransition transition : state.getTransitions()) {
                    if (!stateNames.contains(transition.getTargetState())) {
                        throw new IllegalArgumentException(
                            "Transition target state '" + transition.getTargetState() + 
                            "' not found (from state '" + state.getName() + "')");
                    }
                }
            }
        }
    }
    
    /**
     * Load all active scenarios into cache on startup
     */
    public void loadActiveScenarios() {
        List<Scenario> activeScenarios = repository.findByActiveTrue();
        activeScenarios.forEach(s -> scenarioCache.put(s.getName(), s));
        log.info("Loaded {} active scenarios into cache", activeScenarios.size());
    }
}

