package com.dynamicmock.adapter.out.script;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Context object passed to scripts during execution
 * Contains request data, response data, and state
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScriptContext {
    
    // Request data
    private String method;
    private String path;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private Map<String, String> pathVariables;
    private String body;
    private Map<String, Object> bodyJson;
    
    // Response data (can be modified by scripts)
    private Integer status;
    private Map<String, String> responseHeaders;
    private String responseBody;
    
    // State (shared across requests, stored in Redis)
    private Map<String, Object> state;
    
    // Variables (request-scoped)
    private Map<String, Object> variables;
}

