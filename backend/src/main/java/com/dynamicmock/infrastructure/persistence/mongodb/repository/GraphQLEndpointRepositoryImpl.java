package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.GraphQLEndpoint;
import com.dynamicmock.domain.port.out.GraphQLEndpointRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.GraphQLEndpointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GraphQLEndpointRepositoryImpl implements GraphQLEndpointRepository {

    private final GraphQLEndpointMongoRepository mongoRepository;
    private final GraphQLEndpointMapper mapper;

    @Override
    public List<GraphQLEndpoint> findByActiveTrue() {
        return mongoRepository.findByActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return mongoRepository.existsByName(name);
    }

    @Override
    public GraphQLEndpoint save(GraphQLEndpoint entity) {
        var mongoEntity = mapper.toEntity(entity);
        var saved = mongoRepository.save(mongoEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<GraphQLEndpoint> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GraphQLEndpoint> findAll() {
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
