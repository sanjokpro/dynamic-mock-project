package com.dynamicmock.infrastructure.filter;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
import com.dynamicmock.domain.model.execution.ExecutionMode;
import com.dynamicmock.domain.model.execution.MockActionContract;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intercepts requests to /mock/** and routes them to registered mock endpoints
 * Executes pre-script → template rendering → post-script pipeline
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteDispatcher implements HandlerInterceptor {
    
    private final RouteRegistry routeRegistry;
    private final ScriptEngine scriptEngine;
    private final ResponseTemplateEngine templateEngine;
    private final RequestMatcher requestMatcher;
    private final ScenarioService scenarioService;
    private final ObjectMapper objectMapper;
    private final com.dynamicmock.application.service.TrafficLogger trafficLogger;
    private final com.dynamicmock.application.service.WorkspaceEnvironmentService environmentService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final com.dynamicmock.application.service.AsyncWebhookService asyncWebhookService;

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "transfer-encoding",
        "content-length",
        "connection",
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailers",
        "trailer",
        "upgrade"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startNano = System.nanoTime();
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Only handle /mock/** paths
        if (path == null || !path.startsWith("/mock")) {
            return true; // Continue to next interceptor
        }
        
        // Permissive CORS for /mock/** simulation endpoints (enables sandboxed Origin: null preview iframes)
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "*");
        
        // Remove /mock prefix
        String mockPath = path.substring(5); // "/mock".length()
        if (mockPath.isEmpty()) {
            mockPath = "/";
        }
        
        String rawQueryString = request.getQueryString();
        Map<String, String> reqHeaders = extractRequestHeaders(request);
        Map<String, String> reqQueryParams = extractQueryParams(request);
        
        // Get request body for matcher validation
        String requestBody = null;
        if (request instanceof CachedBodyHttpServletRequest) {
            requestBody = ((CachedBodyHttpServletRequest) request).getBodyAsString();
        }

        // Find matching routes (considering path pattern)
        java.util.List<RouteRegistry.RouteMatch> matches = routeRegistry.findRoutes(method, mockPath);
        if (matches.isEmpty()) {
            if ("OPTIONS".equalsIgnoreCase(method)) {
                response.setStatus(HttpServletResponse.SC_OK);
                return false;
            }
            log.debug("No route found for {} {}", method, mockPath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("{\"error\":\"No mock route found\"}");
            } catch (IOException e) {
                log.error("Error writing response", e);
            }
            return false; // Stop processing
        }
        
        // Postman routing headers
        String mockResponseName = request.getHeader("x-mock-response-name");
        String mockResponseCode = request.getHeader("x-mock-response-code");
        String mockMatchBody = request.getHeader("x-mock-match-request-body");

        // Rule 1: Explicit Postman Routing Header x-mock-response-name (Highest Override)
        RouteRegistry.RouteMatch match = null;
        if (mockResponseName != null && !mockResponseName.isBlank()) {
            String targetName = mockResponseName.trim();
            for (RouteRegistry.RouteMatch candidate : matches) {
                MockRoute r = candidate.getRoute();
                if (r.getMetadata() != null) {
                    String exName = r.getMetadata().get("exampleName");
                    String itName = r.getMetadata().get("itemName");
                    if ((exName != null && exName.equalsIgnoreCase(targetName))
                            || (itName != null && itName.equalsIgnoreCase(targetName))) {
                        match = candidate;
                        log.debug("Selected route {} via x-mock-response-name '{}'", r.getId(), targetName);
                        break;
                    }
                }
            }
            if (match == null) {
                log.debug("x-mock-response-name '{}' did not match any candidate route; falling back to matcher evaluation", targetName);
            }
        }

        if (match == null) {
            java.util.List<RouteRegistry.RouteMatch> candidates = new java.util.ArrayList<>(matches);

            // Filter by x-mock-response-code if supplied
            if (mockResponseCode != null && !mockResponseCode.isBlank()) {
                try {
                    int targetCode = Integer.parseInt(mockResponseCode.trim());
                    java.util.List<RouteRegistry.RouteMatch> codeMatches = candidates.stream()
                            .filter(c -> c.getRoute().getResponseStatus() != null && c.getRoute().getResponseStatus() == targetCode)
                            .toList();
                    if (!codeMatches.isEmpty()) {
                        candidates = new java.util.ArrayList<>(codeMatches);
                    } else {
                        log.debug("x-mock-response-code {} did not match any candidate; falling back to all candidates", targetCode);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // Deterministic sort: Priority DESC, Satisfied constraint count DESC, Matcher signature ASC, Route ID ASC
            candidates.sort((m1, m2) -> {
                MockRoute r1 = m1.getRoute();
                MockRoute r2 = m2.getRoute();

                // 1. Priority DESC
                int p1 = r1.getPriority() != null ? r1.getPriority() : 0;
                int p2 = r2.getPriority() != null ? r2.getPriority() : 0;
                int pCmp = Integer.compare(p2, p1);
                if (pCmp != 0) return pCmp;

                // 2. Number of satisfied exact constraints DESC
                int c1 = countRouteConstraints(r1);
                int c2 = countRouteConstraints(r2);
                int cCmp = Integer.compare(c2, c1);
                if (cCmp != 0) return cCmp;

                // 3. Stable normalized matcher signature ASC (lexicographical)
                String s1 = getNormalizedMatcherSignature(r1);
                String s2 = getNormalizedMatcherSignature(r2);
                int sCmp = s1.compareTo(s2);
                if (sCmp != 0) return sCmp;

                // 4. Stable tie-breaker: Route ID ASC
                String id1 = r1.getId() != null ? r1.getId() : "";
                String id2 = r2.getId() != null ? r2.getId() : "";
                return id1.compareTo(id2);
            });

            for (RouteRegistry.RouteMatch candidate : candidates) {
                if (requestMatcher.matches(request, candidate.getRoute(), requestBody)) {
                    match = candidate;
                    break;
                }
            }
        }
        
        if (match == null) {
            log.debug("Request does not match matchers for any route {} {}", method, mockPath);
            String noMatchBody = "{\"error\":\"Request does not match route matchers\"}";
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write(noMatchBody);
            } catch (IOException e) {
                log.error("Error writing response", e);
            }
            trafficLogger.log(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.builder()
                .protocol("HTTP")
                // Cannot log resourceId properly here if none matched, maybe use the first route's ID or none
                .resourceId(matches.get(0).getRoute().getId())
                .method(method)
                .path(mockPath)
                .queryString(rawQueryString)
                .queryParams(reqQueryParams)
                .requestHeaders(reqHeaders)
                .requestBody(requestBody)
                .status(404)
                .responseHeaders(Map.of("Content-Type", "application/json"))
                .responseBody(noMatchBody)
                .durationMs(computeDurationMs(startNano))
                .matchedRoute("No Match (Matchers)")
                .matchName("No Match (Matchers)")
                .build());
            return false;
        }
        
        MockRoute route = match.getRoute();
        String matchName = route.getMethod() + " " + route.getPath();
        
        log.debug("Matched route: {} {} -> {}", method, mockPath, route.getId());
        
        try {
            // Build script context
            ScriptContext scriptContext = buildScriptContext(request, match);
            
            // Check if route is linked to a scenario
            ScenarioState scenarioState = null;
            if (route.getScenarioName() != null && !route.getScenarioName().isEmpty()) {
                scenarioState = scenarioService.getCurrentStateObject(route.getScenarioName(), scriptContext.getSessionId());
                if (scenarioState != null) {
                    log.debug("Using scenario '{}' (sessionId: {}) state '{}'", 
                        route.getScenarioName(), scriptContext.getSessionId(), scenarioState.getName());
                }
            }
            
            // Determine which scripts and templates to use (scenario state overrides route)
            String preScript = scenarioState != null && scenarioState.getPreScript() != null 
                ? scenarioState.getPreScript() : route.getPreScript();
            String postScript = scenarioState != null && scenarioState.getPostScript() != null 
                ? scenarioState.getPostScript() : route.getPostScript();
            String scriptLanguage = scenarioState != null && scenarioState.getScriptLanguage() != null 
                ? scenarioState.getScriptLanguage() : route.getScriptLanguage();
            String responseTemplate = scenarioState != null && scenarioState.getResponseTemplate() != null 
                ? scenarioState.getResponseTemplate() : route.getResponseTemplate();
            Integer responseStatus = scenarioState != null && scenarioState.getResponseStatus() != null 
                ? scenarioState.getResponseStatus() : route.getResponseStatus();
            Integer delayMs = scenarioState != null && scenarioState.getDelayMs() != null 
                ? scenarioState.getDelayMs() : route.getDelayMs();
            Map<String, String> responseHeaders = scenarioState != null && scenarioState.getResponseHeaders() != null 
                ? scenarioState.getResponseHeaders() : route.getResponseHeaders();
            
            // Update context with scenario state's status if set
            if (responseStatus != null) {
                scriptContext.setStatus(responseStatus);
            }
            if (responseHeaders != null) {
                scriptContext.setResponseHeaders(new HashMap<>(responseHeaders));
            }
            
            // Execute pre-script
            if (preScript != null && !preScript.trim().isEmpty()) {
                executeScript(preScript, scriptLanguage, scriptContext);
            }
            
            // Apply delay if configured
            if (delayMs != null && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // Render response template
            String responseBody = "";
            if (responseTemplate != null && !responseTemplate.trim().isEmpty()) {
                // Resolve environment variables
                Map<String, String> envVars = new HashMap<>();
                String envId = request.getHeader("X-Dynamic-Mock-Env");
                if (envId != null && !envId.isEmpty()) {
                    try {
                        envVars = environmentService.getEnvironment(envId).getVariables();
                    } catch (Exception e) {
                        log.warn("Could not load environment {}: {}", envId, e.getMessage());
                    }
                } else {
                    // Try to find default environment
                    final Map<String, String> finalEnvVars = envVars;
                    environmentService.getAllEnvironments().stream()
                        .filter(e -> Boolean.TRUE.equals(e.getIsDefault()))
                        .findFirst()
                        .ifPresent(e -> {
                            Map<String, String> defaultVars = e.getVariables();
                            if (defaultVars != null) finalEnvVars.putAll(defaultVars);
                        });
                }

                Map<String, Object> templateContext = buildTemplateContext(scriptContext);
                templateContext.put("env", envVars);
                responseBody = templateEngine.render(responseTemplate, templateContext);
            }
            scriptContext.setResponseBody(responseBody);
            
            // Execute post-script (can modify responseBody)
            if (postScript != null && !postScript.trim().isEmpty()) {
                executeScript(postScript, scriptLanguage, scriptContext);
                // If post-script set response.body, use that instead
                if (scriptContext.getResponseBody() != null && !scriptContext.getResponseBody().isEmpty()) {
                    responseBody = scriptContext.getResponseBody();
                }
            }
            
            // Process scenario transition after response
            if (route.getScenarioName() != null && !route.getScenarioName().isEmpty()) {
                Map<String, Object> transitionContext = buildTransitionContext(scriptContext);
                scenarioService.processTransition(route.getScenarioName(), transitionContext, scriptContext.getSessionId());
            }
            
            String requestEventId = java.util.UUID.randomUUID().toString();
            Map<String, Object> webhookConfig = scriptContext.getWebhook();
            MockActionContract actionContract = MockActionContract.fromConfig(webhookConfig);
            boolean hasWebhook = webhookConfig != null && !webhookConfig.isEmpty();
            boolean isSyncWebhook = hasWebhook && actionContract.getExecutionMode() == ExecutionMode.SYNC;

            // Execute synchronous webhook if configured
            if (isSyncWebhook) {
                try {
                    com.dynamicmock.application.service.AsyncWebhookService.WebhookExecutionResult webhookResult =
                        asyncWebhookService.executeWebhookSync(route.getId(), requestEventId, webhookConfig);
                    if (actionContract.isReturnResponseToCaller() && webhookResult != null && webhookResult.getStatus() > 0) {
                        scriptContext.setStatus(webhookResult.getStatus());
                        if (webhookResult.getResponseHeaders() != null && !webhookResult.getResponseHeaders().isEmpty()) {
                            Map<String, String> cleanHeaders = new HashMap<>();
                            webhookResult.getResponseHeaders().forEach((k, v) -> {
                                if (k != null && !HOP_BY_HOP_HEADERS.contains(k.toLowerCase())) {
                                    cleanHeaders.put(k, v);
                                }
                            });
                            scriptContext.setResponseHeaders(cleanHeaders);
                        }
                        if (webhookResult.getResponseBody() != null) {
                            responseBody = webhookResult.getResponseBody();
                            scriptContext.setResponseBody(responseBody);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to execute synchronous webhook: {}", e.getMessage(), e);
                }
            }

            // Send response
            sendResponse(response, route, scriptContext, responseBody);

            int finalStatus = scriptContext.getStatus() != null ? scriptContext.getStatus() :
                (route.getResponseStatus() != null ? route.getResponseStatus() : 200);
            if (response.getStatus() > 0) {
                finalStatus = response.getStatus();
            }

            Map<String, String> finalResponseHeaders = resolveFinalResponseHeaders(route, scriptContext);

            // Trigger async webhook if configured and in ASYNC mode
            if (!isSyncWebhook && hasWebhook) {
                try {
                    asyncWebhookService.triggerWebhook(route.getId(), requestEventId, webhookConfig);
                } catch (Exception e) {
                    log.error("Failed to enqueue async webhook: {}", e.getMessage(), e);
                }
            }

            trafficLogger.log(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.builder()
                .id(requestEventId)
                .protocol("HTTP")
                .resourceId(route.getId())
                .method(method)
                .path(mockPath)
                .queryString(rawQueryString)
                .queryParams(reqQueryParams)
                .requestHeaders(reqHeaders)
                .requestBody(scriptContext.getBody())
                .status(finalStatus)
                .responseHeaders(finalResponseHeaders)
                .responseBody(responseBody)
                .durationMs(computeDurationMs(startNano))
                .matchedRoute(matchName)
                .matchName(matchName)
                .build());
            
        } catch (Exception e) {
            log.error("Error processing mock route: {}", e.getMessage(), e);
            String errorBody = "{\"error\":\"Internal server error: " + e.getMessage() + "\"}";
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write(errorBody);
            } catch (IOException ex) {
                log.error("Error writing error response", ex);
            }
            trafficLogger.log(com.dynamicmock.application.service.TrafficLogger.ExecutionEvent.builder()
                .protocol("HTTP")
                .resourceId(route.getId())
                .method(method)
                .path(mockPath)
                .queryString(rawQueryString)
                .queryParams(reqQueryParams)
                .requestHeaders(reqHeaders)
                .requestBody(requestBody)
                .status(500)
                .responseHeaders(Map.of("Content-Type", "application/json"))
                .responseBody(errorBody)
                .durationMs(computeDurationMs(startNano))
                .matchedRoute(matchName + " (Error)")
                .matchName(matchName + " (Error)")
                .build());
        }
        
        return false; // Stop processing, we've handled the request
    }

    private double computeDurationMs(long startNano) {
        double rawMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return Math.round(rawMs * 100.0) / 100.0;
    }

    private Map<String, String> extractRequestHeaders(HttpServletRequest request) {
        if (request.getHeaderNames() == null) {
            return Collections.emptyMap();
        }
        return Collections.list(request.getHeaderNames())
            .stream()
            .collect(Collectors.toMap(
                name -> name,
                name -> request.getHeader(name) != null ? request.getHeader(name) : "",
                (v1, v2) -> v2
            ));
    }

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        if (request.getParameterMap() == null) {
            return Collections.emptyMap();
        }
        return request.getParameterMap().entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().length > 0)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()[0],
                (v1, v2) -> v2
            ));
    }

    private Map<String, String> resolveFinalResponseHeaders(MockRoute route, ScriptContext context) {
        Map<String, String> merged = new HashMap<>();
        if (route.getResponseHeaders() != null) {
            merged.putAll(route.getResponseHeaders());
        }
        if (context.getResponseHeaders() != null) {
            merged.putAll(context.getResponseHeaders());
        }
        boolean hasContentType = merged.keySet().stream().anyMatch(k -> "content-type".equalsIgnoreCase(k));
        if (!hasContentType) {
            merged.put("Content-Type", "application/json");
        }
        return merged;
    }
    
    private ScriptContext buildScriptContext(HttpServletRequest request, RouteRegistry.RouteMatch match) {
        // Extract headers
        Map<String, String> headers = extractRequestHeaders(request);
        
        // Extract query parameters
        Map<String, String> queryParams = extractQueryParams(request);
        
        // Extract request body
        String body = null;
        try {
            if (request instanceof CachedBodyHttpServletRequest) {
                body = ((CachedBodyHttpServletRequest) request).getBodyAsString();
            } else if (request.getContentLength() > 0 && request.getReader() != null) {
                body = request.getReader().lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.warn("Error reading request body", e);
        }
        
        String sessionId = request.getHeader("X-Session-ID");
        if (sessionId != null) {
            sessionId = sessionId.trim();
        }
        String envId = request.getHeader("X-Dynamic-Mock-Env");
        String stateKey = "dynamic-mock:script-state:global";
        if (sessionId != null && !sessionId.isEmpty()) {
            stateKey = "dynamic-mock:script-state:session:" + sessionId;
        } else if (envId != null && !envId.isEmpty()) {
            stateKey = "dynamic-mock:script-state:env:" + envId;
        }

        Map<String, String> formData = extractFormData(request, body);

        return ScriptContext.builder()
            .method(request.getMethod())
            .path(request.getRequestURI())
            .headers(headers)
            .queryParams(queryParams)
            .pathVariables(match.getPathVariables())
            .formData(formData)
            .formParams(formData)
            .body(body)
            .bodyJson(parseJsonBody(body))
            .status(match.getRoute().getResponseStatus() != null ? match.getRoute().getResponseStatus() : 200)
            .responseHeaders(new HashMap<>(match.getRoute().getResponseHeaders() != null ? 
                match.getRoute().getResponseHeaders() : Map.of()))
            .variables(new HashMap<>())
            .state(new com.dynamicmock.adapter.out.script.RedisBackedStateMap(stateKey, redisTemplate))
            .sessionId(sessionId)
            .build();
    }

    private Map<String, String> extractFormData(HttpServletRequest request, String body) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("application/x-www-form-urlencoded")) {
            Map<String, String> parsed = RequestMatcher.parseFormUrlEncodedBody(body);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return RequestMatcher.parseFormUrlEncodedBody(body);
    }
    
    private Map<String, Object> parseJsonBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            // Use Jackson to parse the body if it looks like JSON
            if (body.trim().startsWith("{") || body.trim().startsWith("[")) {
                return (Map<String, Object>) objectMapper.readValue(body, Map.class);
            }
            return null;
        } catch (Exception e) {
            log.debug("Could not parse body as JSON: {}", e.getMessage());
            return null;
        }
    }
    
    private Map<String, Object> buildTemplateContext(ScriptContext context) {
        Map<String, Object> templateContext = new HashMap<>();
        
        // Add request details
        Map<String, String> resolvedFormData = context.getFormData() != null
            ? context.getFormData()
            : (context.getFormParams() != null ? context.getFormParams() : Map.of());
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("method", context.getMethod() != null ? context.getMethod() : "");
        requestMap.put("path", context.getPath() != null ? context.getPath() : "");
        requestMap.put("headers", context.getHeaders() != null ? context.getHeaders() : Map.of());
        requestMap.put("queryParams", context.getQueryParams() != null ? context.getQueryParams() : Map.of());
        requestMap.put("pathVariables", context.getPathVariables() != null ? context.getPathVariables() : Map.of());
        requestMap.put("formData", resolvedFormData);
        requestMap.put("formParams", resolvedFormData);
        requestMap.put("body", context.getBody() != null ? context.getBody() : "");
        requestMap.put("bodyJson", context.getBodyJson() != null ? context.getBodyJson() : Map.of());
        templateContext.put("request", requestMap);
        
        // Add response details
        templateContext.put("response", Map.of(
            "status", context.getStatus() != null ? context.getStatus() : 200,
            "headers", context.getResponseHeaders() != null ? context.getResponseHeaders() : Map.of()
        ));
        
        // Flatten path variables and query params into root for easy access
        if (context.getPathVariables() != null) {
            templateContext.putAll(context.getPathVariables());
        }
        if (context.getQueryParams() != null) {
            templateContext.putAll(context.getQueryParams());
        }
        
        templateContext.put("vars", context.getVariables() != null ? context.getVariables() : Map.of());
        templateContext.put("state", context.getState() != null ? context.getState() : Map.of());
        return templateContext;
    }
    
    private Map<String, Object> buildTransitionContext(ScriptContext context) {
        Map<String, Object> transitionContext = new HashMap<>();
        transitionContext.put("request", Map.of(
            "method", context.getMethod() != null ? context.getMethod() : "",
            "path", context.getPath() != null ? context.getPath() : "",
            "headers", context.getHeaders() != null ? context.getHeaders() : Map.of(),
            "queryParams", context.getQueryParams() != null ? context.getQueryParams() : Map.of(),
            "pathVariables", context.getPathVariables() != null ? context.getPathVariables() : Map.of()
        ));
        transitionContext.put("response", Map.of(
            "status", context.getStatus() != null ? context.getStatus() : 200
        ));
        transitionContext.put("vars", context.getVariables() != null ? context.getVariables() : Map.of());
        transitionContext.put("state", context.getState() != null ? context.getState() : Map.of());
        return transitionContext;
    }
    
    private void executeScript(String script, String language, ScriptContext context) {
        try {
            ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, language, context);
            // Update context with script modifications
            if (result.getContext() != null) {
                ScriptContext updatedContext = result.getContext();
                context.setStatus(updatedContext.getStatus());
                context.setResponseHeaders(updatedContext.getResponseHeaders());
                context.setResponseBody(updatedContext.getResponseBody());
                // CRITICAL: Update variables, state and webhook from script execution
                context.setVariables(updatedContext.getVariables());
                context.setState(updatedContext.getState());
                context.setWebhook(updatedContext.getWebhook());
                log.debug("Updated context after script execution: vars={}, status={}", 
                    context.getVariables(), context.getStatus());
            }
        } catch (ScriptEngine.ScriptExecutionException e) {
            log.error("Script execution failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    
    private void sendResponse(HttpServletResponse response, MockRoute route, 
                              ScriptContext context, String responseBody) throws IOException {
        // Set status
        int status = context.getStatus() != null ? context.getStatus() : 
                    (route.getResponseStatus() != null ? route.getResponseStatus() : 200);
        response.setStatus(status);
        
        // Set headers: route static headers first, then dynamic context headers override
        if (route.getResponseHeaders() != null) {
            route.getResponseHeaders().forEach((k, v) -> {
                if (k != null && !HOP_BY_HOP_HEADERS.contains(k.toLowerCase())) {
                    response.setHeader(k, v);
                }
            });
        }
        Map<String, String> headers = context.getResponseHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && !HOP_BY_HOP_HEADERS.contains(k.toLowerCase())) {
                    response.setHeader(k, v);
                }
            });
        }
        
        // Set content type if not set
        if (response.getContentType() == null) {
            response.setContentType("application/json");
        }
        
        // Write body - prioritize responseBody (rendered template), then context.getResponseBody() (from post-script)
        String finalBody = responseBody;
        if (finalBody == null || finalBody.isEmpty()) {
            finalBody = context.getResponseBody();
        }
        
        if (finalBody != null && !finalBody.isEmpty()) {
            log.debug("Sending response body: {}", finalBody);
            response.getWriter().write(finalBody);
            response.getWriter().flush();
        } else {
            log.warn("No response body to send");
        }
    }

    private int countRouteConstraints(MockRoute route) {
        if (route == null || route.getMatchers() == null) return 0;
        int count = 0;
        Object qp = route.getMatchers().get("queryParams");
        if (qp instanceof Map<?, ?> qm) {
            count += qm.size();
        }
        Object hdrs = route.getMatchers().get("headers");
        if (hdrs instanceof Map<?, ?> hm) {
            count += hm.size();
        }
        Object body = route.getMatchers().get("body");
        if (body != null) {
            count += 1;
        }
        return count;
    }

    private String getNormalizedMatcherSignature(MockRoute route) {
        if (route == null || route.getMatchers() == null) return "";
        StringBuilder sb = new StringBuilder();
        Object qp = route.getMatchers().get("queryParams");
        if (qp instanceof Map<?, ?> qm) {
            java.util.List<String> sorted = qm.entrySet().stream()
                    .map(e -> String.valueOf(e.getKey()) + "=" + String.valueOf(e.getValue()))
                    .sorted()
                    .toList();
            sb.append("q=").append(String.join("&", sorted));
        }
        Object hdrs = route.getMatchers().get("headers");
        if (hdrs instanceof Map<?, ?> hm) {
            java.util.List<String> sorted = hm.entrySet().stream()
                    .map(e -> String.valueOf(e.getKey()).toLowerCase() + ":" + String.valueOf(e.getValue()))
                    .sorted()
                    .toList();
            sb.append("&h=").append(String.join("|", sorted));
        }
        return sb.toString();
    }

    private boolean isBlockingWebhook(Map<String, Object> webhookConfig) {
        return MockActionContract.fromConfig(webhookConfig).getExecutionMode() == ExecutionMode.SYNC;
    }
}
