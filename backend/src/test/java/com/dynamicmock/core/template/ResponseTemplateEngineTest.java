package com.dynamicmock.core.template;

import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ResponseTemplateEngineTest {

    private ResponseTemplateEngine templateEngine;

    @Mock
    private ScriptEngine scriptEngine;

    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() {
        templateEngine = new ResponseTemplateEngine(scriptEngine, objectMapper);
    }
    
    @Test
    void testRenderSimpleTemplate() {
        String template = "Hello {{name}}";
        Map<String, Object> context = Map.of("name", "World");
        String result = templateEngine.render(template, context);
        assertEquals("Hello World", result);
    }
    
    @Test
    void testRenderWithRandomInt() {
        // Note: Add space between }} and } to avoid Handlebars parsing issue
        String template = "{\"value\":{{$randomInt 1 100}} }";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"value\":"));
        // Extract number and verify it's in range
        String numberStr = result.replaceAll(".*\"value\":(\\d+).*", "$1");
        int value = Integer.parseInt(numberStr);
        assertTrue(value >= 1 && value <= 100);
    }
    
    @Test
    void testRenderWithRandomBool() {
        // Note: Add space between }} and } to avoid Handlebars parsing issue
        String template = "{\"active\":{{$randomBool}} }";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"active\":"));
    }
    
    @Test
    void testRenderWithRandomString() {
        String template = "{\"id\":\"{{$randomString 8}}\"}";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"id\":\""));
    }
    
    @Test
    void testRenderWithTimestamp() {
        // Note: Add space between }} and } to avoid Handlebars parsing issue
        String template = "{\"timestamp\":{{$timestamp}} }";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"timestamp\":"));
        // Extract timestamp and verify it's a valid number
        String timestampStr = result.replaceAll(".*\"timestamp\":(\\d+).*", "$1");
        long timestamp = Long.parseLong(timestampStr);
        assertTrue(timestamp > 0);
    }
    
    @Test
    void testRenderWithRandomUUID() {
        String template = "{\"uuid\":\"{{$randomUUID}}\"}";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"uuid\":\""));
        // Verify UUID format
        String uuid = result.replaceAll(".*\"uuid\":\"([^\"]+)\".*", "$1");
        assertTrue(uuid.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));
    }
    
    @Test
    void testRenderWithRandomEmail() {
        String template = "{\"email\":\"{{$randomEmail}}\"}";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"email\":\""));
        String email = result.replaceAll(".*\"email\":\"([^\"]+)\".*", "$1");
        assertTrue(email.contains("@"));
    }
    
    @Test
    void testRenderWithContextVariables() {
        String template = "{\"userId\":\"{{request.pathVariables.id}}\",\"method\":\"{{request.method}}\"}";
        Map<String, Object> context = new HashMap<>();
        context.put("request", Map.of(
            "pathVariables", Map.of("id", "123"),
            "method", "GET"
        ));
        String result = templateEngine.render(template, context);
        assertTrue(result.contains("\"userId\":\"123\""));
        assertTrue(result.contains("\"method\":\"GET\""));
    }
    
    @Test
    void testRenderEmptyTemplate() {
        String result = templateEngine.render("");
        assertEquals("", result);
    }
    
    @Test
    void testRenderNullTemplate() {
        String result = templateEngine.render(null);
        assertEquals("", result);
    }
    
    @Test
    void testRenderComplexTemplate() {
        // Note: For numeric values at end of JSON, add space between }} and } to avoid Handlebars parsing issue
        String template = "{\"id\":\"{{$randomUUID}}\",\"name\":\"{{$randomString 10}}\",\"value\":{{$randomInt 1 100}},\"timestamp\":{{$timestamp}} }";
        String result = templateEngine.render(template);
        assertNotNull(result);
        assertTrue(result.contains("\"id\":\""));
        assertTrue(result.contains("\"name\":\""));
        assertTrue(result.contains("\"value\":"));
        assertTrue(result.contains("\"timestamp\":"));
    }
}

