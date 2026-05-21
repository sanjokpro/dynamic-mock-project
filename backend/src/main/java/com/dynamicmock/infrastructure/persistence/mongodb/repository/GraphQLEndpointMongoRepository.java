package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.GraphQLEndpointMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GraphQLEndpointMongoRepository extends MongoRepository<GraphQLEndpointMongoEntity, String> {
    List<GraphQLEndpointMongoEntity> findByActiveTrue();
    boolean existsByName(String name);
}
