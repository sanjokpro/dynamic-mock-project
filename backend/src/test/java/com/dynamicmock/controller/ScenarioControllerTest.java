package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.ScenarioController;
import com.dynamicmock.adapter.in.web.dto.ScenarioRequest;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.Scenario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ScenarioControllerTest {

    @Mock
    private ScenarioService scenarioService;

    @InjectMocks
    private ScenarioController scenarioController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(scenarioController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllScenarios_shouldReturnScenarios() throws Exception {
        // Given
        List<Scenario> scenarios = Arrays.asList(
            createScenario("auth-flow", "Authentication Flow"),
            createScenario("payment-flow", "Payment Flow")
        );
        when(scenarioService.listScenarios()).thenReturn(scenarios);

        // When & Then
        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("auth-flow"))
                .andExpect(jsonPath("$[1].name").value("payment-flow"));

        verify(scenarioService).listScenarios();
    }

    @Test
    void getScenario_shouldReturnScenario() throws Exception {
        // Given
        Scenario scenario = createScenario("auth-flow", "Authentication Flow");
        when(scenarioService.getScenario("auth-flow")).thenReturn(scenario);
        when(scenarioService.getCurrentState("auth-flow")).thenReturn("unauthenticated");

        // When & Then
        mockMvc.perform(get("/api/scenarios/auth-flow"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("auth-flow"))
                .andExpect(jsonPath("$.description").value("Authentication Flow"));

        verify(scenarioService).getScenario("auth-flow");
    }

    @Test
    void createScenario_shouldCreateAndReturnScenario() throws Exception {
        // Given
        ScenarioRequest request = new ScenarioRequest();
        request.setName("auth-flow");
        request.setDescription("Authentication Flow");
        request.setInitialState("unauthenticated");

        Scenario createdScenario = createScenario("auth-flow", "Authentication Flow");
        when(scenarioService.createScenario(any(Scenario.class))).thenReturn(createdScenario);

        // When & Then
        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("auth-flow"))
                .andExpect(jsonPath("$.description").value("Authentication Flow"));

        verify(scenarioService).createScenario(any(Scenario.class));
    }

    @Test
    void updateScenario_shouldUpdateAndReturnScenario() throws Exception {
        // Given
        ScenarioRequest request = new ScenarioRequest();
        request.setDescription("Updated Authentication Flow");

        Scenario updatedScenario = createScenario("auth-flow", "Updated Authentication Flow");
        when(scenarioService.updateScenario(eq("auth-flow"), any(Scenario.class))).thenReturn(updatedScenario);

        // When & Then
        mockMvc.perform(put("/api/scenarios/auth-flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("auth-flow"))
                .andExpect(jsonPath("$.description").value("Updated Authentication Flow"));

        verify(scenarioService).updateScenario(eq("auth-flow"), any(Scenario.class));
    }

    @Test
    void deleteScenario_shouldDeleteScenario() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/scenarios/auth-flow"))
                .andExpect(status().isNoContent());

        verify(scenarioService).deleteScenario("auth-flow");
    }

    @Test
    void triggerTransition_shouldTriggerTransition() throws Exception {
        // Given
        Scenario scenario = createScenario("auth-flow", "Authentication Flow");
        when(scenarioService.getScenario("auth-flow")).thenReturn(scenario);
        when(scenarioService.getCurrentState("auth-flow")).thenReturn("unauthenticated");
        when(scenarioService.processTransition(eq("auth-flow"), any())).thenReturn("authenticated");

        // When & Then
        mockMvc.perform(post("/api/scenarios/auth-flow/transition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currentState").value("authenticated"))
                .andExpect(jsonPath("$.previousState").value("unauthenticated"));

        verify(scenarioService).processTransition(eq("auth-flow"), any());
    }

    @Test
    void resetScenario_shouldResetScenario() throws Exception {
        // Given
        Scenario resetScenario = createScenario("auth-flow", "Authentication Flow");
        resetScenario.setCurrentState("unauthenticated");
        when(scenarioService.resetScenario("auth-flow")).thenReturn(resetScenario);

        // When & Then
        mockMvc.perform(post("/api/scenarios/auth-flow/reset"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.currentState").value("unauthenticated"));

        verify(scenarioService).resetScenario("auth-flow");
    }

    private Scenario createScenario(String name, String description) {
        Scenario scenario = new Scenario();
        scenario.setId(name + "-id");
        scenario.setName(name);
        scenario.setDescription(description);
        scenario.setInitialState("unauthenticated");
        scenario.setCurrentState("unauthenticated");
        return scenario;
    }
}
