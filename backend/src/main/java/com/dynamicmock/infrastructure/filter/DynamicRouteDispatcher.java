package com.dynamicmock.infrastructure.filter;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.application.service.ScenarioService;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.entity.Scenario.ScenarioState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Only handle /mock/** paths
        if (!path.startsWith("/mock")) {
            return true; // Continue to next interceptor
        }
        
        // Remove /mock prefix
        String mockPath = path.substring(5); // "/mock".length()
        if (mockPath.isEmpty()) {
            mockPath = "/";
        }
        
        // Get request body for matcher validation
        String requestBody = null;
        if (request instanceof CachedBodyHttpServletRequest) {
            requestBody = ((CachedBodyHttpServletRequest) request).getBodyAsString();
        }

        // Find matching route (considering path pattern and matchers)
        RouteRegistry.RouteMatch match = routeRegistry.findRoute(method, mockPath);
        if (match == null) {
            log.debug("No route found for {} {}", method, mockPath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("{\"error\":\"No mock route found\"}");
            } catch (IOException e) {
                log.error("Error writing response", e);
            }
            return false; // Stop processing
        }
        
        MockRoute route = match.getRoute();
        
        // Validate request against route matchers
        if (!requestMatcher.matches(request, route, requestBody)) {
            log.debug("Request does not match matchers for route {} {}", method, mockPath);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("{\"error\":\"Request does not match route matchers\"}");
            } catch (IOException e) {
                log.error("Error writing response", e);
            }
            return false;
        }
        
        log.debug("Matched route: {} {} -> {}", method, mockPath, route.getId());
        
        try {
            // Build script context
            ScriptContext scriptContext = buildScriptContext(request, match);
            
            // Check if route is linked to a scenario
            ScenarioState scenarioState = null;
            if (route.getScenarioName() != null && !route.getScenarioName().isEmpty()) {
                scenarioState = scenarioService.getCurrentStateObject(route.getScenarioName());
                if (scenarioState != null) {
                    log.debug("Using scenario '{}' state '{}'", route.getScenarioName(), scenarioState.getName());
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
                responseBody = templateEngine.render(responseTemplate, buildTemplateContext(scriptContext));
            }
            
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
                scenarioService.processTransition(route.getScenarioName(), transitionContext);
            }
            
            // Send response
            sendResponse(response, route, scriptContext, responseBody);
            
        } catch (Exception e) {
            log.error("Error processing mock route: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"error\":\"Internal server error: " + e.getMessage() + "\"}");
            } catch (IOException ex) {
                log.error("Error writing error response", ex);
            }
        }
        
        return false; // Stop processing, we've handled the request
    }
    
    private ScriptContext buildScriptContext(HttpServletRequest request, RouteRegistry.RouteMatch match) {
        // Extract headers
        Map<String, String> headers = Collections.list(request.getHeaderNames())
            .stream()
            .collect(Collectors.toMap(
                name -> name,
                request::getHeader
            ));
        
        // Extract query parameters
        Map<String, String> queryParams = request.getParameterMap().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()[0] // Take first value
            ));
        
        // Extract request body
        String body = null;
        try {
            if (request instanceof CachedBodyHttpServletRequest) {
                body = ((CachedBodyHttpServletRequest) request).getBodyAsString();
            } else if (request.getContentLength() > 0) {
                body = request.getReader().lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.warn("Error reading request body", e);
        }
        
        return ScriptContext.builder()
            .method(request.getMethod())
            .path(request.getRequestURI())
            .headers(headers)
            .queryParams(queryParams)
            .pathVariables(match.getPathVariables())
            .body(body)
            .bodyJson(parseJsonBody(body))
            .status(match.getRoute().getResponseStatus() != null ? match.getRoute().getResponseStatus() : 200)
            .responseHeaders(new HashMap<>(match.getRoute().getResponseHeaders() != null ? 
                match.getRoute().getResponseHeaders() : Map.of()))
            .variables(new HashMap<>())
            .state(new HashMap<>())
            .build();
    }
    
    private Map<String, Object> parseJsonBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            // Simple JSON parsing - in production, use Jackson ObjectMapper
            // For now, return null and let scripts handle it
            return null;
        } catch (Exception e) {
            log.debug("Could not parse body as JSON", e);
            return null;
        }
    }
    
    private Map<String, Object> buildTemplateContext(ScriptContext context) {
        Map<String, Object> templateContext = new HashMap<>();
        templateContext.put("request", Map.of(
            "method", context.getMethod() != null ? context.getMethod() : "",
            "path", context.getPath() != null ? context.getPath() : "",
            "headers", context.getHeaders() != null ? context.getHeaders() : Map.of(),
            "queryParams", context.getQueryParams() != null ? context.getQueryParams() : Map.of(),
            "pathVariables", context.getPathVariables() != null ? context.getPathVariables() : Map.of(),
            "body", context.getBody() != null ? context.getBody() : ""
        ));
        templateContext.put("response", Map.of(
            "status", context.getStatus() != null ? context.getStatus() : 200,
            "headers", context.getResponseHeaders() != null ? context.getResponseHeaders() : Map.of()
        ));
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
                // CRITICAL: Update variables and state from script execution
                context.setVariables(updatedContext.getVariables());
                context.setState(updatedContext.getState());
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
        
        // Set headers
        Map<String, String> headers = context.getResponseHeaders();
        if (headers != null) {
            headers.forEach(response::setHeader);
        }
        if (route.getResponseHeaders() != null) {
            route.getResponseHeaders().forEach(response::setHeader);
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
}

