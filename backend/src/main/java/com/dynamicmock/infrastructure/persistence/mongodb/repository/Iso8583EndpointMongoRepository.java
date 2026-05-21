package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.Iso8583EndpointMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Iso8583EndpointMongoRepository extends MongoRepository<Iso8583EndpointMongoEntity, String> {
    List<Iso8583EndpointMongoEntity> findByActiveTrue();
    boolean existsByPortAndIsolatedPortTrue(Integer port);
}
