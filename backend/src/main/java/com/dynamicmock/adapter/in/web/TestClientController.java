package com.dynamicmock.adapter.in.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test-client")
@RequiredArgsConstructor
public class TestClientController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService;

    @org.springframework.beans.factory.annotation.Value("${app.mock.base-url:http://localhost:8080}")
    private String mockBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${mock.route.base-path:/mock}")
    private String mockBasePath;

    @PostMapping("/execute")
    public ResponseEntity<TestResponse> execute(
            @RequestBody TestRequest request,
            @RequestHeader(value = "X-Dynamic-Mock-Env", required = false) String envId) {
        
        log.info("Executing test request: {} {} (Env: {})", request.getMethod(), request.getUrl(), envId);
        
        Map<String, String> envVars = new HashMap<>();
        if (envId != null && !envId.isEmpty()) {
            try {
                envVars = environmentService.getEnvironment(envId).getVariables();
            } catch (Exception e) {
                log.warn("Could not load environment {}: {}", envId, e.getMessage());
            }
        }

        // Resolve variables in URL, Body and Headers
        String resolvedUrl = resolveVariables(request.getUrl(), envVars);
        String resolvedBody = resolveVariables(request.getBody(), envVars);
        
        long startTime = System.currentTimeMillis();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                final Map<String, String> finalEnvVars = envVars;
                request.getHeaders().forEach((k, v) -> headers.add(k, resolveVariables(v, finalEnvVars)));
            }

            HttpEntity<String> entity = new HttpEntity<>(resolvedBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                resolvedUrl,
                HttpMethod.valueOf(request.getMethod()),
                entity,
                String.class
            );

            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(TestResponse.builder()
                .status(response.getStatusCode().value())
                .statusText(response.getStatusCode().toString())
                .headers(response.getHeaders().toSingleValueMap())
                .body(response.getBody())
                .durationMs(duration)
                .build());
                
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Test execution failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TestResponse.builder()
                    .status(500)
                    .statusText("Error: " + e.getMessage())
                    .durationMs(duration)
                    .build());
        }
    }

    private String resolveVariables(String input, Map<String, String> variables) {
        if (input == null) return null;
        String result = input;
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        // Fallback for unresolved BASE_URL
        if (result.contains("{{BASE_URL}}")) {
            String base = (mockBaseUrl.endsWith("/") ? mockBaseUrl.substring(0, mockBaseUrl.length() - 1) : mockBaseUrl)
                    + (mockBasePath.startsWith("/") ? mockBasePath : "/" + mockBasePath);
            result = result.replace("{{BASE_URL}}", base);
        }
        return result;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestRequest {
        private String url;
        private String method;
        private Map<String, String> headers;
        private String body;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestResponse {
        private int status;
        private String statusText;
        private Map<String, String> headers;
        private String body;
        private long durationMs;
    }
}
