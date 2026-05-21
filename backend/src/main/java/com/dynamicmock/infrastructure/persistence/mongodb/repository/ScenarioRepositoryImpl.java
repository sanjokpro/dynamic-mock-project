package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.Scenario;
import com.dynamicmock.domain.port.out.ScenarioRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.ScenarioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ScenarioRepositoryImpl implements ScenarioRepository {

    private final ScenarioMongoRepository mongoRepository;
    private final ScenarioMapper mapper;

    @Override
    public Optional<Scenario> findByName(String name) {
        return mongoRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return mongoRepository.existsByName(name);
    }

    @Override
    public List<Scenario> findByActiveTrue() {
        return mongoRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public <S extends Scenario> S save(S entity) {
        var mongoEntity = mapper.toEntity(entity);
        var saved = mongoRepository.save(mongoEntity);
        return (S) mapper.toDomain(saved);
    }

    @Override
    public Optional<Scenario> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Scenario> findAll() {
        return mongoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        mongoRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        mongoRepository.deleteAll();
    }
}
