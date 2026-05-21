package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.Iso8583Endpoint;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for Iso8583Endpoint persistence.
 */
public interface Iso8583EndpointRepository {
    
    List<Iso8583Endpoint> findByActiveTrue();

    boolean existsByPortAndIsolatedPortTrue(Integer port);

    Iso8583Endpoint save(Iso8583Endpoint entity);

    Optional<Iso8583Endpoint> findById(String id);

    List<Iso8583Endpoint> findAll();

    void deleteById(String id);

    void deleteAll();
}
