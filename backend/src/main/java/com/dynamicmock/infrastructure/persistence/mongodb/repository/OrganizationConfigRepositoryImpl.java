package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.domain.port.out.OrganizationConfigRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.OrganizationConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MongoDB adapter implementing the domain's OrganizationConfigRepository port.
 * Translates between domain entities and infrastructure persistence entities.
 */
@Repository
@RequiredArgsConstructor
public class OrganizationConfigRepositoryImpl implements OrganizationConfigRepository {

    private final MongoOrganizationConfigRepository mongoRepository;
    private final OrganizationConfigMapper mapper;

    @Override
    public Optional<OrganizationConfig> findConfig() {
        return mongoRepository.findById(OrganizationConfig.SINGLETON_ID)
                .map(mapper::toDomain);
    }

    @Override
    public OrganizationConfig save(OrganizationConfig config) {
        config.setId(OrganizationConfig.SINGLETON_ID);
        var entity = mapper.toEntity(config);
        var saved = mongoRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteConfig() {
        mongoRepository.deleteById(OrganizationConfig.SINGLETON_ID);
    }
}
