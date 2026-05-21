package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.RouteVersion;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for RouteVersion persistence.
 */
public interface RouteVersionRepository {
    
    List<RouteVersion> findByRouteIdOrderByVersionNumberDesc(String routeId);
    
    Optional<RouteVersion> findByRouteIdAndVersionNumber(String routeId, int versionNumber);

    Optional<RouteVersion> findTopByRouteIdOrderByVersionNumberDesc(String routeId);

    RouteVersion save(RouteVersion entity);

    Optional<RouteVersion> findById(String id);

    List<RouteVersion> findAll();

    void deleteById(String id);

    void deleteByRouteId(String routeId);

    void deleteAll();
}
