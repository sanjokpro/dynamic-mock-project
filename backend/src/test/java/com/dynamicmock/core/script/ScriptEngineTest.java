package com.dynamicmock.core.script;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ScriptEngineTest {
    
    private ScriptEngine scriptEngine;
    
    @BeforeEach
    void setUp() {
        scriptEngine = new ScriptEngine(5, 128, false, false);
    }
    
    @Test
    void testExecuteEmptyScript() {
        ScriptContext context = createTestContext();
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute("", "js", context);
        assertNotNull(result);
    }
    
    @Test
    void testExecuteNullScript() {
        ScriptContext context = createTestContext();
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(null, "js", context);
        assertNotNull(result);
    }
    
    @Test
    void testExecuteJavaScriptScript() {
        ScriptContext context = createTestContext();
        String script = "response.status = 201; response.body = 'Hello from script';";
        
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "js", context);
        
        assertNotNull(result);
        assertNotNull(result.getContext());
    }
    
    @Test
    void testExecutePythonScript() {
        ScriptContext context = createTestContext();
        String script = "response['status'] = 201\nresponse['body'] = 'Hello from Python'";
        
        org.junit.jupiter.api.Assumptions.assumeTrue(
            scriptEngine.isLanguageAvailable("python"),
            "GraalPython not installed; run on GraalVM with python component (gu install python)"
        );

        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "python", context);
        
        assertNotNull(result);
        assertNotNull(result.getContext());
    }
    
    @Test
    void testExecuteScriptWithRequestAccess() {
        ScriptContext context = createTestContext();
        context.setMethod("GET");
        context.setPath("/test");
        context.setHeaders(Map.of("Content-Type", "application/json"));
        
        String script = "vars.method = request.method; vars.path = request.path;";
        
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "js", context);
        
        assertNotNull(result);
    }
    
    @Test
    void testExecuteScriptThrowsExceptionForUnsupportedLanguage() {
        ScriptContext context = createTestContext();
        String script = "print('test')";
        
        assertThrows(IllegalArgumentException.class, () -> {
            scriptEngine.execute(script, "ruby", context);
        });
    }
    
    @Test
    void testExecuteScriptThrowsExceptionForNullLanguage() {
        ScriptContext context = createTestContext();
        String script = "print('test')";
        
        assertThrows(IllegalArgumentException.class, () -> {
            scriptEngine.execute(script, null, context);
        });
    }
    
    @Test
    void testExecuteScriptWithTimeout() {
        ScriptEngine shortTimeoutEngine = new ScriptEngine(1, 128, false, false);
        ScriptContext context = createTestContext();
        
        // Script that runs longer than 1 second
        String script = "var start = Date.now(); while(Date.now() - start < 2000) {}";
        
        // Expect a timeout when script exceeds allowed duration
        assertThrows(ScriptEngine.ScriptExecutionException.class, () -> {
            shortTimeoutEngine.execute(script, "js", context);
        });
    }
    
    @Test
    void testExecuteScriptWithVariables() {
        ScriptContext context = createTestContext();
        context.setVariables(new HashMap<>(Map.of("initial", "value")));
        
        String script = "vars.newVar = 'newValue';";
        
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "js", context);
        
        assertNotNull(result);
    }
    
    @Test
    void testExecuteScriptWithState() {
        ScriptContext context = createTestContext();
        context.setState(new HashMap<>(Map.of("count", 0)));
        
        String script = "state.count = (state.count || 0) + 1;";
        
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "js", context);
        
        assertNotNull(result);
    }
    
    @Test
    void testExecuteScriptWithQueryParams() {
        ScriptContext context = createTestContext();
        context.setQueryParams(Map.of("userId", "123", "page", "1"));
        
        String script = "vars.userId = request.queryParams.userId;";
        
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "js", context);
        
        assertNotNull(result);
    }
    
    @Test
    void testExecuteScriptWithPathVariables() {
        ScriptContext context = createTestContext();
        context.setPathVariables(Map.of("id", "456"));
        
        String script = "vars.id = request.pathVariables.id;";
        
        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(script, "js", context);
        
        assertNotNull(result);
    }
    
    private ScriptContext createTestContext() {
        return ScriptContext.builder()
            .method("GET")
            .path("/test")
            .headers(new HashMap<>())
            .queryParams(new HashMap<>())
            .pathVariables(new HashMap<>())
            .body("")
            .status(200)
            .responseHeaders(new HashMap<>())
            .variables(new HashMap<>())
            .state(new HashMap<>())
            .build();
    }
}

