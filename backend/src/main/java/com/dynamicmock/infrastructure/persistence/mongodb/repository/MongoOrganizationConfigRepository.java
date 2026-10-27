package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.infrastructure.persistence.mongodb.entity.OrganizationConfigMongoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for OrganizationConfigMongoEntity.
 * Infrastructure layer only — not visible to domain or application layers.
 */
public interface MongoOrganizationConfigRepository extends MongoRepository<OrganizationConfigMongoEntity, String> {
}
