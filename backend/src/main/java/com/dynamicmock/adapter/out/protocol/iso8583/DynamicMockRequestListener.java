package com.dynamicmock.adapter.out.protocol.iso8583;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.entity.Iso8583Endpoint.Iso8583Mock;
import lombok.extern.slf4j.Slf4j;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * jPOS Request Listener that handles ISO8583 messages.
 * 
 * Key Features:
 * - Multiple mocks share the same port (routed by MTI + field matchers)
 * - Optional interceptor script (GraalVM) runs for ALL messages
 * - Per-mock response scripts for complex logic
 * - Handlebars templating for response fields
 * 
 * Message Flow:
 * 1. Receive ISO8583 message
 * 2. Run interceptor script (if enabled) - can modify/reject
 * 3. Find matching mock (MTI + field matchers, priority order)
 * 4. Run mock's response script (if enabled)
 * 5. Apply response field templates
 * 6. Send response
 */
@Slf4j
public class DynamicMockRequestListener implements ISORequestListener, Configurable {
    
    private int serverPort;
    private ResponseTemplateEngine templateEngine;
    private ScriptEngine scriptEngine;
    
    private static ApplicationContext applicationContext;
    
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }
    
    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.serverPort = cfg.getInt("server-port", 8583);
        
        if (applicationContext != null) {
            this.templateEngine = applicationContext.getBean(ResponseTemplateEngine.class);
            this.scriptEngine = applicationContext.getBean(ScriptEngine.class);
        }
        
        log.info("DynamicMockRequestListener configured for port {}", serverPort);
    }
    
    @Override
    public boolean process(ISOSource source, ISOMsg request) {
        try {
            String mti = request.getMTI();
            log.debug("Received ISO8583 on port {}: MTI={}", serverPort, mti);
            
            // Get all endpoints configured for this port
            List<Iso8583Endpoint> endpoints = Q2ServerManager.getEndpointsByPort(serverPort);
            
            if (endpoints.isEmpty()) {
                log.warn("No endpoints configured for port {}", serverPort);
                sendDefaultResponse(source, request);
                return true;
            }
            
            // Build context for scripts and templates
            Map<String, Object> context = buildContext(request, mti);
            
            // Run interceptor scripts (from all endpoints with interceptors enabled)
            for (Iso8583Endpoint endpoint : endpoints) {
                if (Boolean.TRUE.equals(endpoint.getInterceptorEnabled()) && 
                    endpoint.getInterceptorScript() != null) {
                    
                    boolean continueProcessing = runInterceptor(endpoint, context, request);
                    if (!continueProcessing) {
                        // Interceptor rejected the message
                        log.debug("Message rejected by interceptor from endpoint '{}'", endpoint.getName());
                        return true;
                    }
                }
            }
            
            // Find matching mock across all endpoints
            MatchedMock matched = findMatchingMock(endpoints, request, mti);
            
            if (matched == null) {
                log.debug("No matching mock found for MTI={}, using default response", mti);
                sendDefaultResponse(source, request);
                return true;
            }
            
            log.debug("Matched mock '{}' from endpoint '{}'", 
                    matched.mock.getName(), matched.endpoint.getName());
            
            // Process and send response
            ISOMsg response = processMessage(request, matched, context);
            source.send(response);
            
            log.debug("Sent response: MTI={}", response.getMTI());
            return true;
            
        } catch (Exception e) {
            log.error("Error processing message on port {}", serverPort, e);
            try {
                sendErrorResponse(source, request, e.getMessage());
            } catch (Exception ex) {
                log.error("Failed to send error response", ex);
            }
            return true;
        }
    }
    
    private boolean runInterceptor(Iso8583Endpoint endpoint, Map<String, Object> context, ISOMsg request) {
        try {
            ScriptContext scriptContext = new ScriptContext();
            scriptContext.setBody(context.get("request").toString());
            scriptContext.setVariables(new HashMap<>(context));
            
            // Add control variable
            scriptContext.getVariables().put("continueProcessing", true);
            
            scriptEngine.execute(
                    endpoint.getInterceptorScript(),
                    endpoint.getInterceptorScriptLanguage() != null ? 
                            endpoint.getInterceptorScriptLanguage() : "js",
                    scriptContext
            );
            
            // Check if script wants to stop processing
            Object continueFlag = scriptContext.getVariables().get("continueProcessing");
            if (continueFlag instanceof Boolean && !((Boolean) continueFlag)) {
                return false;
            }
            
            // Update context with any modifications from interceptor
            context.putAll(scriptContext.getVariables());
            
            return true;
        } catch (Exception e) {
            log.error("Interceptor script error in endpoint '{}'", endpoint.getName(), e);
            return true; // Continue processing on error
        }
    }
    
    private MatchedMock findMatchingMock(List<Iso8583Endpoint> endpoints, ISOMsg request, String mti) 
            throws ISOException {
        
        // Collect all mocks with their endpoints, sorted by priority
        List<MatchedMock> candidates = new ArrayList<>();
        
        for (Iso8583Endpoint endpoint : endpoints) {
            if (endpoint.getMocks() == null) continue;
            
            for (Iso8583Mock mock : endpoint.getMocks()) {
                if (!Boolean.TRUE.equals(mock.getEnabled())) continue;
                if (!mock.getMti().equals(mti)) continue;
                
                candidates.add(new MatchedMock(endpoint, mock));
            }
        }
        
        // Sort by priority (descending)
        candidates.sort((a, b) -> {
            int priorityA = a.mock.getPriority() != null ? a.mock.getPriority() : 0;
            int priorityB = b.mock.getPriority() != null ? b.mock.getPriority() : 0;
            return Integer.compare(priorityB, priorityA);
        });
        
        // Find first matching mock
        for (MatchedMock candidate : candidates) {
            if (matchesConditions(request, candidate.mock.getMatchers())) {
                return candidate;
            }
        }
        
        // Fallback: find mock with no matchers (catch-all)
        for (MatchedMock candidate : candidates) {
            if (candidate.mock.getMatchers() == null || candidate.mock.getMatchers().isEmpty()) {
                return candidate;
            }
        }
        
        return null;
    }
    
    private boolean matchesConditions(ISOMsg request, Map<String, String> matchers) throws ISOException {
        if (matchers == null || matchers.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, String> entry : matchers.entrySet()) {
            String key = entry.getKey();
            String pattern = entry.getValue();
            
            String actualValue = null;
            
            if (key.startsWith("field.") || key.startsWith("field_")) {
                int fieldNum = Integer.parseInt(key.substring(6));
                actualValue = request.getString(fieldNum);
            } else if (key.equals("mti")) {
                actualValue = request.getMTI();
            }
            
            if (actualValue == null) {
                return false;
            }
            
            if (!Pattern.matches(pattern, actualValue)) {
                return false;
            }
        }
        
        return true;
    }
    
    private ISOMsg processMessage(ISOMsg request, MatchedMock matched, Map<String, Object> context) 
            throws ISOException {
        
        Iso8583Mock mock = matched.mock;
        String mti = request.getMTI();
        
        // Clone request to create response
        ISOMsg response = (ISOMsg) request.clone();
        
        // Apply delay
        if (mock.getDelayMs() != null && mock.getDelayMs() > 0) {
            try {
                Thread.sleep(mock.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Set response MTI
        String responseMti = mock.getResponseMti() != null ? 
                mock.getResponseMti() : getResponseMti(mti);
        response.setMTI(responseMti);
        
        // Run mock's response script if enabled
        if (Boolean.TRUE.equals(mock.getScriptEnabled()) && mock.getScript() != null) {
            runResponseScript(mock, context, response);
        }
        
        // Apply response field templates
        if (mock.getResponseFields() != null) {
            for (Map.Entry<Integer, String> entry : mock.getResponseFields().entrySet()) {
                String value = renderTemplate(entry.getValue(), context);
                response.set(entry.getKey(), value);
            }
        }
        
        // Set response code
        if (mock.getResponseCode() != null) {
            response.set(39, mock.getResponseCode());
        } else if (!response.hasField(39)) {
            response.set(39, "00");
        }
        
        return response;
    }
    
    private void runResponseScript(Iso8583Mock mock, Map<String, Object> context, ISOMsg response) {
        try {
            ScriptContext scriptContext = new ScriptContext();
            scriptContext.setBody(context.get("request").toString());
            scriptContext.setVariables(new HashMap<>(context));
            
            // Add mutable response fields map
            Map<String, String> responseFields = new HashMap<>();
            scriptContext.getVariables().put("responseFields", responseFields);
            
            scriptEngine.execute(
                    mock.getScript(),
                    mock.getScriptLanguage() != null ? mock.getScriptLanguage() : "js",
                    scriptContext
            );
            
            // Apply script-set response fields
            @SuppressWarnings("unchecked")
            Map<String, String> scriptResponseFields = 
                    (Map<String, String>) scriptContext.getVariables().get("responseFields");
            
            if (scriptResponseFields != null) {
                for (Map.Entry<String, String> entry : scriptResponseFields.entrySet()) {
                    try {
                        response.set(Integer.parseInt(entry.getKey()), entry.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid field number in script response: {}", entry.getKey());
                    }
                }
            }
            
            // Update context
            context.putAll(scriptContext.getVariables());
            
        } catch (Exception e) {
            log.error("Response script error in mock '{}'", mock.getName(), e);
        }
    }
    
    private Map<String, Object> buildContext(ISOMsg request, String mti) throws ISOException {
        Map<String, Object> context = new HashMap<>();
        Map<String, String> requestFields = new HashMap<>();
        
        for (int i = 0; i <= 128; i++) {
            if (request.hasField(i)) {
                requestFields.put(String.valueOf(i), request.getString(i));
            }
        }
        
        context.put("request", requestFields);
        context.put("mti", mti);
        
        // Common fields for convenience
        context.put("pan", request.getString(2));
        context.put("processingCode", request.getString(3));
        context.put("amount", request.getString(4));
        context.put("stan", request.getString(11));
        context.put("localTime", request.getString(12));
        context.put("localDate", request.getString(13));
        context.put("expiryDate", request.getString(14));
        context.put("mcc", request.getString(18));
        context.put("posEntryMode", request.getString(22));
        context.put("rrn", request.getString(37));
        context.put("terminalId", request.getString(41));
        context.put("merchantId", request.getString(42));
        context.put("currencyCode", request.getString(49));
        
        // Shared state (for stateful mocks)
        context.put("state", new HashMap<String, Object>());
        
        return context;
    }
    
    private String renderTemplate(String template, Map<String, Object> context) {
        if (templateEngine == null || template == null) {
            return template;
        }
        try {
            return templateEngine.render(template, context);
        } catch (Exception e) {
            log.warn("Template rendering error: {}", e.getMessage());
            return template;
        }
    }
    
    private String getResponseMti(String requestMti) {
        char[] mti = requestMti.toCharArray();
        if (mti.length >= 4) {
            mti[2] = (char) (mti[2] + 1);
        }
        return new String(mti);
    }
    
    private void sendDefaultResponse(ISOSource source, ISOMsg request) throws ISOException, IOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI(getResponseMti(request.getMTI()));
        response.set(39, "00");
        source.send(response);
    }
    
    private void sendErrorResponse(ISOSource source, ISOMsg request, String error) 
            throws ISOException, IOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI(getResponseMti(request.getMTI()));
        response.set(39, "96");
        source.send(response);
    }
    
    /**
     * Helper class to track matched mock with its parent endpoint
     */
    private static class MatchedMock {
        final Iso8583Endpoint endpoint;
        final Iso8583Mock mock;
        
        MatchedMock(Iso8583Endpoint endpoint, Iso8583Mock mock) {
            this.endpoint = endpoint;
            this.mock = mock;
        }
    }
}
