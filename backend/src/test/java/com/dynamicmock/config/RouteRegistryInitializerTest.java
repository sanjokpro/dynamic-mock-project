package com.dynamicmock.config;

import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.infrastructure.config.RouteRegistryInitializer;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteRegistryInitializerTest {

    @Mock
    private RouteRegistry routeRegistry;

    @Mock
    private MockRouteRepository repository;

    @InjectMocks
    private RouteRegistryInitializer initializer;

    @Test
    void initialize_shouldLoadActiveRoutes() {
        // When
        initializer.initialize();

        // Then
        verify(repository).findByActiveTrue();
        verify(routeRegistry).loadRoutes(any());
    }

    @Test
    void initialize_shouldHandleExceptions() {
        // Given
        doThrow(new RuntimeException("db down")).when(repository).findByActiveTrue();

        // When
        initializer.initialize();

        // Then
        verify(repository).findByActiveTrue();
        // loadRoutes should not be called due to exception
        verify(routeRegistry, never()).loadRoutes(any());
    }
}

