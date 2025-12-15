package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.GrpcEndpointRequest;
import com.dynamicmock.adapter.out.protocol.grpc.DynamicGrpcServer;
import com.dynamicmock.application.service.GrpcService;
import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.dynamicmock.domain.port.out.GrpcEndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrpcServiceTest {

    @Mock
    private GrpcEndpointRepository repository;

    @Mock
    private DynamicGrpcServer grpcServer;

    @InjectMocks
    private GrpcService grpcService;

    private GrpcEndpoint testEndpoint;

    @BeforeEach
    void setUp() {
        testEndpoint = GrpcEndpoint.builder()
                .id("test-id")
                .name("Test gRPC")
                .description("Test Description")
                .serviceName("com.test.Service")
                .port(9090)
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findAll_shouldReturnAllEndpoints() {
        // Given
        List<GrpcEndpoint> endpoints = Arrays.asList(testEndpoint);
        when(repository.findAll()).thenReturn(endpoints);

        // When
        List<GrpcEndpoint> result = grpcService.findAll();

        // Then
        assertEquals(1, result.size());
        assertEquals(testEndpoint.getId(), result.get(0).getId());
        verify(repository).findAll();
    }

    @Test
    void findById_shouldReturnEndpoint() {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));

        // When
        GrpcEndpoint result = grpcService.findById("test-id");

        // Then
        assertEquals(testEndpoint.getId(), result.getId());
        verify(repository).findById("test-id");
    }

    @Test
    void findById_shouldThrowWhenNotFound() {
        // Given
        when(repository.findById("unknown")).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> grpcService.findById("unknown"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void create_shouldCreateEndpoint() {
        // Given
        GrpcEndpointRequest request = new GrpcEndpointRequest();
        request.setName("New gRPC");
        request.setServiceName("com.new.Service");
        request.setPort(9091);
        request.setActive(false);

        when(repository.existsByServiceName("com.new.Service")).thenReturn(false);
        when(repository.existsByPort(9091)).thenReturn(false);
        when(repository.save(any(GrpcEndpoint.class))).thenAnswer(inv -> {
            GrpcEndpoint e = inv.getArgument(0);
            e.setId("new-id");
            return e;
        });

        // When
        GrpcEndpoint result = grpcService.create(request);

        // Then
        assertNotNull(result);
        assertEquals("New gRPC", result.getName());
        verify(repository).existsByServiceName("com.new.Service");
        verify(repository).save(any(GrpcEndpoint.class));
    }

    @Test
    void create_shouldThrowWhenServiceNameExists() {
        // Given
        GrpcEndpointRequest request = new GrpcEndpointRequest();
        request.setServiceName("com.existing.Service");

        when(repository.existsByServiceName("com.existing.Service")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> grpcService.create(request));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void create_shouldThrowWhenPortExists() {
        // Given
        GrpcEndpointRequest request = new GrpcEndpointRequest();
        request.setServiceName("com.new.Service");
        request.setPort(9090);

        when(repository.existsByServiceName("com.new.Service")).thenReturn(false);
        when(repository.existsByPort(9090)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> grpcService.create(request));
        assertTrue(exception.getMessage().contains("port"));
    }

    @Test
    void update_shouldUpdateEndpoint() {
        // Given
        GrpcEndpointRequest request = new GrpcEndpointRequest();
        request.setName("Updated Name");
        request.setServiceName("com.updated.Service");
        request.setPort(9090);
        request.setActive(false);

        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(GrpcEndpoint.class))).thenReturn(testEndpoint);

        // When
        GrpcEndpoint result = grpcService.update("test-id", request);

        // Then
        assertNotNull(result);
        verify(repository).findById("test-id");
        verify(repository).save(any(GrpcEndpoint.class));
    }

    @Test
    void delete_shouldDeleteEndpoint() {
        // When
        grpcService.delete("test-id");

        // Then
        verify(repository).deleteById("test-id");
    }

    @Test
    void activate_shouldActivateEndpoint() throws Exception {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(GrpcEndpoint.class))).thenReturn(testEndpoint);
        when(grpcServer.startService(any(GrpcEndpoint.class))).thenReturn(9090);

        // When
        GrpcEndpoint result = grpcService.activate("test-id");

        // Then
        assertTrue(result.getActive());
        verify(grpcServer).startService(any(GrpcEndpoint.class));
        verify(repository).save(any(GrpcEndpoint.class));
    }

    @Test
    void deactivate_shouldDeactivateEndpoint() {
        // Given
        testEndpoint.setActive(true);
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(GrpcEndpoint.class))).thenReturn(testEndpoint);

        // When
        GrpcEndpoint result = grpcService.deactivate("test-id");

        // Then
        assertFalse(result.getActive());
        verify(repository).save(any(GrpcEndpoint.class));
    }

    @Test
    void reloadActiveEndpoints_shouldLoadAllActiveEndpoints() throws Exception {
        // Given
        testEndpoint.setActive(true);
        when(repository.findByActiveTrue()).thenReturn(Arrays.asList(testEndpoint));
        when(grpcServer.startService(any(GrpcEndpoint.class))).thenReturn(9090);

        // When
        grpcService.reloadActiveEndpoints();

        // Then
        verify(repository).findByActiveTrue();
        verify(grpcServer).startService(any(GrpcEndpoint.class));
    }

    @Test
    void shutdownAll_shouldShutdownAllServers() {
        // When
        grpcService.shutdownAll();

        // Then
        verify(grpcServer).shutdownAll();
    }
}

