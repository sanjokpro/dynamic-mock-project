package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.dynamicmock.domain.port.out.GrpcEndpointRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.GrpcEndpointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GrpcEndpointRepositoryImpl implements GrpcEndpointRepository {

    private final GrpcEndpointMongoRepository mongoRepository;
    private final GrpcEndpointMapper mapper;

    @Override
    public List<GrpcEndpoint> findByActiveTrue() {
        return mongoRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByServiceName(String serviceName) {
        return mongoRepository.existsByServiceName(serviceName);
    }

    @Override
    public boolean existsByPort(Integer port) {
        return mongoRepository.existsByPort(port);
    }

    @Override
    public GrpcEndpoint save(GrpcEndpoint entity) {
        var mongoEntity = mapper.toEntity(entity);
        var saved = mongoRepository.save(mongoEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<GrpcEndpoint> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GrpcEndpoint> findAll() {
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
