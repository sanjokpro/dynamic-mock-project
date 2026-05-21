package com.dynamicmock.adapter.out.protocol.iso8583;

import com.dynamicmock.domain.entity.Iso8583Endpoint;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jpos.q2.Q2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Q2 framework for ISO8583 servers.
 * 
 * Key Design:
 * - Multiple mocks can share the same port (default behavior)
 * - Mocks are routed by MTI + field matchers, not by port
 * - Optional: Isolated port mode for complete separation
 * - Optional: Custom jPOS XML for advanced users
 * - Optional: Interceptor scripts (GraalVM) for all messages
 */
@Slf4j
@Component
public class Q2ServerManager {
    
    @Value("${iso8583.q2.deploy-dir:deploy}")
    private String deployDir;
    
    @Value("${iso8583.q2.enabled:true}")
    private boolean q2Enabled;
    
    @Value("${iso8583.default-port:8583}")
    private int defaultPort;
    
    private Q2 q2;
    
    // Track deployed servers by port
    private final Map<Integer, String> deployedServersByPort = new ConcurrentHashMap<>();
    
    // Track all endpoint configs (multiple endpoints can share a port)
    private static final Map<Integer, List<Iso8583Endpoint>> endpointsByPort = new ConcurrentHashMap<>();
    
    // Track individual endpoint configs by ID
    private static final Map<String, Iso8583Endpoint> endpointsById = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() throws IOException {
        if (!q2Enabled) {
            log.info("Q2 framework disabled, using standalone ISO8583 server");
            return;
        }
        
        Path deployPath = Paths.get(deployDir);
        if (!Files.exists(deployPath)) {
            Files.createDirectories(deployPath);
            log.info("Created Q2 deploy directory: {}", deployPath.toAbsolutePath());
        }
        
        createBaseConfigurations();
        
        q2 = new Q2(deployDir);
        q2.start();
        
        log.info("Q2 framework started with deploy directory: {}", deployPath.toAbsolutePath());
    }
    
    @PreDestroy
    public void shutdown() {
        if (q2 != null) {
            q2.shutdown(true);
            log.info("Q2 framework shutdown");
        }
    }
    
    /**
     * Deploy or update an ISO8583 endpoint.
     * 
     * - If isolatedPort=true: Creates dedicated server
     * - If isolatedPort=false (default): Shares port with other endpoints
     */
    public void deployEndpoint(Iso8583Endpoint endpoint) throws IOException {
        String endpointId = endpoint.getId();
        int port = endpoint.getPort() != null ? endpoint.getPort() : defaultPort;
        
        // Register endpoint
        endpointsById.put(endpointId, endpoint);
        
        // Add to port's endpoint list
        endpointsByPort.compute(port, (p, list) -> {
            if (list == null) {
                list = new java.util.ArrayList<>();
            }
            // Remove existing if updating
            list.removeIf(e -> e.getId().equals(endpointId));
            list.add(endpoint);
            return list;
        });
        
        // Check if server already running on this port
        if (deployedServersByPort.containsKey(port)) {
            // Server exists - just update endpoint registry (hot config update)
            log.info("Updated ISO8583 endpoint '{}' on shared port {}", endpoint.getName(), port);
            return;
        }
        
        // Deploy new server for this port
        deployServer(port, endpoint);
    }
    
    /**
     * Undeploy an endpoint.
     * Only removes server if this was the last endpoint on the port.
     */
    public void undeployEndpoint(String endpointId) {
        Iso8583Endpoint endpoint = endpointsById.remove(endpointId);
        if (endpoint == null) return;
        
        int port = endpoint.getPort() != null ? endpoint.getPort() : defaultPort;
        
        // Remove from port's endpoint list
        List<Iso8583Endpoint> remaining = endpointsByPort.compute(port, (p, list) -> {
            if (list != null) {
                list.removeIf(e -> e.getId().equals(endpointId));
                return list.isEmpty() ? null : list;
            }
            return null;
        });
        
        // If no endpoints left on port, undeploy server
        if (remaining == null) {
            undeployServer(port);
        }
        
        log.info("Undeployed ISO8583 endpoint '{}' from port {}", endpoint.getName(), port);
    }
    
    /**
     * Get all endpoints configured for a port.
     */
    public static List<Iso8583Endpoint> getEndpointsByPort(int port) {
        return endpointsByPort.getOrDefault(port, List.of());
    }
    
    /**
     * Get endpoint by ID.
     */
    public static Iso8583Endpoint getEndpointById(String id) {
        return endpointsById.get(id);
    }
    
