package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.MockRoute;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for MockRoute persistence.
 * Pure Java interface with zero framework dependencies.
 */
public interface MockRouteRepository {
    
    List<MockRoute> findByActiveTrue();
    
    Optional<MockRoute> findByPathAndMethodAndVersion(String path, String method, Integer version);
    
    List<MockRoute> findByPathAndMethod(String path, String method);

    MockRoute save(MockRoute entity);

    Optional<MockRoute> findById(String id);

    boolean existsById(String id);

    List<MockRoute> findAll();

    long count();

    void deleteById(String id);

    void delete(MockRoute entity);

    void deleteAll();
}
