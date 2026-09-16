package com.dynamicmock.adapter.out.protocol.iso8583;

import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ISO8583 mock server with dual-mode support:
 * 
 * 1. Q2 Mode (Recommended): Uses jPOS Q2 framework with XML hot-deployment
 *    - Full jPOS ecosystem
 *    - Hot deploy/undeploy
 *    - Complete isolation between mocks
 *    
 * 2. Standalone Mode: Direct socket handling (fallback)
 *    - Simpler setup
 *    - No Q2 dependencies
 *    
 * Q2 mode is enabled by default. Set iso8583.q2.enabled=false to use standalone.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Iso8583Server {
    
    private final ResponseTemplateEngine templateEngine;
    private final ScriptEngine scriptEngine;
    private final Q2ServerManager q2ServerManager;
    
    // Standalone mode: Track running servers by port
    private final Map<Integer, ServerSocket> standaloneServers = new ConcurrentHashMap<>();
    private final Map<Integer, Iso8583Endpoint> endpointConfigs = new ConcurrentHashMap<>();
    private final Map<Integer, ISOPackager> packagers = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * Start an ISO8583 server for the given endpoint.
     * Uses Q2 if enabled, otherwise falls back to standalone mode.
     */
    public void startServer(Iso8583Endpoint endpoint) throws IOException {
        if (q2ServerManager.isQ2Enabled()) {
            try {
                // Q2 Mode: Deploy via XML - completely isolated from other mocks
                q2ServerManager.deployEndpoint(endpoint);
                log.info("Deployed ISO8583 endpoint '{}' via Q2 framework on port {}", 
                        endpoint.getName(), endpoint.getPort());
                return;
            } catch (Exception e) {
                log.warn("Q2 deployment failed for endpoint '{}', falling back to standalone: {}",
                        endpoint.getName(), e.getMessage());
            }
        }
        // Standalone Mode: Direct socket handling
        startStandaloneServer(endpoint);
    }
    
    /**
     * Stop an ISO8583 server.
     * Stopping one mock doesn't affect others (in shared port mode).
     */
    public void stopServer(int port) {
        Iso8583Endpoint endpoint = endpointConfigs.get(port);
        String endpointId = endpoint != null ? endpoint.getId() : null;
        
        if (q2ServerManager.isQ2Enabled() && endpointId != null) {
            // Q2 Mode: Undeploy endpoint
            q2ServerManager.undeployEndpoint(endpointId);
        } else {
            // Standalone Mode
            stopStandaloneServer(port);
        }
        
        endpointConfigs.remove(port);
    }
    
    /**
     * Update an existing ISO8583 mock endpoint.
     * Hot-reloads in Q2 mode without affecting other mocks.
     */
    public void updateServer(Iso8583Endpoint endpoint) throws IOException {
        endpointConfigs.put(endpoint.getPort(), endpoint);
        
        if (q2ServerManager.isQ2Enabled()) {
            // Q2 handles hot-reload automatically via deployEndpoint
            q2ServerManager.deployEndpoint(endpoint);
        } else {
            // Standalone: restart
            stopStandaloneServer(endpoint.getPort());
            startStandaloneServer(endpoint);
        }
    }
    
    /**
     * Shutdown all running servers
     */
    @PreDestroy
    public void shutdownAll() {
        // Q2 shutdown is handled by Q2ServerManager
        
        // Shutdown standalone servers
        for (Integer port : standaloneServers.keySet()) {
            stopStandaloneServer(port);
        }
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("Shutdown all ISO8583 servers");
    }
    
    // ==================== Standalone Mode Implementation ====================
    
    private void startStandaloneServer(Iso8583Endpoint endpoint) throws IOException {
        int port = endpoint.getPort();
        
        if (standaloneServers.containsKey(port)) {
            throw new IllegalStateException("Server already running on port " + port);
        }
        
        // Initialize packager
        ISOPackager packager = createPackager(endpoint);
        packagers.put(port, packager);
        
        ServerSocket serverSocket = new ServerSocket(port);
        standaloneServers.put(port, serverSocket);
        endpointConfigs.put(port, endpoint);
        
        // Start accepting connections
        executorService.submit(() -> acceptConnections(serverSocket, endpoint, packager));
        
        log.info("Started standalone ISO8583 server '{}' on port {}", endpoint.getName(), port);
    }
    
    private void stopStandaloneServer(int port) {
        ServerSocket serverSocket = standaloneServers.remove(port);
        packagers.remove(port);
        
        if (serverSocket != null) {
            try {
                serverSocket.close();
                log.info("Stopped standalone ISO8583 server on port {}", port);
            } catch (IOException e) {
                log.error("Error closing server socket on port {}", port, e);
            }
        }
    }
    
    private ISOPackager createPackager(Iso8583Endpoint endpoint) {
        try {
            InputStream packagerStream = getClass().getResourceAsStream("/iso8583/packager.xml");
            if (packagerStream != null) {
                return new GenericPackager(packagerStream);
            }
            return new org.jpos.iso.packager.ISO87APackager();
        } catch (ISOException e) {
            log.warn("Failed to create custom packager, using default: {}", e.getMessage());
            return new org.jpos.iso.packager.ISO87APackager();
        }
    }
    
    private void acceptConnections(ServerSocket serverSocket, Iso8583Endpoint endpoint, ISOPackager packager) {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.debug("Accepted connection from {} on port {}", clientSocket.getRemoteSocketAddress(), endpoint.getPort());
                executorService.submit(() -> handleConnection(clientSocket, endpoint, packager));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    log.error("Error accepting connection", e);
                }
            }
        }
    }
    
    private void handleConnection(Socket clientSocket, Iso8583Endpoint endpoint, ISOPackager packager) {
        try (clientSocket;
             DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            
            clientSocket.setSoTimeout(30000);
            
            while (!clientSocket.isClosed()) {
                try {
                    log.debug("Awaiting ISO8583 length header port={} headerType={}", endpoint.getPort(), endpoint.getHeaderLengthType());
                    int length = readLength(in, endpoint.getHeaderLengthType());
                    log.debug("ISO8583 inbound length={} headerType={} port={}", length, endpoint.getHeaderLengthType(), endpoint.getPort());
                    if (length <= 0) break;
                    
                    byte[] messageBytes = new byte[length];
                    in.readFully(messageBytes);
                    log.debug("ISO8583 inbound raw hex={}", toHex(messageBytes));
                    
                    ISOMsg request = new ISOMsg();
                    request.setPackager(packager);
                    request.unpack(messageBytes);
                    
                    log.debug("Received ISO8583 message: MTI={} port={} fields={}", request.getMTI(), endpoint.getPort(), request.getComposite().toString());
                    
                    ISOMsg response = processMessage(request, endpoint);
                    byte[] responseBytes = response.pack();
                    log.debug("ISO8583 outbound raw hex={}", toHex(responseBytes));
                    
                    writeLength(out, responseBytes.length, endpoint.getHeaderLengthType());
                    out.write(responseBytes);
                    out.flush();
                    
                    log.debug("Sent ISO8583 response: MTI={} len={} port={}", response.getMTI(), responseBytes.length, endpoint.getPort());
                    
                } catch (EOFException e) {
                    break;
                } catch (ISOException e) {
                    log.error("ISO8583 message processing error: {}", e.getMessage());
                } catch (IOException e) {
                    if (!clientSocket.isClosed()) {
                        log.error("IO error: {}", e.getMessage());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error handling connection", e);
        }
    }
    
    private int readLength(DataInputStream in, String headerType) throws IOException {
        return switch (headerType != null ? headerType : "2BYTE") {
            case "4BYTE" -> in.readInt();
            case "NONE" -> 1024;
            default -> in.readUnsignedShort();
        };
    }
    
    private void writeLength(DataOutputStream out, int length, String headerType) throws IOException {
        switch (headerType != null ? headerType : "2BYTE") {
            case "4BYTE" -> out.writeInt(length);
            case "NONE" -> {}
            default -> out.writeShort(length);
        }
    }
    
    private ISOMsg processMessage(ISOMsg request, Iso8583Endpoint endpoint) throws ISOException {
        String mti = request.getMTI();
        log.debug("Processing ISO8583 message: MTI={}", mti);

        // Build context for scripts and templates
        Map<String, Object> context = buildContext(request, mti);

        // Run interceptor script if enabled
        if (Boolean.TRUE.equals(endpoint.getInterceptorEnabled()) &&
            endpoint.getInterceptorScript() != null) {
            boolean continueProcessing = runInterceptor(endpoint, context, request);
            if (!continueProcessing) {
                log.debug("Message rejected by interceptor");
                ISOMsg response = (ISOMsg) request.clone();
                response.setMTI(getResponseMti(mti));
                response.set(39, "05"); // Do not honor
                return response;
            }
        }

        // Find matching mock
        Iso8583Endpoint.Iso8583Mock matched = findMatchingMock(endpoint, request, mti);

        if (matched == null) {
            log.debug("No matching mock found for MTI={}, using default response", mti);
            ISOMsg response = (ISOMsg) request.clone();
            response.setMTI(getResponseMti(mti));
            response.set(39, "00");
            return response;
        }

        log.debug("Matched mock '{}' for MTI={}", matched.getName(), mti);

        // Clone request to create response
        ISOMsg response = (ISOMsg) request.clone();

        // Apply delay
        if (matched.getDelayMs() != null && matched.getDelayMs() > 0) {
            try {
                Thread.sleep(matched.getDelayMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Set response MTI
        String responseMti = matched.getResponseMti() != null ?
                matched.getResponseMti() : getResponseMti(mti);
        response.setMTI(responseMti);

        // Run mock's response script if enabled
        if (Boolean.TRUE.equals(matched.getScriptEnabled()) && matched.getScript() != null) {
            runResponseScript(matched, context, response);
        }

        // Apply response field templates
        if (matched.getResponseFields() != null) {
            for (Map.Entry<Integer, String> entry : matched.getResponseFields().entrySet()) {
                String value = renderTemplate(entry.getValue(), context);
                if (value != null) {
                    response.set(entry.getKey(), value);
                }
            }
        }

        // Set response code
        if (matched.getResponseCode() != null) {
            response.set(39, matched.getResponseCode());
        } else if (!response.hasField(39)) {
            response.set(39, "00");
        }

        log.debug("ISO8583 response prepared: MTI={} code={}", response.getMTI(), response.getString(39));
        return response;
    }

    /**
     * Find the best matching mock by MTI and field matchers.
     */
    private Iso8583Endpoint.Iso8583Mock findMatchingMock(Iso8583Endpoint endpoint, ISOMsg request, String mti) throws ISOException {
        if (endpoint.getMocks() == null || endpoint.getMocks().isEmpty()) {
            return null;
        }

        // Collect candidates with matching MTI, sorted by priority
        List<Iso8583Endpoint.Iso8583Mock> candidates = endpoint.getMocks().stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .filter(m -> m.getMti() != null && m.getMti().equals(mti))
                .sorted((a, b) -> {
                    int priorityA = a.getPriority() != null ? a.getPriority() : 0;
                    int priorityB = b.getPriority() != null ? b.getPriority() : 0;
                    return Integer.compare(priorityB, priorityA);
                })
                .collect(Collectors.toList());

        // Find first candidate whose matchers match the request
        for (Iso8583Endpoint.Iso8583Mock candidate : candidates) {
            if (matchesConditions(request, candidate.getMatchers())) {
                return candidate;
            }
        }

        // Fallback: find a catch-all mock (no matchers) among candidates
        for (Iso8583Endpoint.Iso8583Mock candidate : candidates) {
            if (candidate.getMatchers() == null || candidate.getMatchers().isEmpty()) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Check if the request matches the mock's field matchers using regex.
     */
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

    /**
     * Build the context map for scripts and templates from the ISO8583 request.
     */
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

        // Shared state
        context.put("state", new HashMap<String, Object>());

        return context;
    }

    /**
     * Run the interceptor script. Returns false if the script rejects the message.
     */
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

    /**
     * Run a mock's response script to set dynamic response fields.
     */
    private void runResponseScript(Iso8583Endpoint.Iso8583Mock mock, Map<String, Object> context, ISOMsg response) {
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

    /**
     * Render a template string using the Handlebars template engine.
     */
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
    
    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
