package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.GrpcEndpointRequest;
import com.dynamicmock.adapter.out.protocol.grpc.DynamicGrpcServer;
import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.dynamicmock.domain.port.out.GrpcEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrpcService {
    
    private final GrpcEndpointRepository repository;
    private final DynamicGrpcServer grpcServer;
    
    // Track running servers by endpoint ID
    private final Map<String, Integer> runningServers = new ConcurrentHashMap<>();
    
    public List<GrpcEndpoint> findAll() {
        return repository.findAll();
    }
    
    public GrpcEndpoint findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("gRPC endpoint not found: " + id));
    }
    
    public GrpcEndpoint create(GrpcEndpointRequest request) {
        if (repository.existsByServiceName(request.getServiceName())) {
            throw new RuntimeException("gRPC endpoint with service name already exists: " + request.getServiceName());
        }
        
        if (request.getPort() != null && repository.existsByPort(request.getPort())) {
            throw new RuntimeException("gRPC endpoint already running on port: " + request.getPort());
        }
        
        GrpcEndpoint endpoint = GrpcEndpoint.builder()
                .name(request.getName())
                .description(request.getDescription())
                .serviceName(request.getServiceName())
                .protoSchema(request.getProtoSchema())
                .methods(request.getMethods())
                .port(request.getPort())
                .active(request.getActive() != null ? request.getActive() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        GrpcEndpoint saved = repository.save(endpoint);
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            activateEndpoint(saved);
        }
        
        return saved;
    }
    
    public GrpcEndpoint update(String id, GrpcEndpointRequest request) {
        GrpcEndpoint existing = findById(id);
        
        // Check if port changed and new port is in use
        if (request.getPort() != null && !request.getPort().equals(existing.getPort())) {
            if (repository.existsByPort(request.getPort())) {
                throw new RuntimeException("gRPC endpoint already running on port: " + request.getPort());
            }
        }
        
        // Deactivate current server if running
        if (runningServers.containsKey(id)) {
            deactivateEndpoint(id);
        }
        
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setServiceName(request.getServiceName());
        existing.setProtoSchema(request.getProtoSchema());
        existing.setMethods(request.getMethods());
        existing.setPort(request.getPort());
        existing.setActive(request.getActive());
        existing.setUpdatedAt(LocalDateTime.now());
        
        GrpcEndpoint saved = repository.save(existing);
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            activateEndpoint(saved);
        }
        
        return saved;
    }
    
    public void delete(String id) {
        deactivateEndpoint(id);
        repository.deleteById(id);
    }
    
    public GrpcEndpoint activate(String id) {
        GrpcEndpoint endpoint = findById(id);
        activateEndpoint(endpoint);
        endpoint.setActive(true);
        endpoint.setUpdatedAt(LocalDateTime.now());
        return repository.save(endpoint);
    }
    
    public GrpcEndpoint deactivate(String id) {
        GrpcEndpoint endpoint = findById(id);
        deactivateEndpoint(id);
        endpoint.setActive(false);
        endpoint.setUpdatedAt(LocalDateTime.now());
        return repository.save(endpoint);
    }
    
    private void activateEndpoint(GrpcEndpoint endpoint) {
        try {
            int port = grpcServer.startService(endpoint);
            runningServers.put(endpoint.getId(), port);
            log.info("Activated gRPC endpoint '{}' on port {}", endpoint.getName(), port);
        } catch (Exception e) {
            log.error("Failed to activate gRPC endpoint: {}", endpoint.getName(), e);
            throw new RuntimeException("Failed to start gRPC server: " + e.getMessage(), e);
        }
    }
    
    private void deactivateEndpoint(String id) {
        Integer port = runningServers.remove(id);
        if (port != null) {
            grpcServer.stopService(port);
            log.info("Deactivated gRPC endpoint on port {}", port);
        }
    }
    
    /**
     * Reload all active endpoints on startup
     */
    public void reloadActiveEndpoints() {
        List<GrpcEndpoint> activeEndpoints = repository.findByActiveTrue();
        for (GrpcEndpoint endpoint : activeEndpoints) {
            try {
                activateEndpoint(endpoint);
            } catch (Exception e) {
                log.error("Failed to reload gRPC endpoint: {}", endpoint.getName(), e);
            }
        }
        log.info("Reloaded {} active gRPC endpoints", activeEndpoints.size());
    }
    
    /**
     * Shutdown all running gRPC servers
     */
    public void shutdownAll() {
        grpcServer.shutdownAll();
        runningServers.clear();
    }
}

