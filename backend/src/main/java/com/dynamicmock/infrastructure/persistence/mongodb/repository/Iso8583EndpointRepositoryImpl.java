package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.port.out.Iso8583EndpointRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.Iso8583EndpointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class Iso8583EndpointRepositoryImpl implements Iso8583EndpointRepository {

    private final Iso8583EndpointMongoRepository mongoRepository;
    private final Iso8583EndpointMapper mapper;

    @Override
    public List<Iso8583Endpoint> findByActiveTrue() {
        return mongoRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByPortAndIsolatedPortTrue(Integer port) {
        return mongoRepository.existsByPortAndIsolatedPortTrue(port);
    }

    @Override
    public Iso8583Endpoint save(Iso8583Endpoint entity) {
        var mongoEntity = mapper.toEntity(entity);
        var saved = mongoRepository.save(mongoEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Iso8583Endpoint> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Iso8583Endpoint> findAll() {
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
