package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.GraphQLEndpoint;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.GraphQLEndpointMongoEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper between Domain GraphQLEndpoint and MongoDB GraphQLEndpointMongoEntity.
 */
@Component
public class GraphQLEndpointMapper {

    public GraphQLEndpoint toDomain(GraphQLEndpointMongoEntity entity) {
        if (entity == null) return null;
        return GraphQLEndpoint.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .schema(entity.getSchema())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .resolvers(entity.getResolvers().stream().map(this::resolverToDomain).collect(Collectors.toList()))
                .build();
    }

    private GraphQLEndpoint.ResolverConfig resolverToDomain(GraphQLEndpointMongoEntity.ResolverConfigEntity entity) {
        if (entity == null) return null;
        return GraphQLEndpoint.ResolverConfig.builder()
                .operationType(entity.getOperationType())
                .fieldName(entity.getFieldName())
                .responseTemplate(entity.getResponseTemplate())
                .script(entity.getScript())
                .scriptLanguage(entity.getScriptLanguage())
                .delayMs(entity.getDelayMs())
                .matchers(entity.getMatchers())
                .build();
    }

    public GraphQLEndpointMongoEntity toEntity(GraphQLEndpoint domain) {
        if (domain == null) return null;
        return GraphQLEndpointMongoEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .schema(domain.getSchema())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .resolvers(domain.getResolvers().stream().map(this::resolverToEntity).collect(Collectors.toList()))
                .build();
    }

    private GraphQLEndpointMongoEntity.ResolverConfigEntity resolverToEntity(GraphQLEndpoint.ResolverConfig domain) {
        if (domain == null) return null;
        return GraphQLEndpointMongoEntity.ResolverConfigEntity.builder()
                .operationType(domain.getOperationType())
                .fieldName(domain.getFieldName())
                .responseTemplate(domain.getResponseTemplate())
                .script(domain.getScript())
                .scriptLanguage(domain.getScriptLanguage())
                .delayMs(domain.getDelayMs())
                .matchers(domain.getMatchers())
                .build();
    }
}
