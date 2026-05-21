package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.ScenarioMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScenarioMongoRepository extends MongoRepository<ScenarioMongoEntity, String> {
    Optional<ScenarioMongoEntity> findByName(String name);
    boolean existsByName(String name);
    List<ScenarioMongoEntity> findByActiveTrue();
}
