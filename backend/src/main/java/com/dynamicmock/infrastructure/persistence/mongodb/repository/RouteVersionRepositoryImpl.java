package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.RouteVersion;
import com.dynamicmock.domain.port.out.RouteVersionRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.RouteVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RouteVersionRepositoryImpl implements RouteVersionRepository {

    private final RouteVersionMongoRepository mongoRepository;
    private final RouteVersionMapper mapper;

    @Override
    public List<RouteVersion> findByRouteIdOrderByVersionNumberDesc(String routeId) {
        return mongoRepository.findByRouteIdOrderByVersionNumberDesc(routeId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RouteVersion> findByRouteIdAndVersionNumber(String routeId, int versionNumber) {
        return mongoRepository.findByRouteIdAndVersionNumber(routeId, versionNumber)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<RouteVersion> findTopByRouteIdOrderByVersionNumberDesc(String routeId) {
        return mongoRepository.findTopByRouteIdOrderByVersionNumberDesc(routeId)
                .map(mapper::toDomain);
    }

    @Override
    public RouteVersion save(RouteVersion entity) {
        var mongoEntity = mapper.toEntity(entity);
        var saved = mongoRepository.save(mongoEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RouteVersion> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<RouteVersion> findAll() {
        return mongoRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        mongoRepository.deleteById(id);
    }

    @Override
    public void deleteByRouteId(String routeId) {
        mongoRepository.deleteByRouteId(routeId);
    }

    @Override
    public void deleteAll() {
        mongoRepository.deleteAll();
    }
}
