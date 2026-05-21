package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.GrpcEndpoint;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for GrpcEndpoint persistence.
 */
public interface GrpcEndpointRepository {
    
    List<GrpcEndpoint> findByActiveTrue();

    boolean existsByServiceName(String serviceName);

    boolean existsByPort(Integer port);

    GrpcEndpoint save(GrpcEndpoint entity);

    Optional<GrpcEndpoint> findById(String id);

    List<GrpcEndpoint> findAll();

    void deleteById(String id);

    void deleteAll();
}
