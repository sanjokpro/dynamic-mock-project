package com.dynamicmock.infrastructure.persistence.mongodb.repository;

import com.dynamicmock.domain.entity.Collection;
import com.dynamicmock.domain.port.out.CollectionRepository;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.CollectionMongoEntity;
import com.dynamicmock.infrastructure.persistence.mongodb.mapper.CollectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CollectionRepositoryImpl implements CollectionRepository {

    private final MongoCollectionRepository mongoRepository;
    private final CollectionMapper mapper;

    @Override
    public Collection save(Collection collection) {
        CollectionMongoEntity entity = mapper.toEntity(collection);
        entity = mongoRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Collection> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Collection> findByUserId(String userId) {
        return mongoRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        mongoRepository.deleteById(id);
    }
}
