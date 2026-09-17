package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.Collection;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.CollectionMongoEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CollectionMapper {

    public Collection toDomain(CollectionMongoEntity entity) {
        if (entity == null) return null;
        return Collection.builder()
                .id(entity.getId())
                .name(entity.getName())
                .userId(entity.getUserId())
                .description(entity.getDescription())
                .items(entity.getItems() == null ? null : entity.getItems().stream()
                        .map(item -> Collection.CollectionItem.builder()
                                .id(item.getId())
                                .protocol(item.getProtocol())
                                .resourceId(item.getResourceId())
                                .name(item.getName())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CollectionMongoEntity toEntity(Collection domain) {
        if (domain == null) return null;
        return CollectionMongoEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .userId(domain.getUserId())
                .description(domain.getDescription())
                .items(domain.getItems() == null ? null : domain.getItems().stream()
                        .map(item -> CollectionMongoEntity.CollectionItemEntity.builder()
                                .id(item.getId())
                                .protocol(item.getProtocol())
                                .resourceId(item.getResourceId())
                                .name(item.getName())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
