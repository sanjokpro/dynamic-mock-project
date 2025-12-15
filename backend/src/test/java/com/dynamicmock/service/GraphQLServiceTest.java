package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.GraphQLEndpointRequest;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.application.service.GraphQLService;
import com.dynamicmock.domain.entity.GraphQLEndpoint;
import com.dynamicmock.domain.port.out.GraphQLEndpointRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class GraphQLServiceTest {

    @Mock
    private GraphQLEndpointRepository repository;

    @Mock
    private ResponseTemplateEngine templateEngine;

    @Mock
    private ScriptEngine scriptEngine;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GraphQLService graphQLService;

    private GraphQLEndpoint testEndpoint;

    @BeforeEach
    void setUp() {
        testEndpoint = GraphQLEndpoint.builder()
                .id("test-id")
                .name("Test GraphQL")
                .description("Test Description")
                .schema("type Query { hello: String }")
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void findAll_shouldReturnAllEndpoints() {
        // Given
        List<GraphQLEndpoint> endpoints = Arrays.asList(testEndpoint);
        when(repository.findAll()).thenReturn(endpoints);

        // When
        List<GraphQLEndpoint> result = graphQLService.findAll();

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
        GraphQLEndpoint result = graphQLService.findById("test-id");

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
            () -> graphQLService.findById("unknown"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void create_shouldCreateEndpoint() {
        // Given
        GraphQLEndpointRequest request = new GraphQLEndpointRequest();
        request.setName("New GraphQL");
        request.setSchema("type Query { hello: String }");
        request.setActive(false);

        when(repository.existsByName("New GraphQL")).thenReturn(false);
        when(repository.save(any(GraphQLEndpoint.class))).thenAnswer(inv -> {
            GraphQLEndpoint e = inv.getArgument(0);
            e.setId("new-id");
            return e;
        });

        // When
        GraphQLEndpoint result = graphQLService.create(request);

        // Then
        assertNotNull(result);
        assertEquals("New GraphQL", result.getName());
        verify(repository).existsByName("New GraphQL");
        verify(repository).save(any(GraphQLEndpoint.class));
    }

    @Test
    void create_shouldThrowWhenNameExists() {
        // Given
        GraphQLEndpointRequest request = new GraphQLEndpointRequest();
        request.setName("Existing Name");

        when(repository.existsByName("Existing Name")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> graphQLService.create(request));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void update_shouldUpdateEndpoint() {
        // Given
        GraphQLEndpointRequest request = new GraphQLEndpointRequest();
        request.setName("Updated Name");
        request.setSchema("type Query { hello: String }");
        request.setActive(false);

        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(GraphQLEndpoint.class))).thenReturn(testEndpoint);

        // When
        GraphQLEndpoint result = graphQLService.update("test-id", request);

        // Then
        assertNotNull(result);
        verify(repository).findById("test-id");
        verify(repository).save(any(GraphQLEndpoint.class));
    }

    @Test
    void delete_shouldDeleteEndpoint() {
        // When
        graphQLService.delete("test-id");

        // Then
        verify(repository).deleteById("test-id");
    }

    @Test
    void activate_shouldActivateEndpoint() {
        // Given
        testEndpoint.setSchema("type Query { hello: String }");
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(GraphQLEndpoint.class))).thenReturn(testEndpoint);

        // When
        GraphQLEndpoint result = graphQLService.activate("test-id");

        // Then
        assertTrue(result.getActive());
        verify(repository).save(any(GraphQLEndpoint.class));
    }

    @Test
    void deactivate_shouldDeactivateEndpoint() {
        // Given
        testEndpoint.setActive(true);
        when(repository.findById("test-id")).thenReturn(Optional.of(testEndpoint));
        when(repository.save(any(GraphQLEndpoint.class))).thenReturn(testEndpoint);

        // When
        GraphQLEndpoint result = graphQLService.deactivate("test-id");

        // Then
        assertFalse(result.getActive());
        verify(repository).save(any(GraphQLEndpoint.class));
    }

    @Test
    void reloadActiveEndpoints_shouldLoadAllActiveEndpoints() {
        // Given
        testEndpoint.setActive(true);
        testEndpoint.setSchema("type Query { hello: String }");
        when(repository.findByActiveTrue()).thenReturn(Arrays.asList(testEndpoint));

        // When
        graphQLService.reloadActiveEndpoints();

        // Then
        verify(repository).findByActiveTrue();
    }
}