    private void deployServer(int port, Iso8583Endpoint endpoint) throws IOException {
        String filename = String.format("50_iso8583_port_%d.xml", port);
        Path filePath = Paths.get(deployDir, filename);
        
        String serverXml;
        if (Boolean.TRUE.equals(endpoint.getCustomXmlEnabled()) && 
            endpoint.getCustomServerXml() != null && 
            !endpoint.getCustomServerXml().isBlank()) {
            // Use custom XML (advanced mode)
            serverXml = endpoint.getCustomServerXml();
            log.info("Using custom jPOS XML for port {}", port);
        } else {
            // Generate XML
            serverXml = generateServerXml(port, endpoint);
        }
        
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(serverXml);
        }
        
        deployedServersByPort.put(port, filename);
        log.info("Deployed ISO8583 server on port {} -> {}", port, filename);
    }
    
    private void undeployServer(int port) {
        String filename = deployedServersByPort.remove(port);
        if (filename != null) {
            Path filePath = Paths.get(deployDir, filename);
            try {
                Files.deleteIfExists(filePath);
                log.info("Undeployed ISO8583 server from port {}", port);
            } catch (IOException e) {
                log.error("Failed to undeploy server: {}", filename, e);
            }
        }
    }
    
    private void createBaseConfigurations() throws IOException {
        createLoggerConfig();
        
        Path cfgPath = Paths.get(deployDir, "..", "cfg");
        if (!Files.exists(cfgPath)) {
            Files.createDirectories(cfgPath);
        }
        copyPackagerConfig(cfgPath);
    }
    
    private void createLoggerConfig() throws IOException {
        String loggerXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <logger name="Q2" class="org.jpos.q2.qbean.LoggerAdaptor">
                <log-listener class="org.jpos.util.SimpleLogListener"/>
            </logger>
            """;
        
        Path loggerPath = Paths.get(deployDir, "00_logger.xml");
        if (!Files.exists(loggerPath)) {
            Files.writeString(loggerPath, loggerXml);
        }
    }
    
    private void copyPackagerConfig(Path cfgPath) throws IOException {
        Path packagerPath = cfgPath.resolve("packager.xml");
        if (!Files.exists(packagerPath)) {
            var is = getClass().getResourceAsStream("/iso8583/packager.xml");
            if (is != null) {
                Files.copy(is, packagerPath);
            }
        }
    }
    
    private String generateServerXml(int port, Iso8583Endpoint endpoint) {
        String channelClass = getChannelClass(endpoint.getEncoding());
        String packagerConfig = endpoint.getPackagerConfig() != null ? 
                endpoint.getPackagerConfig() : "cfg/packager.xml";
        
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <!--
                ISO8583 Server - Port %d
                Auto-generated by Dynamic Mock Server
                
                This server handles multiple mock scenarios.
                Mocks are routed by MTI + field matchers.
                
                To use custom configuration:
                1. Enable "Advanced Mode" in the UI
                2. Edit the XML directly
            -->
            <server name="iso8583-server-%d" 
                    class="org.jpos.iso.server.ISOServer" 
                    logger="Q2">
                
                <attr name="port">%d</attr>
                <attr name="minSessions">1</attr>
                <attr name="maxSessions">100</attr>
                <attr name="timeout">30000</attr>
                
                <channel class="%s"
                         packager="org.jpos.iso.packager.GenericPackager"
                         logger="Q2">
                    <property name="packager-config" value="%s"/>
                    %s
                </channel>
                
                <request-listener class="com.dynamicmock.core.protocol.iso8583.DynamicMockRequestListener"
                                  logger="Q2">
                    <property name="server-port" value="%d"/>
                </request-listener>
                
            </server>
            """,
            port, port, port,
            channelClass,
            packagerConfig,
            getHeaderConfig(endpoint.getHeaderLengthType()),
            port
        );
    }
    
    private String getChannelClass(String encoding) {
        if (encoding == null) return "org.jpos.iso.channel.ASCIIChannel";
        return switch (encoding.toUpperCase()) {
            case "EBCDIC" -> "org.jpos.iso.channel.BASE24Channel";
            case "BINARY" -> "org.jpos.iso.channel.NACChannel";
            default -> "org.jpos.iso.channel.ASCIIChannel";
        };
    }
    
    private String getHeaderConfig(String headerType) {
        if (headerType == null) return "";
        return switch (headerType.toUpperCase()) {
            case "4BYTE" -> "<property name=\"length-digits\" value=\"4\"/>";
            case "NONE" -> "<property name=\"length-digits\" value=\"0\"/>";
            default -> "";
        };
    }
    
    public boolean isQ2Enabled() {
        return q2Enabled;
    }
    
    public boolean isQ2Running() {
        return q2 != null && q2.running();
    }
}
