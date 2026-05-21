package com.dynamicmock.service;

import com.dynamicmock.application.service.VersionService;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.entity.RouteVersion;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.domain.port.out.RouteVersionRepository;
import com.dynamicmock.infrastructure.filter.RouteRegistry;
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
class VersionServiceTest {

    @Mock
    private RouteVersionRepository versionRepository;

    @Mock
    private MockRouteRepository routeRepository;

    @Mock
    private RouteRegistry routeRegistry;

    @InjectMocks
    private VersionService versionService;

    private MockRoute testRoute;
    private RouteVersion testVersion;

    @BeforeEach
    void setUp() {
        testRoute = MockRoute.builder()
                .id("route-id")
                .path("/test")
                .method("GET")
                .responseTemplate("{}")
                .responseStatus(200)
                .version(1)
                .active(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testVersion = RouteVersion.builder()
                .id("version-id")
                .routeId("route-id")
                .versionNumber(1)
                .path("/test")
                .method("GET")
                .responseTemplate("{}")
                .responseStatus(200)
                .changeDescription("Initial version")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void saveVersion_shouldSaveNewVersion() {
        // Given
        when(versionRepository.findTopByRouteIdOrderByVersionNumberDesc("route-id"))
            .thenReturn(Optional.empty());
        when(versionRepository.save(any(RouteVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RouteVersion result = versionService.saveVersion(testRoute, "New change");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getVersionNumber());
        assertEquals("New change", result.getChangeDescription());
        verify(versionRepository).save(any(RouteVersion.class));
    }

    @Test
    void saveVersion_shouldIncrementVersionNumber() {
        // Given
        when(versionRepository.findTopByRouteIdOrderByVersionNumberDesc("route-id"))
            .thenReturn(Optional.of(testVersion));
        when(versionRepository.save(any(RouteVersion.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        RouteVersion result = versionService.saveVersion(testRoute, "Another change");

        // Then
        assertEquals(2, result.getVersionNumber());
    }

    @Test
    void getVersionHistory_shouldReturnAllVersions() {
        // Given
        when(versionRepository.findByRouteIdOrderByVersionNumberDesc("route-id"))
            .thenReturn(Arrays.asList(testVersion));

        // When
        List<RouteVersion> result = versionService.getVersionHistory("route-id");

        // Then
        assertEquals(1, result.size());
        assertEquals(testVersion.getId(), result.get(0).getId());
    }

    @Test
    void getVersion_shouldReturnSpecificVersion() {
        // Given
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 1))
            .thenReturn(Optional.of(testVersion));

        // When
        RouteVersion result = versionService.getVersion("route-id", 1);

        // Then
        assertEquals(testVersion.getId(), result.getId());
    }

    @Test
    void getVersion_shouldThrowWhenNotFound() {
        // Given
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 99))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> versionService.getVersion("route-id", 99));
    }

    @Test
    void rollbackToVersion_shouldRollbackRoute() {
        // Given
        when(routeRepository.findById("route-id")).thenReturn(Optional.of(testRoute));
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 1))
            .thenReturn(Optional.of(testVersion));
        when(versionRepository.findTopByRouteIdOrderByVersionNumberDesc("route-id"))
            .thenReturn(Optional.of(testVersion));
        when(versionRepository.save(any(RouteVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routeRepository.save(any(MockRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MockRoute result = versionService.rollbackToVersion("route-id", 1);

        // Then
        assertNotNull(result);
        assertEquals("/test", result.getPath());
        verify(routeRepository).save(any(MockRoute.class));
    }

    @Test
    void rollbackToVersion_shouldThrowWhenRouteNotFound() {
        // Given
        when(routeRepository.findById("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> versionService.rollbackToVersion("unknown", 1));
    }

    @Test
    void rollbackToVersion_shouldThrowWhenVersionNotFound() {
        // Given
        when(routeRepository.findById("route-id")).thenReturn(Optional.of(testRoute));
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 99))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> versionService.rollbackToVersion("route-id", 99));
    }

    @Test
    void rollbackToVersion_shouldUpdateRegistryWhenActive() {
        // Given
        testRoute.setActive(true);
        when(routeRepository.findById("route-id")).thenReturn(Optional.of(testRoute));
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 1))
            .thenReturn(Optional.of(testVersion));
        when(versionRepository.findTopByRouteIdOrderByVersionNumberDesc("route-id"))
            .thenReturn(Optional.of(testVersion));
        when(versionRepository.save(any(RouteVersion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(routeRepository.save(any(MockRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        versionService.rollbackToVersion("route-id", 1);

        // Then
        verify(routeRegistry).unregister("GET", "/test");
        verify(routeRegistry).register(any(MockRoute.class));
    }

    @Test
    void diffVersions_shouldReturnDifferences() {
        // Given
        RouteVersion version2 = RouteVersion.builder()
                .id("version-id-2")
                .routeId("route-id")
                .versionNumber(2)
                .path("/test/updated")
                .method("POST")
                .responseTemplate("{\"updated\": true}")
                .responseStatus(201)
                .changeDescription("Updated version")
                .createdAt(LocalDateTime.now())
                .build();

        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 1))
            .thenReturn(Optional.of(testVersion));
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 2))
            .thenReturn(Optional.of(version2));

        // When
        VersionService.VersionDiff diff = versionService.diffVersions("route-id", 1, 2);

        // Then
        assertNotNull(diff);
        assertEquals("route-id", diff.getRouteId());
        assertEquals(1, diff.getVersion1());
        assertEquals(2, diff.getVersion2());
        assertTrue(diff.getTotalChanges() > 0);
    }

    @Test
    void diffWithCurrent_shouldCompareToCurrent() {
        // Given
        when(routeRepository.findById("route-id")).thenReturn(Optional.of(testRoute));
        when(versionRepository.findByRouteIdAndVersionNumber("route-id", 1))
            .thenReturn(Optional.of(testVersion));

        // When
        VersionService.VersionDiff diff = versionService.diffWithCurrent("route-id", 1);

        // Then
        assertNotNull(diff);
        assertEquals("route-id", diff.getRouteId());
    }

    @Test
    void deleteVersionHistory_shouldDeleteAllVersions() {
        // When
        versionService.deleteVersionHistory("route-id");

        // Then
        verify(versionRepository).deleteByRouteId("route-id");
    }
}

