package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.RedirectHop;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/test-client")
public class TestClientController {

    public static final int MAX_REDIRECTS = 5;

    private final RestTemplate restTemplate;
    private final com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService;

    @Value("${app.mock.base-url:http://localhost:8080}")
    private String mockBaseUrl = "http://localhost:8080";

    @Value("${mock.route.base-path:/mock}")
    private String mockBasePath = "/mock";

    @Autowired
    public TestClientController(com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService) {
        this.environmentService = environmentService;
        this.restTemplate = createNoRedirectRestTemplate();
    }

    public TestClientController(
            RestTemplate restTemplate,
            com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService) {
        this.restTemplate = restTemplate;
        this.environmentService = environmentService;
    }

    private static RestTemplate createNoRedirectRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        RestTemplate template = new RestTemplate(factory);
        template.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(URI url, HttpMethod method, ClientHttpResponse response) {
                // Allow 4xx/5xx mock stub responses to be inspected directly in TestClient
            }
        });
        return template;
    }

    @PostMapping("/execute")
    public ResponseEntity<TestResponse> execute(
            @RequestBody TestRequest request,
            @RequestHeader(value = "X-Dynamic-Mock-Env", required = false) String envId) {
        
        log.info("Executing test request: {} {} (Env: {})", request.getMethod(), request.getUrl(), envId);
        
        Map<String, String> envVars = new HashMap<>();
        if (envId != null && !envId.isEmpty() && environmentService != null) {
            try {
                envVars = environmentService.getEnvironment(envId).getVariables();
            } catch (Exception e) {
                log.warn("Could not load environment {}: {}", envId, e.getMessage());
            }
        }

        // Resolve variables in URL, Body and Headers
        String resolvedUrl = resolveVariables(request.getUrl(), envVars);
        String resolvedBody = resolveVariables(request.getBody(), envVars);
        
        long totalStartNano = System.nanoTime();
        List<RedirectHop> redirectChain = new ArrayList<>();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            if (request.getHeaders() != null) {
                final Map<String, String> finalEnvVars = envVars;
                request.getHeaders().forEach((k, v) -> headers.add(k, resolveVariables(v, finalEnvVars)));
            }

            String currentUrl = normalizeInternalMockUrl(resolvedUrl);
            HttpMethod currentMethod = HttpMethod.valueOf(
                    request.getMethod() != null ? request.getMethod().toUpperCase() : "GET"
            );
            String currentBody = resolvedBody;

            ResponseEntity<String> response = null;

            for (int hop = 1; hop <= MAX_REDIRECTS + 1; hop++) {
                long hopStartNano = System.nanoTime();
                HttpEntity<String> entity = new HttpEntity<>(currentBody, headers);

                response = restTemplate.exchange(
                        currentUrl,
                        currentMethod,
                        entity,
                        String.class
                );

                double hopDurationMs = computeDurationMs(hopStartNano);
                int statusCode = response.getStatusCode().value();
                String location = extractLocationHeader(response.getHeaders());

                if (isRedirectStatus(statusCode) && location != null && !location.isBlank() && hop <= MAX_REDIRECTS) {
                    redirectChain.add(RedirectHop.builder()
                            .step(hop)
                            .method(currentMethod.name())
                            .url(currentUrl)
                            .status(statusCode)
                            .location(location)
                            .durationMs(hopDurationMs)
                            .build());

                    currentUrl = normalizeInternalMockUrl(resolveRedirectUrl(currentUrl, location));
                    if (statusCode == 301 || statusCode == 302 || statusCode == 303) {
                        currentMethod = HttpMethod.GET;
                        currentBody = null;
                    }
                } else {
                    if (!redirectChain.isEmpty()) {
                        redirectChain.add(RedirectHop.builder()
                                .step(hop)
                                .method(currentMethod.name())
                                .url(currentUrl)
                                .status(statusCode)
                                .location(location)
                                .durationMs(hopDurationMs)
                                .build());
                    }
                    break;
                }
            }

            double totalDurationMs = computeDurationMs(totalStartNano);

            return ResponseEntity.ok(TestResponse.builder()
                .status(response.getStatusCode().value())
                .statusText(response.getStatusCode().toString())
                .headers(response.getHeaders() != null ? response.getHeaders().toSingleValueMap() : Collections.emptyMap())
                .body(response.getBody())
                .durationMs(totalDurationMs)
                .redirectChain(redirectChain)
                .build());
                
        } catch (Exception e) {
            double totalDurationMs = computeDurationMs(totalStartNano);
            log.error("Test execution failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(TestResponse.builder()
                    .status(500)
                    .statusText("Error: " + e.getMessage())
                    .durationMs(totalDurationMs)
                    .redirectChain(redirectChain)
                    .build());
        }
    }

    private static boolean isRedirectStatus(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String extractLocationHeader(HttpHeaders headers) {
        if (headers == null) return null;
        URI loc = headers.getLocation();
        if (loc != null) return loc.toString();
        for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
            if (entry.getKey() != null && "location".equalsIgnoreCase(entry.getKey())
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    private static String resolveRedirectUrl(String baseUrl, String location) {
        try {
            return URI.create(baseUrl).resolve(location).toString();
        } catch (Exception e) {
            return location;
        }
    }

    private String normalizeInternalMockUrl(String url) {
        if (url == null) return null;
        String base = mockBaseUrl != null && mockBaseUrl.endsWith("/")
                ? mockBaseUrl.substring(0, mockBaseUrl.length() - 1)
                : (mockBaseUrl != null ? mockBaseUrl : "http://localhost:8080");
        if (url.startsWith("http://localhost/mock/")) {
            return base + url.substring("http://localhost".length());
        }
        return url;
    }

    private static double computeDurationMs(long startNano) {
        double rawMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return Math.round(rawMs * 100.0) / 100.0;
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
        private double durationMs;
        private List<RedirectHop> redirectChain;
    }
}
