package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.in.web.dto.UpdateRouteRequest;
import com.dynamicmock.application.service.RouteService;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.domain.port.out.RouteVersionRepository;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {
    
    @Mock
    private MockRouteRepository repository;
    
    @Mock
    private RouteRegistry routeRegistry;
    
    @Mock
    private RouteVersionRepository versionRepository;
    
    @InjectMocks
    private RouteService routeService;
    
    private MockRoute mockRoute;
    
    @BeforeEach
    void setUp() {
        mockRoute = MockRoute.builder()
            .id("test-id")
            .path("/test")
            .method("GET")
            .responseStatus(200)
            .active(false)
            .version(1)
            .delayMs(0)
            .scriptLanguage("js")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    @Test
    void testCreateRoute() {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/users");
        request.setMethod("GET");
        request.setResponseStatus(200);
        request.setResponseTemplate("{\"message\":\"test\"}");
        
        when(repository.save(any(MockRoute.class))).thenReturn(mockRoute);
        
        RouteResponse response = routeService.createRoute(request);
        
        assertNotNull(response);
        assertEquals("test-id", response.getId());
        verify(repository, times(1)).save(any(MockRoute.class));
    }
    
    @Test
    void testUpdateRoute() {
        UpdateRouteRequest request = new UpdateRouteRequest();
        request.setPath("/updated");
        request.setResponseStatus(201);
        
        when(repository.findById("test-id")).thenReturn(Optional.of(mockRoute));
        when(repository.save(any(MockRoute.class))).thenReturn(mockRoute);
        
        RouteResponse response = routeService.updateRoute("test-id", request);
        
        assertNotNull(response);
        verify(repository, times(1)).findById("test-id");
        verify(repository, times(1)).save(any(MockRoute.class));
    }
    
    @Test
    void testUpdateRouteNotFound() {
        UpdateRouteRequest request = new UpdateRouteRequest();
        
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            routeService.updateRoute("nonexistent", request);
        });
    }
    
    @Test
    void testGetRoute() {
        when(repository.findById("test-id")).thenReturn(Optional.of(mockRoute));
        
        RouteResponse response = routeService.getRoute("test-id");
        
        assertNotNull(response);
        assertEquals("test-id", response.getId());
    }
    
    @Test
    void testGetRouteNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            routeService.getRoute("nonexistent");
        });
    }
    
    @Test
    void testDeleteRoute() {
        mockRoute.setActive(true);
        when(repository.findById("test-id")).thenReturn(Optional.of(mockRoute));
        
        routeService.deleteRoute("test-id");
        
        verify(repository, times(1)).findById("test-id");
        verify(routeRegistry, times(1)).unregister("GET", "/test");
        verify(repository, times(1)).deleteById("test-id");
        verify(versionRepository, times(1)).deleteByRouteId("test-id");
    }
    
    @Test
    void testDeleteRouteNotFound() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            routeService.deleteRoute("nonexistent");
        });
    }
    
    @Test
    void testActivateRoute() {
        when(repository.findById("test-id")).thenReturn(Optional.of(mockRoute));
        when(repository.save(any(MockRoute.class))).thenReturn(mockRoute);
        
        RouteResponse response = routeService.activateRoute("test-id");
        
        assertNotNull(response);
        ArgumentCaptor<MockRoute> routeCaptor = ArgumentCaptor.forClass(MockRoute.class);
        verify(repository, times(1)).save(routeCaptor.capture());
        assertTrue(routeCaptor.getValue().getActive());
        verify(routeRegistry, times(1)).register(any(MockRoute.class));
    }
    
    @Test
    void testDeactivateRoute() {
        mockRoute.setActive(true);
        when(repository.findById("test-id")).thenReturn(Optional.of(mockRoute));
        when(repository.save(any(MockRoute.class))).thenReturn(mockRoute);
        
        RouteResponse response = routeService.deactivateRoute("test-id");
        
        assertNotNull(response);
        ArgumentCaptor<MockRoute> routeCaptor = ArgumentCaptor.forClass(MockRoute.class);
        verify(repository, times(1)).save(routeCaptor.capture());
        assertFalse(routeCaptor.getValue().getActive());
        verify(routeRegistry, times(1)).unregister("GET", "/test");
    }
    
    @Test
    void testUpdateRouteActivatesRegistry() {
        mockRoute.setActive(true);
        UpdateRouteRequest request = new UpdateRouteRequest();
        request.setPath("/updated");
        
        when(repository.findById("test-id")).thenReturn(Optional.of(mockRoute));
        when(repository.save(any(MockRoute.class))).thenReturn(mockRoute);
        
        routeService.updateRoute("test-id", request);
        
        verify(routeRegistry, times(1)).unregister(anyString(), anyString());
        verify(routeRegistry, times(1)).register(any(MockRoute.class));
    }
}

