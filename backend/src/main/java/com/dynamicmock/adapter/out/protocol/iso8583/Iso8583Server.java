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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        // Placeholder for new message processing logic using endpoint.getMocks()
        String mti = request.getMTI();
        ISOMsg response = (ISOMsg) request.clone();
        response.setMTI(getResponseMti(mti));
        response.set(39, "00");
        return response;
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
