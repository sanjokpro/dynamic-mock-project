package com.dynamicmock.adapter.out.template;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Template engine for rendering response templates using Handlebars
 * with built-in random data functions and script support
 */
@Slf4j
@Component
public class ResponseTemplateEngine {
    
    private final ScriptEngine scriptEngine;
    private final ObjectMapper objectMapper;
    private final Handlebars handlebars;
    
    public ResponseTemplateEngine(ScriptEngine scriptEngine, ObjectMapper objectMapper) {
        this.scriptEngine = scriptEngine;
        this.objectMapper = objectMapper;
        this.handlebars = new Handlebars();
        registerHelpers();
    }
    
    @SuppressWarnings("unchecked")
    private ScriptContext reconstructScriptContext(Map<String, Object> contextMap) {
        if (contextMap == null) return ScriptContext.builder().build();
        
        Object requestObj = contextMap.get("request");
        Map<String, Object> request = (requestObj instanceof Map) ? (Map<String, Object>) requestObj : Map.of();
        
        Object responseObj = contextMap.get("response");
        Map<String, Object> response = (responseObj instanceof Map) ? (Map<String, Object>) responseObj : Map.of();
        
        Object varsObj = contextMap.get("vars");
        Map<String, Object> vars = (varsObj instanceof Map) ? (Map<String, Object>) varsObj : Map.of();
        
        Object stateObj = contextMap.get("state");
        Map<String, Object> state = (stateObj instanceof Map) ? (Map<String, Object>) stateObj : Map.of();
        
        return ScriptContext.builder()
            .method(request.get("method") != null ? request.get("method").toString() : null)
            .path(request.get("path") != null ? request.get("path").toString() : null)
            .headers((Map<String, String>) request.getOrDefault("headers", Map.of()))
            .queryParams((Map<String, String>) request.getOrDefault("queryParams", Map.of()))
            .pathVariables((Map<String, String>) request.getOrDefault("pathVariables", Map.of()))
            .body(request.get("body") != null ? request.get("body").toString() : null)
            .bodyJson((Map<String, Object>) request.getOrDefault("bodyJson", Map.of()))
            .status(response.get("status") instanceof Integer ? (Integer) response.get("status") : 200)
            .responseHeaders(new HashMap<>((Map<String, String>) response.getOrDefault("headers", Map.of())))
            .variables(new HashMap<>(vars))
            .state(new HashMap<>(state))
            .build();
    }
    
    private void registerHelpers() {
        // Register script helper
        handlebars.registerHelper("script", (context, options) -> {
            String script = options.fn().toString();
            String language = options.hash("language", "js").toString();
            
            // Reconstruct ScriptContext from template context if possible
            Map<String, Object> contextMap = new HashMap<>();
            if (options.context.model() instanceof Map) {
                contextMap = (Map<String, Object>) options.context.model();
            }
            
            ScriptContext scriptContext = reconstructScriptContext(contextMap);
            
            try {
                ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, language, scriptContext);
                Object val = result.getReturnValue();
                
                if (val == null) return "";
                
                if (val instanceof String) return val.toString();
                
                // For objects, return as JSON
                return objectMapper.writeValueAsString(val);
            } catch (Exception e) {
                log.error("Error executing embedded script", e);
                return "Error (" + e.getClass().getSimpleName() + "): " + e.getMessage();
            }
        });

        // Register random data functions as Handlebars helpers
        handlebars.registerHelper("$randomInt", (context, options) -> {
            if (options.params.length == 0) {
                return RandomDataFunctions.randomInt(100);
            } else if (options.params.length == 1) {
                return RandomDataFunctions.randomInt((Integer) options.params[0]);
            } else {
                return RandomDataFunctions.randomInt(
                    (Integer) options.params[0],
                    (Integer) options.params[1]
                );
            }
        });
        
        handlebars.registerHelper("$randomBool", (context, options) -> 
            RandomDataFunctions.randomBool()
        );
        
        handlebars.registerHelper("$randomString", (context, options) -> {
            if (options.params.length == 0) {
                return RandomDataFunctions.randomString();
            } else {
                return RandomDataFunctions.randomString((Integer) options.params[0]);
            }
        });
        
        handlebars.registerHelper("$timestamp", (context, options) -> 
            RandomDataFunctions.timestamp()
        );
        
        handlebars.registerHelper("$randomUUID", (context, options) -> 
            RandomDataFunctions.randomUUID()
        );
        
        handlebars.registerHelper("$randomEmail", (context, options) -> 
            RandomDataFunctions.randomEmail()
        );
        
        handlebars.registerHelper("$randomFloat", (context, options) -> {
            if (options.params.length == 0) {
                return RandomDataFunctions.randomFloat();
            } else {
                return RandomDataFunctions.randomFloat(
                    ((Number) options.params[0]).doubleValue(),
                    ((Number) options.params[1]).doubleValue()
                );
            }
        });
        
        // Register conditional helpers
        handlebars.registerHelpers(ConditionalHelpers.class);
    }
    
    /**
     * Render a template with the given context
     */
    public String render(String template, Map<String, Object> context) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        
        try {
            Template compiled = handlebars.compileInline(template);
            return compiled.apply(context);
        } catch (IOException e) {
            log.error("Error rendering template: {}", e.getMessage(), e);
            throw new TemplateRenderException("Failed to render template: " + e.getMessage(), e);
        }
    }
    
    /**
     * Render a template with empty context
     */
    public String render(String template) {
        return render(template, Map.of());
    }
    
    public static class TemplateRenderException extends RuntimeException {
        public TemplateRenderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

