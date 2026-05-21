package com.dynamicmock.adapter.out.script;

import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.ArrayList;

/**
 * Script engine for executing JavaScript and Python scripts
 * using GraalVM polyglot API with security sandboxing
 */
@Slf4j
@Component
public class ScriptEngine {
    
    private final long timeoutSeconds;
    private final long maxMemoryMB;
    private final boolean allowHostAccess;
    private final boolean allowIO;
    
    private final Engine engine;
    
    public ScriptEngine(
            @org.springframework.beans.factory.annotation.Value("${graalvm.script.timeout-seconds:5}") long timeoutSeconds,
            @org.springframework.beans.factory.annotation.Value("${graalvm.script.max-memory-mb:128}") long maxMemoryMB,
            @org.springframework.beans.factory.annotation.Value("${graalvm.script.allow-host-access:false}") boolean allowHostAccess,
            @org.springframework.beans.factory.annotation.Value("${graalvm.script.allow-io:false}") boolean allowIO) {
        this.timeoutSeconds = timeoutSeconds;
        this.maxMemoryMB = maxMemoryMB;
        this.allowHostAccess = allowHostAccess;
        this.allowIO = allowIO;
        
        // Create shared engine for better performance
        this.engine = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    }
    
    /**
     * Execute a script in the specified language
     */
    public ScriptExecutionResult execute(String script, String language, ScriptContext context) {
        if (script == null || script.trim().isEmpty()) {
            return ScriptExecutionResult.empty();
        }
        
        validateLanguage(language);
        if (!isLanguageAvailable(language)) {
            throw new ScriptExecutionException(
                "Language '" + language + "' is not available in this JVM.",
                null
            );
        }
        
        Instant startTime = Instant.now();
        
        try (Context polyglotContext = createContext()) {
            // Expose context to script
            Value bindings = polyglotContext.getBindings(language);
            bindings.putMember("request", createRequestObject(polyglotContext, context, language));
            bindings.putMember("response", createResponseObject(polyglotContext, context, language));
            bindings.putMember("state", createStateObject(polyglotContext, context, language));
            bindings.putMember("vars", createVarsObject(polyglotContext, context, language));
            
            // Execute script with timeout
            Value result = executeWithTimeout(polyglotContext, script, language);
            
            // Extract modified response data
            ScriptExecutionResult executionResult = extractResult(result, bindings, context, polyglotContext, language);
            
            Duration duration = Duration.between(startTime, Instant.now());
            log.debug("Script executed in {}ms", duration.toMillis());
            
            return executionResult;
            
        } catch (TimeoutException e) {
            throw new ScriptExecutionException("Script execution timed out after " + timeoutSeconds + " seconds", e);
        } catch (Exception e) {
            log.error("Error executing script: {}", e.getMessage(), e);
            throw new ScriptExecutionException("Script execution failed: " + e.getMessage(), e);
        }
    }
    
    private Context createContext() {
        Context.Builder builder = Context.newBuilder()
            .engine(engine)
            .allowIO(allowIO)
            .allowCreateProcess(false)
            .allowCreateThread(false)
            .allowNativeAccess(false);
            
        if (allowHostAccess) {
            builder.allowAllAccess(true);
        } else {
            builder.allowHostAccess(HostAccess.NONE)
                   .allowHostClassLookup(s -> false);
        }
        
        return builder.build();
    }
    
    private Value executeWithTimeout(Context context, String script, String language) throws TimeoutException {
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        
        String preparedScript = script.trim();
        if ("js".equals(language) && preparedScript.contains("return")) {
            preparedScript = "(function() { " + script + " })()";
        } else if ("python".equals(language) && preparedScript.startsWith("return ")) {
            preparedScript = preparedScript.substring(7);
        }
        
        Value result = context.eval(language, preparedScript);
        
        if (Instant.now().isAfter(deadline)) {
            throw new TimeoutException("Script execution exceeded timeout");
        }
        
        return result;
    }
    
    private Value createRequestObject(Context polyglotContext, ScriptContext context, String language) {
        Object bodyObj = context.getBodyJson() != null ? context.getBodyJson() : (context.getBody() != null ? context.getBody() : "");
        
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("method", context.getMethod() != null ? context.getMethod() : "");
        requestMap.put("path", context.getPath() != null ? context.getPath() : "");
        requestMap.put("headers", context.getHeaders() != null ? context.getHeaders() : Map.of());
        requestMap.put("query", context.getQueryParams() != null ? context.getQueryParams() : Map.of());
        requestMap.put("queryParams", context.getQueryParams() != null ? context.getQueryParams() : Map.of());
        requestMap.put("params", context.getPathVariables() != null ? context.getPathVariables() : Map.of());
        requestMap.put("pathVariables", context.getPathVariables() != null ? context.getPathVariables() : Map.of());
        requestMap.put("body", bodyObj);
        
        return createObject(polyglotContext, language, requestMap);
    }
    
    private Value createResponseObject(Context polyglotContext, ScriptContext context, String language) {
        return createObject(polyglotContext, language, Map.of(
            "status", context.getStatus() != null ? context.getStatus() : 200,
            "headers", context.getResponseHeaders() != null ? context.getResponseHeaders() : Map.of(),
            "body", context.getResponseBody() != null ? context.getResponseBody() : ""
        ));
    }
    
