package com.dynamicmock.core.script;

import com.dynamicmock.adapter.out.script.ScriptContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScriptContextTest {

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() {
        scriptContext = new ScriptContext();
    }

    @Test
    void shouldSetAndGetMethod() {
        // When
        scriptContext.setMethod("POST");

        // Then
        assertEquals("POST", scriptContext.getMethod());
    }

    @Test
    void shouldSetAndGetPath() {
        // When
        scriptContext.setPath("/api/users");

        // Then
        assertEquals("/api/users", scriptContext.getPath());
    }

    @Test
    void shouldSetAndGetBody() {
        // When
        scriptContext.setBody("{\"name\": \"John\"}");

        // Then
        assertEquals("{\"name\": \"John\"}", scriptContext.getBody());
    }

    @Test
    void shouldSetAndGetResponseBody() {
        // When
        scriptContext.setResponseBody("{\"response\": \"data\"}");

        // Then
        assertEquals("{\"response\": \"data\"}", scriptContext.getResponseBody());
    }

    @Test
    void shouldSetAndGetHeaders() {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token");

        // When
        scriptContext.setHeaders(headers);

        // Then
        assertEquals(headers, scriptContext.getHeaders());
        assertEquals("application/json", scriptContext.getHeaders().get("Content-Type"));
    }

    @Test
    void shouldSetAndGetQueryParams() {
        // Given
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("page", "1");
        queryParams.put("limit", "10");

        // When
        scriptContext.setQueryParams(queryParams);

        // Then
        assertEquals(queryParams, scriptContext.getQueryParams());
        assertEquals("1", scriptContext.getQueryParams().get("page"));
    }

    @Test
    void shouldSetAndGetPathVariables() {
        // Given
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("id", "123");
        pathVariables.put("action", "edit");

        // When
        scriptContext.setPathVariables(pathVariables);

        // Then
        assertEquals(pathVariables, scriptContext.getPathVariables());
        assertEquals("123", scriptContext.getPathVariables().get("id"));
    }

    @Test
    void shouldSetAndGetState() {
        // Given
        Map<String, Object> state = new HashMap<>();
        state.put("counter", 5);
        state.put("lastUser", "John");

        // When
        scriptContext.setState(state);

        // Then
        assertEquals(state, scriptContext.getState());
        assertEquals(5, scriptContext.getState().get("counter"));
    }

    @Test
    void shouldSetAndGetVars() {
        // Given
        Map<String, Object> vars = new HashMap<>();
        vars.put("tempValue", "temp");
        vars.put("calculated", 42);

        // When
        scriptContext.setVariables(vars);

        // Then
        assertEquals(vars, scriptContext.getVariables());
        assertEquals("temp", scriptContext.getVariables().get("tempValue"));
    }

    @Test
    void shouldSetAndGetResponseHeaders() {
        // Given
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("X-Custom-Header", "value");

        // When
        scriptContext.setResponseHeaders(responseHeaders);

        // Then
        assertEquals(responseHeaders, scriptContext.getResponseHeaders());
    }

    @Test
    void shouldInitializeWithNullValues() {
        // Then
        assertNull(scriptContext.getMethod());
        assertNull(scriptContext.getPath());
        assertNull(scriptContext.getBody());
    }

    @Test
    void shouldAllowModifyingHeaders() {
        // Given
        Map<String, String> headers = new HashMap<>();
        headers.put("Initial", "value");
        scriptContext.setHeaders(headers);

        // When
        scriptContext.getHeaders().put("Added", "newValue");

        // Then
        assertEquals("newValue", scriptContext.getHeaders().get("Added"));
    }

    @Test
    void shouldAllowModifyingState() {
        // Given
        Map<String, Object> state = new HashMap<>();
        state.put("counter", 0);
        scriptContext.setState(state);

        // When
        scriptContext.getState().put("counter", 1);

        // Then
        assertEquals(1, scriptContext.getState().get("counter"));
    }
}

