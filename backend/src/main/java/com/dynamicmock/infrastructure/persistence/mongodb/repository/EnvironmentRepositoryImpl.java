package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.Environment;
import com.dynamicmock.domain.port.out.EnvironmentRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.EnvironmentMongoEntity;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.EnvironmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class EnvironmentRepositoryImpl implements EnvironmentRepository {

    private final MongoEnvironmentRepository mongoRepository;
    private final EnvironmentMapper mapper;

    @Override
    public Environment save(Environment environment) {
        EnvironmentMongoEntity entity = mapper.toEntity(environment);
        entity = mongoRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Environment> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Environment> findAll() {
        return mongoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        mongoRepository.deleteById(id);
    }
}
