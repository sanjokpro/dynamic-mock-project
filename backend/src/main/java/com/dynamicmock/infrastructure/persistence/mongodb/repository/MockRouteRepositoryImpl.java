package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.MockRouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the output port for MockRoute persistence.
 * Bridges the Domain Layer and Infrastructure Layer.
 */
@Repository
@RequiredArgsConstructor
public class MockRouteRepositoryImpl implements MockRouteRepository {

    private final MockRouteMongoRepository mongoRepository;
    private final MockRouteMapper mapper;

    @Override
    public List<MockRoute> findByActiveTrue() {
        return mongoRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MockRoute> findByPathAndMethodAndVersion(String path, String method, Integer version) {
        return mongoRepository.findByPathAndMethodAndVersion(path, method, version)
                .map(mapper::toDomain);
    }

    @Override
    public List<MockRoute> findByPathAndMethod(String path, String method) {
        return mongoRepository.findByPathAndMethod(path, method).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public MockRoute save(MockRoute entity) {
        var mongoEntity = mapper.toEntity(entity);
        var saved = mongoRepository.save(mongoEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MockRoute> findById(String s) {
        return mongoRepository.findById(s).map(mapper::toDomain);
    }

    @Override
    public boolean existsById(String s) {
        return mongoRepository.existsById(s);
    }

    @Override
    public List<MockRoute> findAll() {
        return mongoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return mongoRepository.count();
    }

    @Override
    public void deleteById(String s) {
        mongoRepository.deleteById(s);
    }

    @Override
    public void delete(MockRoute entity) {
        mongoRepository.delete(mapper.toEntity(entity));
    }

    @Override
    public void deleteAll() {
        mongoRepository.deleteAll();
    }
}
