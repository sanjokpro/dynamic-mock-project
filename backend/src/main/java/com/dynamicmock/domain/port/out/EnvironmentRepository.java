package com.dynamicmock.domain.port.out;

import com.dynamicmock.domain.entity.Environment;
import java.util.List;
import java.util.Optional;

public interface EnvironmentRepository {
    Environment save(Environment environment);
    Optional<Environment> findById(String id);
    List<Environment> findAll();
    void deleteById(String id);
}
