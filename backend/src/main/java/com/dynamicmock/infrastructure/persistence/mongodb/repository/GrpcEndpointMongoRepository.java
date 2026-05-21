package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.GrpcEndpointMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrpcEndpointMongoRepository extends MongoRepository<GrpcEndpointMongoEntity, String> {
    List<GrpcEndpointMongoEntity> findByActiveTrue();
    boolean existsByServiceName(String serviceName);
    boolean existsByPort(Integer port);
}