    private Value createStateObject(Context polyglotContext, ScriptContext context, String language) {
        return createObject(polyglotContext, language, context.getState() != null ? context.getState() : Map.of());
    }
    
    private Value createVarsObject(Context polyglotContext, ScriptContext context, String language) {
        return createObject(polyglotContext, language, context.getVariables() != null ? context.getVariables() : Map.of());
    }
    
    private Value createObject(Context polyglotContext, String language, Map<String, ?> map) {
        if ("python".equals(language)) {
            Value dict = polyglotContext.eval("python", "dict()");
            if (map != null) {
                map.forEach((key, value) -> {
                    try {
                        if (value instanceof Map) {
                            dict.putMember(key, createObject(polyglotContext, language, (Map<String, ?>) value));
                        } else {
                            dict.putMember(key, value);
                        }
                    } catch (Exception e) {
                        log.warn("Error setting key '{}' in Python dict", key);
                    }
                });
            }
            return dict;
        } else {
            Value obj = polyglotContext.eval("js", "({})");
            if (map != null) {
                map.forEach((key, value) -> {
                    if (value instanceof Map) {
                        obj.putMember(key, createObject(polyglotContext, language, (Map<String, ?>) value));
                    } else {
                        obj.putMember(key, value);
                    }
                });
            }
            return obj;
        }
    }
    
    private ScriptExecutionResult extractResult(Value resultValue, Value bindings, ScriptContext originalContext, Context polyglotContext, String language) {
        ScriptContext updatedContext = originalContext.toBuilder().build();
        
        Value response = bindings.getMember("response");
        if (response != null && response.hasMembers()) {
            Value status = response.getMember("status");
            if (status != null && status.isNumber()) {
                updatedContext.setStatus(status.asInt());
            }
            
            Value headers = response.getMember("headers");
            if (headers != null && headers.hasMembers()) {
                updatedContext.setResponseHeaders(extractHeaders(headers));
            }
            
            Value body = response.getMember("body");
            if (body != null && body.isString()) {
                updatedContext.setResponseBody(body.asString());
            }
        }
        
        Value state = bindings.getMember("state");
        if (state != null && state.hasMembers()) {
            updatedContext.setState(extractMap(state, language));
        }
        
        Value vars = bindings.getMember("vars");
        if (vars != null && vars.hasMembers()) {
            updatedContext.setVariables(extractMap(vars, language));
        }
        
        return ScriptExecutionResult.builder()
            .context(updatedContext)
            .returnValue(extractValue(resultValue, language))
            .build();
    }
    
    private Map<String, String> extractHeaders(Value headersValue) {
        Map<String, String> headers = new HashMap<>();
        if (headersValue == null || !headersValue.hasMembers()) return headers;
        
        for (String key : headersValue.getMemberKeys()) {
            Value val = headersValue.getMember(key);
            headers.put(key, val != null ? val.toString() : "");
        }
        return headers;
    }
    
    private Map<String, Object> extractMap(Value value, String language) {
        Map<String, Object> map = new HashMap<>();
        if (value == null) return map;
        
        // Handle Hash entries (e.g. Python dicts)
        if (value.hasHashEntries()) {
            try {
                Value keys = value.getHashKeysIterator();
                while (keys.hasIteratorNextElement()) {
                    Value key = keys.getIteratorNextElement();
                    String keyStr = key.isString() ? key.asString() : key.toString();
                    map.put(keyStr, extractValue(value.getHashValue(key), language));
                }
                return map;
            } catch (Exception e) {
                log.debug("Failed to extract map via hash entries: {}", e.getMessage());
            }
        }
        
        // Handle Members (e.g. JS objects)
        if (value.hasMembers()) {
            for (String key : value.getMemberKeys()) {
                // Skip internal methods for Python if falling back
                if ("python".equals(language) && isPythonInternalMethod(key)) continue;
                map.put(key, extractValue(value.getMember(key), language));
            }
        }
        return map;
    }
    
    private boolean isPythonInternalMethod(String name) {
        return List.of("pop", "fromkeys", "setdefault", "keys", "values", "get", 
                       "clear", "update", "popitem", "copy", "items").contains(name);
    }
    
    private Object extractValue(Value value, String language) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isString()) {
            return value.asString();
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isNumber()) {
            if (value.fitsInInt()) return value.asInt();
            if (value.fitsInLong()) return value.asLong();
            return value.asDouble();
        } else if (value.hasMembers()) {
            return extractMap(value, language);
        } else if (value.hasArrayElements()) {
            List<Object> list = new ArrayList<>();
            for (long i = 0; i < value.getArraySize(); i++) {
                list.add(extractValue(value.getArrayElement(i), language));
            }
            return list;
        }
        return value.toString();
    }
    
    private void validateLanguage(String language) {
        if (language == null || (!language.equals("js") && !language.equals("python"))) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    public boolean isLanguageAvailable(String languageId) {
        return engine.getLanguages().containsKey(languageId);
    }
    
    public static class ScriptExecutionException extends RuntimeException {
        public ScriptExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ScriptExecutionResult {
        private ScriptContext context;
        private Object returnValue;
        
        public static ScriptExecutionResult empty() {
            return ScriptExecutionResult.builder().build();
        }
    }
}
