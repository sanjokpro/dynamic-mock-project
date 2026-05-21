package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.Scenario;

import java.util.List;
import java.util.Optional;

/**
 * Output Port for Scenario persistence.
 */
public interface ScenarioRepository {
    
    Optional<Scenario> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Scenario> findByActiveTrue();

    <S extends Scenario> S save(S entity);

    Optional<Scenario> findById(String id);

    List<Scenario> findAll();

    void deleteById(String id);

    void deleteAll();
}
