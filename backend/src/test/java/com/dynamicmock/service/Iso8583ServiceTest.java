package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.Iso8583EndpointRequest;
import com.dynamicmock.adapter.out.protocol.iso8583.Iso8583Server;
import com.dynamicmock.application.service.Iso8583Service;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.port.out.Iso8583EndpointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Iso8583ServiceTest {

    @Mock
    private Iso8583EndpointRepository repository;

    @Mock
    private Iso8583Server iso8583Server;

    @InjectMocks
    private Iso8583Service iso8583Service;

    private Iso8583Endpoint testEndpoint;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(iso8583Service, "defaultPort", 8583);
        
        testEndpoint = Iso8583Endpoint.builder()
                .id("test-id")
                .name("Test ISO8583")
                .description("Test Description")
                .port(8583)
                .isolatedPort(false)
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findAll_shouldReturnAllEndpoints() {
        // Given
        List<Iso8583Endpoint> endpoints = Arrays.asList(testEndpoint);
        when(repository.findAll()).thenReturn(endpoints);

        // When
        List<Iso8583Endpoint> result = iso8583Service.findAll();

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
        Iso8583Endpoint result = iso8583Service.findById("test-id");

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
            () -> iso8583Service.findById("unknown"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void create_shouldCreateEndpoint() {
        // Given
        Iso8583EndpointRequest request = new Iso8583EndpointRequest();
        request.setName("New ISO8583");
        request.setPort(8584);
        request.setIsolatedPort(true);
        request.setActive(false);

        when(repository.existsByPortAndIsolatedPortTrue(8584)).thenReturn(false);
        when(repository.save(any(Iso8583Endpoint.class))).thenAnswer(inv -> {
            Iso8583Endpoint e = inv.getArgument(0);
            e.setId("new-id");
            return e;
        });

        // When
        Iso8583Endpoint result = iso8583Service.create(request);

        // Then
        assertNotNull(result);
        assertEquals("New ISO8583", result.getName());
        verify(repository).save(any(Iso8583Endpoint.class));
    }

    @Test
    void create_shouldThrowWhenIsolatedPortExists() {
        // Given
        Iso8583EndpointRequest request = new Iso8583EndpointRequest();
        request.setName("New ISO8583");
        request.setPort(8583);
        request.setIsolatedPort(true);

        when(repository.existsByPortAndIsolatedPortTrue(8583)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> iso8583Service.create(request));
        assertTrue(exception.getMessage().contains("already exists on port"));
    }

    @Test
    void create_shouldUseDefaultPortWhenNotProvided() {
        // Given
        Iso8583EndpointRequest request = new Iso8583EndpointRequest();
        request.setName("New ISO8583");
        request.setActive(false);
        // Port not set

        when(repository.save(any(Iso8583Endpoint.class))).thenAnswer(inv -> {
            Iso8583Endpoint e = inv.getArgument(0);
            e.setId("new-id");
            return e;
        });

        // When
        Iso8583Endpoint result = iso8583Service.create(request);

        // Then
        assertEquals(8583, result.getPort());
    }

    @Test
    void update_shouldUpdateEndpoint() {
        // Given
        Iso8583EndpointRequest request = new Iso8583EndpointRequest();
        request.setName("Updated Name");
        request.setPort(8583);
        request.setActive(false);

        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(Iso8583Endpoint.class))).thenReturn(testEndpoint);

        // When
        Iso8583Endpoint result = iso8583Service.update("test-id", request);

        // Then
        assertNotNull(result);
        verify(repository).findById("test-id");
        verify(repository).save(any(Iso8583Endpoint.class));
    }

    @Test
    void delete_shouldDeleteEndpoint() {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));

        // When
        iso8583Service.delete("test-id");

        // Then
        verify(repository).deleteById("test-id");
    }

    @Test
    void activate_shouldActivateEndpoint() throws Exception {
        // Given
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(Iso8583Endpoint.class))).thenReturn(testEndpoint);
        doNothing().when(iso8583Server).startServer(any(Iso8583Endpoint.class));

        // When
        Iso8583Endpoint result = iso8583Service.activate("test-id");

        // Then
        assertTrue(result.getActive());
        verify(iso8583Server).startServer(any(Iso8583Endpoint.class));
        verify(repository).save(any(Iso8583Endpoint.class));
    }

    @Test
    void deactivate_shouldDeactivateEndpoint() {
        // Given
        testEndpoint.setActive(true);
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(Iso8583Endpoint.class))).thenReturn(testEndpoint);

        // When
        Iso8583Endpoint result = iso8583Service.deactivate("test-id");

        // Then
        assertFalse(result.getActive());
        verify(iso8583Server).stopServer(8583);
        verify(repository).save(any(Iso8583Endpoint.class));
    }

    @Test
    void reloadActiveEndpoints_shouldLoadAllActiveEndpoints() throws Exception {
        // Given
        testEndpoint.setActive(true);
        when(repository.findByActiveTrue()).thenReturn(Arrays.asList(testEndpoint));
        doNothing().when(iso8583Server).startServer(any(Iso8583Endpoint.class));

        // When
        iso8583Service.reloadActiveEndpoints();

        // Then
        verify(repository).findByActiveTrue();
        verify(iso8583Server).startServer(any(Iso8583Endpoint.class));
    }

    @Test
    void shutdownAll_shouldShutdownAllServers() {
        // When
        iso8583Service.shutdownAll();

        // Then
        verify(iso8583Server).shutdownAll();
    }
}

