package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.GraphQLEndpoint;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for GraphQLEndpoint persistence.
 */
public interface GraphQLEndpointRepository {
    
    List<GraphQLEndpoint> findByActiveTrue();

    boolean existsByName(String name);

    GraphQLEndpoint save(GraphQLEndpoint entity);

    Optional<GraphQLEndpoint> findById(String id);

    List<GraphQLEndpoint> findAll();

    void deleteById(String id);

    void deleteAll();
}
