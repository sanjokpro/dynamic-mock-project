package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.Iso8583EndpointRequest;
import com.dynamicmock.adapter.out.protocol.iso8583.Iso8583Server;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.port.out.Iso8583EndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class Iso8583Service {
    
    private final Iso8583EndpointRepository repository;
    private final Iso8583Server iso8583Server;
    
    @Value("${iso8583.default-port:8583}")
    private int defaultPort;
    
    public List<Iso8583Endpoint> findAll() {
        return repository.findAll();
    }
    
    public Iso8583Endpoint findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ISO8583 endpoint not found: " + id));
    }
    
    public Iso8583Endpoint create(Iso8583EndpointRequest request) {
        // Validate port if isolated mode
        if (Boolean.TRUE.equals(request.getIsolatedPort()) && request.getPort() != null) {
            if (repository.existsByPortAndIsolatedPortTrue(request.getPort())) {
                throw new RuntimeException("Isolated ISO8583 endpoint already exists on port: " + request.getPort());
            }
        }
        
        sanitizeMocks(request);
        
        Iso8583Endpoint endpoint = Iso8583Endpoint.builder()
                .name(request.getName())
                .description(request.getDescription())
                .port(request.getPort() != null ? request.getPort() : defaultPort)
                .isolatedPort(request.getIsolatedPort() != null ? request.getIsolatedPort() : false)
                .mocks(request.getMocks())
                .interceptorScript(request.getInterceptorScript())
                .interceptorScriptLanguage(request.getInterceptorScriptLanguage())
                .interceptorEnabled(request.getInterceptorEnabled() != null ? request.getInterceptorEnabled() : false)
                .customServerXml(request.getCustomServerXml())
                .customXmlEnabled(request.getCustomXmlEnabled() != null ? request.getCustomXmlEnabled() : false)
                .headerLengthType(request.getHeaderLengthType() != null ? request.getHeaderLengthType() : "2BYTE")
                .encoding(request.getEncoding() != null ? request.getEncoding() : "ASCII")
                .packagerConfig(request.getPackagerConfig())
                .active(request.getActive() != null ? request.getActive() : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        Iso8583Endpoint saved = repository.save(endpoint);
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            activateEndpoint(saved);
        }
        
        return saved;
    }
    
    public Iso8583Endpoint update(String id, Iso8583EndpointRequest request) {
        Iso8583Endpoint existing = findById(id);
        
        // Check port conflict for isolated mode
        if (Boolean.TRUE.equals(request.getIsolatedPort()) && 
            request.getPort() != null && 
            !request.getPort().equals(existing.getPort())) {
            if (repository.existsByPortAndIsolatedPortTrue(request.getPort())) {
                throw new RuntimeException("Isolated ISO8583 endpoint already exists on port: " + request.getPort());
            }
        }
        
        // Deactivate if currently active
        if (Boolean.TRUE.equals(existing.getActive())) {
            deactivateEndpoint(existing);
        }
        
        sanitizeMocks(request);
        
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPort(request.getPort() != null ? request.getPort() : defaultPort);
        existing.setIsolatedPort(request.getIsolatedPort() != null ? request.getIsolatedPort() : false);
        existing.setMocks(request.getMocks());
        existing.setInterceptorScript(request.getInterceptorScript());
        existing.setInterceptorScriptLanguage(request.getInterceptorScriptLanguage());
        existing.setInterceptorEnabled(request.getInterceptorEnabled());
        existing.setCustomServerXml(request.getCustomServerXml());
        existing.setCustomXmlEnabled(request.getCustomXmlEnabled());
        existing.setHeaderLengthType(request.getHeaderLengthType());
        existing.setEncoding(request.getEncoding());
        existing.setPackagerConfig(request.getPackagerConfig());
        existing.setActive(request.getActive());
        existing.setUpdatedAt(LocalDateTime.now());
        
        Iso8583Endpoint saved = repository.save(existing);
        
        if (Boolean.TRUE.equals(saved.getActive())) {
            activateEndpoint(saved);
        }
        
        return saved;
    }
    
    public void delete(String id) {
        Iso8583Endpoint endpoint = findById(id);
        deactivateEndpoint(endpoint);
        repository.deleteById(id);
    }
    
    public Iso8583Endpoint activate(String id) {
        Iso8583Endpoint endpoint = findById(id);
        activateEndpoint(endpoint);
        endpoint.setActive(true);
        endpoint.setUpdatedAt(LocalDateTime.now());
        return repository.save(endpoint);
    }
    
    public Iso8583Endpoint deactivate(String id) {
        Iso8583Endpoint endpoint = findById(id);
        deactivateEndpoint(endpoint);
        endpoint.setActive(false);
        endpoint.setUpdatedAt(LocalDateTime.now());
        return repository.save(endpoint);
    }
    
    private void activateEndpoint(Iso8583Endpoint endpoint) {
        try {
            iso8583Server.startServer(endpoint);
            log.info("Activated ISO8583 endpoint '{}' on port {}", 
                    endpoint.getName(), endpoint.getPort());
        } catch (Exception e) {
            log.error("Failed to activate ISO8583 endpoint: {}", endpoint.getName(), e);
            throw new RuntimeException("Failed to start ISO8583 server: " + e.getMessage(), e);
        }
    }
    
    private void deactivateEndpoint(Iso8583Endpoint endpoint) {
        try {
            iso8583Server.stopServer(endpoint.getPort());
            log.info("Deactivated ISO8583 endpoint '{}'", endpoint.getName());
        } catch (Exception e) {
            log.warn("Error deactivating ISO8583 endpoint: {}", e.getMessage());
        }
    }
    
    public void reloadActiveEndpoints() {
        List<Iso8583Endpoint> activeEndpoints = repository.findByActiveTrue();
        for (Iso8583Endpoint endpoint : activeEndpoints) {
            try {
                activateEndpoint(endpoint);
            } catch (Exception e) {
                log.error("Failed to reload ISO8583 endpoint: {}", endpoint.getName(), e);
            }
        }
        log.info("Reloaded {} active ISO8583 endpoints", activeEndpoints.size());
    }
    
    public void shutdownAll() {
        iso8583Server.shutdownAll();
    }

    private void sanitizeMocks(Iso8583EndpointRequest request) {
        if (request.getMocks() == null) {
            return;
        }
        request.getMocks().forEach(mock -> {
            if (mock.getId() == null) {
                mock.setId(UUID.randomUUID().toString());
            }
            if (mock.getMatchers() != null) {
                mock.setMatchers(mock.getMatchers().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                e -> e.getKey().replace('.', '_'),
                                java.util.Map.Entry::getValue
                        )));
            }
        });
    }
}
