package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.RouteVersionMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteVersionMongoRepository extends MongoRepository<RouteVersionMongoEntity, String> {
    
    List<RouteVersionMongoEntity> findByRouteIdOrderByVersionNumberDesc(String routeId);
    
    Optional<RouteVersionMongoEntity> findByRouteIdAndVersionNumber(String routeId, int versionNumber);

    Optional<RouteVersionMongoEntity> findTopByRouteIdOrderByVersionNumberDesc(String routeId);

    void deleteByRouteId(String routeId);
}
