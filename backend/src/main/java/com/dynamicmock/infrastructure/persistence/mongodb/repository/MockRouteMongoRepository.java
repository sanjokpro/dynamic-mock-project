package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.MockRouteMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MockRouteMongoRepository extends MongoRepository<MockRouteMongoEntity, String> {
    
    List<MockRouteMongoEntity> findByActiveTrue();
    
    Optional<MockRouteMongoEntity> findByPathAndMethodAndVersion(String path, String method, Integer version);
    
    List<MockRouteMongoEntity> findByPathAndMethod(String path, String method);
}
