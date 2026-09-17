package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.Environment;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.EnvironmentMongoEntity;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentMapper {

    public Environment toDomain(EnvironmentMongoEntity entity) {
        if (entity == null) return null;
        return Environment.builder()
                .id(entity.getId())
                .name(entity.getName())
                .variables(entity.getVariables())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public EnvironmentMongoEntity toEntity(Environment domain) {
        if (domain == null) return null;
        return EnvironmentMongoEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .variables(domain.getVariables())
                .isDefault(domain.getIsDefault())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
