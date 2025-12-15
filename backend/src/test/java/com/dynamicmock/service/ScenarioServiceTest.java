package com.dynamicmock.service;

import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
import com.dynamicmock.domain.entity.Scenario.StateTransition;
import com.dynamicmock.domain.port.out.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {

    @Mock
    private ScenarioRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private ScenarioService scenarioService;

    private Scenario testScenario;

    @BeforeEach
    void setUp() {
        // Build test states with transitions
        StateTransition transition = new StateTransition();
        transition.setTargetState("state2");
        transition.setCondition("always");
        transition.setPriority(1);

        ScenarioState state1 = new ScenarioState();
        state1.setName("state1");
        state1.setTransitions(Arrays.asList(transition));

        ScenarioState state2 = new ScenarioState();
        state2.setName("state2");
        state2.setTransitions(Collections.emptyList());

        testScenario = Scenario.builder()
                .id("test-id")
                .name("Test Scenario")
                .description("Test Description")
                .initialState("state1")
                .currentState("state1")
                .states(Arrays.asList(state1, state2))
                .active(false)
                .autoReset(true)
                .executionCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createScenario_shouldCreateNewScenario() {
        // Given
        when(repository.existsByName("Test Scenario")).thenReturn(false);
        when(repository.save(any(Scenario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Scenario result = scenarioService.createScenario(testScenario);

        // Then
        assertNotNull(result);
        assertEquals("Test Scenario", result.getName());
        assertEquals("state1", result.getCurrentState());
        verify(repository).existsByName("Test Scenario");
        verify(repository).save(any(Scenario.class));
    }

    @Test
    void createScenario_shouldThrowWhenNameExists() {
        // Given
        when(repository.existsByName("Test Scenario")).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> scenarioService.createScenario(testScenario));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void createScenario_shouldThrowWhenNameIsEmpty() {
        // Given
        testScenario.setName("");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> scenarioService.createScenario(testScenario));
    }

    @Test
    void createScenario_shouldThrowWhenInitialStateIsEmpty() {
        // Given
        when(repository.existsByName("Test Scenario")).thenReturn(false);
        testScenario.setInitialState("");

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> scenarioService.createScenario(testScenario));
    }

    @Test
    void createScenario_shouldThrowWhenStatesIsEmpty() {
        // Given
        when(repository.existsByName("Test Scenario")).thenReturn(false);
        testScenario.setStates(Collections.emptyList());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> scenarioService.createScenario(testScenario));
    }

    @Test
    void updateScenario_shouldUpdateExistingScenario() {
        // Given
        Scenario updates = Scenario.builder()
                .name("Updated Name")
                .description("Updated Description")
                .build();

        when(repository.findById("test-id")).thenReturn(Optional.of(testScenario));
        when(repository.existsByName("Updated Name")).thenReturn(false);
        when(repository.save(any(Scenario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Scenario result = scenarioService.updateScenario("test-id", updates);

        // Then
        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("Updated Description", result.getDescription());
        verify(repository).findById("test-id");
        verify(repository).save(any(Scenario.class));
    }

    @Test
    void updateScenario_shouldThrowWhenNotFound() {
        // Given
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> scenarioService.updateScenario("unknown", testScenario));
    }

    @Test
    void getScenario_shouldReturnScenario() {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testScenario));

        // When
        Scenario result = scenarioService.getScenario("test-id");

        // Then
        assertEquals(testScenario.getId(), result.getId());
        verify(repository).findById("test-id");
    }

    @Test
    void getScenario_shouldThrowWhenNotFound() {
        // Given
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> scenarioService.getScenario("unknown"));
    }

    @Test
    void getScenarioByName_shouldReturnScenario() {
        // Given
        when(repository.findByName("Test Scenario")).thenReturn(Optional.of(testScenario));

        // When
        Optional<Scenario> result = scenarioService.getScenarioByName("Test Scenario");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testScenario.getId(), result.get().getId());
    }

    @Test
    void listScenarios_shouldReturnAllScenarios() {
        // Given
        when(repository.findAll()).thenReturn(Arrays.asList(testScenario));

        // When
        List<Scenario> result = scenarioService.listScenarios();

        // Then
        assertEquals(1, result.size());
        verify(repository).findAll();
    }

    @Test
    void deleteScenario_shouldDeleteScenario() {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testScenario));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        scenarioService.deleteScenario("test-id");

        // Then
        verify(repository).deleteById("test-id");
    }

    @Test
    void activateScenario_shouldActivateScenario() {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testScenario));
        when(repository.save(any(Scenario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Scenario result = scenarioService.activateScenario("test-id");

        // Then
        assertTrue(result.getActive());
        verify(repository).save(any(Scenario.class));
    }

    @Test
    void deactivateScenario_shouldDeactivateScenario() {
        // Given
        testScenario.setActive(true);
        when(repository.findById("test-id")).thenReturn(Optional.of(testScenario));
        when(repository.save(any(Scenario.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        Scenario result = scenarioService.deactivateScenario("test-id");

        // Then
        assertFalse(result.getActive());
        verify(repository).save(any(Scenario.class));
    }

    @Test
    void resetScenario_shouldResetToInitialState() {
        // Given
        testScenario.setCurrentState("state2");
        when(repository.findById("test-id")).thenReturn(Optional.of(testScenario));
        when(repository.save(any(Scenario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // When
        Scenario result = scenarioService.resetScenario("test-id");

        // Then
        assertEquals("state1", result.getCurrentState());
        assertEquals(0, result.getExecutionCount());
        verify(repository).save(any(Scenario.class));
    }

    @Test
    void getCurrentState_shouldReturnStateFromRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("scenario:state:Test Scenario")).thenReturn("state2");

        // When
        String result = scenarioService.getCurrentState("Test Scenario");

        // Then
        assertEquals("state2", result);
    }

    @Test
    void getCurrentState_shouldFallbackToScenarioWhenNotInRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("scenario:state:Test Scenario")).thenReturn(null);
        when(repository.findByName("Test Scenario")).thenReturn(Optional.of(testScenario));

        // When
        String result = scenarioService.getCurrentState("Test Scenario");

        // Then
        assertEquals("state1", result);
    }

    @Test
    void loadActiveScenarios_shouldLoadAllActiveScenarios() {
        // Given
        testScenario.setActive(true);
        when(repository.findByActiveTrue()).thenReturn(Arrays.asList(testScenario));

        // When
        scenarioService.loadActiveScenarios();

        // Then
        verify(repository).findByActiveTrue();
    }
}

