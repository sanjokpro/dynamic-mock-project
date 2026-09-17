package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.CollectionMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoCollectionRepository extends MongoRepository<CollectionMongoEntity, String> {
    List<CollectionMongoEntity> findByUserId(String userId);
}
