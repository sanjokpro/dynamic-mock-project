package com.dynamicmock.application.service;

import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
import com.dynamicmock.domain.entity.Scenario.StateTransition;
import com.dynamicmock.domain.port.out.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceNamespacingTest {

    @Mock
    private ScenarioRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ScenarioService scenarioService;
    private Scenario cardPaymentFlow;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        scenarioService = new ScenarioService(repository, redisTemplate);

        // Build sample scenario matching Banking Sandbox
        cardPaymentFlow = Scenario.builder()
                .id("sc-1")
                .name("Card Payment Flow")
                .initialState("INITIAL")
                .active(true)
                .states(List.of(
                        ScenarioState.builder()
                                .name("INITIAL")
                                .transitions(List.of(
                                        StateTransition.builder()
                                                .targetState("AUTHORIZED")
                                                .condition("request.mti == '0100'")
                                                .build()
                                ))
                                .build(),
                        ScenarioState.builder()
                                .name("AUTHORIZED")
                                .transitions(List.of(
                                        StateTransition.builder()
                                                .targetState("CAPTURED")
                                                .condition("request.mti == '0220'")
                                                .build()
                                ))
                                .build(),
                        ScenarioState.builder().name("CAPTURED").build()
                ))
                .build();
    }

    @Test
    @DisplayName("Should use namespaced Redis key when session ID is provided")
    void testSessionNamespacedStateKey() {
        when(valueOperations.get("scenario:state:Card Payment Flow:session-alpha"))
                .thenReturn("AUTHORIZED");

        String state = scenarioService.getCurrentState("Card Payment Flow", "session-alpha");

        assertThat(state).isEqualTo("AUTHORIZED");
        verify(valueOperations).get("scenario:state:Card Payment Flow:session-alpha");
    }

    @Test
    @DisplayName("Should use global Redis key when session ID is null")
    void testGlobalStateKeyFallback() {
        when(valueOperations.get("scenario:state:Card Payment Flow"))
                .thenReturn("INITIAL");

        String state = scenarioService.getCurrentState("Card Payment Flow");

        assertThat(state).isEqualTo("INITIAL");
        verify(valueOperations).get("scenario:state:Card Payment Flow");
    }

    @Test
    @DisplayName("Two concurrent sessions maintain separate state transitions on same scenario")
    void testConcurrentSessions_IsolatedStates() {
        // Mock session-alpha at INITIAL
        when(valueOperations.get("scenario:state:Card Payment Flow:session-alpha"))
                .thenReturn("INITIAL");
        // Mock session-beta at AUTHORIZED
        when(valueOperations.get("scenario:state:Card Payment Flow:session-beta"))
                .thenReturn("AUTHORIZED");

        when(repository.findByName("Card Payment Flow")).thenReturn(Optional.of(cardPaymentFlow));

        // Session Alpha processes 0100 -> moves to AUTHORIZED
        Map<String, Object> alphaContext = Map.of("request", Map.of("mti", "0100"));
        String alphaNext = scenarioService.processTransition("Card Payment Flow", alphaContext, "session-alpha");
        assertThat(alphaNext).isEqualTo("AUTHORIZED");
        verify(valueOperations).set(eq("scenario:state:Card Payment Flow:session-alpha"), eq("AUTHORIZED"), any());

        // Session Beta processes 0220 -> moves to CAPTURED
        Map<String, Object> betaContext = Map.of("request", Map.of("mti", "0220"));
        String betaNext = scenarioService.processTransition("Card Payment Flow", betaContext, "session-beta");
        assertThat(betaNext).isEqualTo("CAPTURED");
        verify(valueOperations).set(eq("scenario:state:Card Payment Flow:session-beta"), eq("CAPTURED"), any());
    }

    @Test
    @DisplayName("Execution counters increment independently per session")
    void testExecutionCounters_IndependentPerSession() {
        scenarioService.setCurrentState("Card Payment Flow", "INITIAL", "session-1");
        verify(valueOperations).set(eq("scenario:state:Card Payment Flow:session-1"), eq("INITIAL"), any());

        when(valueOperations.get("scenario:exec:Card Payment Flow:session-1")).thenReturn("5");
        when(valueOperations.get("scenario:exec:Card Payment Flow:session-2")).thenReturn("1");

        assertThat(scenarioService.getExecutionCount("Card Payment Flow", "session-1")).isEqualTo(5L);
        assertThat(scenarioService.getExecutionCount("Card Payment Flow", "session-2")).isEqualTo(1L);
    }
}
