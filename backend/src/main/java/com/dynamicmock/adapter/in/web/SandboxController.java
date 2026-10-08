package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.Iso8583EndpointRequest;
import com.dynamicmock.adapter.in.web.dto.ScenarioRequest;
import com.dynamicmock.application.service.Iso8583Service;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.entity.Scenario;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sandbox")
@RequiredArgsConstructor
public class SandboxController {

    private final Iso8583Service iso8583Service;
    private final ScenarioService scenarioService;
    private final ObjectMapper objectMapper;

    @Data
    public static class SandboxBundle {
        private ScenarioRequest scenario;
        private Iso8583EndpointRequest endpoint;
    }

    @PostMapping("/load")
    public ResponseEntity<Map<String, String>> loadSandbox() {
        try {
            ClassPathResource resource = new ClassPathResource("sandbox/banking-sandbox.json");
            try (InputStream is = resource.getInputStream()) {
                SandboxBundle bundle = objectMapper.readValue(is, SandboxBundle.class);

                // Load Scenario (Idempotent: check by name)
                boolean scenarioCreated = false;
                Optional<Scenario> existingScenario = scenarioService.listScenarios().stream()
                        .filter(s -> s.getName().equals(bundle.getScenario().getName()))
                        .findFirst();
                
                if (existingScenario.isEmpty()) {
                    ScenarioRequest sReq = bundle.getScenario();
                    Scenario scenario = Scenario.builder()
                            .name(sReq.getName())
                            .description(sReq.getDescription())
                            .initialState(sReq.getInitialState())
                            .states(sReq.getStates())
                            .maxExecutions(sReq.getMaxExecutions())
                            .autoReset(sReq.getAutoReset())
                            .active(sReq.getActive())
                            .build();
                    scenarioService.createScenario(scenario);
                    scenarioCreated = true;
                }

                // Load ISO8583 Endpoint (Idempotent: check by port)
                boolean endpointCreated = false;
                Optional<Iso8583Endpoint> existingEndpoint = iso8583Service.findAll().stream()
                        .filter(e -> e.getPort().equals(bundle.getEndpoint().getPort()))
                        .findFirst();
                
                if (existingEndpoint.isEmpty()) {
                    iso8583Service.create(bundle.getEndpoint());
                    endpointCreated = true;
                }

                String message = "Sandbox loaded successfully. " +
                        (scenarioCreated ? "Created scenario. " : "Scenario already exists. ") +
                        (endpointCreated ? "Created endpoint." : "Endpoint already exists.");

                return ResponseEntity.ok(Map.of("status", "success", "message", message));
            }
        } catch (Exception e) {
            log.error("Failed to load sandbox", e);
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
