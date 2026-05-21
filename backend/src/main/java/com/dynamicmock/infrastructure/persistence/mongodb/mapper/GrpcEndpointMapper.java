package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.GrpcEndpointMongoEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper between Domain GrpcEndpoint and MongoDB GrpcEndpointMongoEntity.
 */
@Component
public class GrpcEndpointMapper {

    public GrpcEndpoint toDomain(GrpcEndpointMongoEntity entity) {
        if (entity == null) return null;
        return GrpcEndpoint.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .serviceName(entity.getServiceName())
                .protoSchema(entity.getProtoSchema())
                .port(entity.getPort())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .methods(entity.getMethods().stream().map(this::methodToDomain).collect(Collectors.toList()))
                .build();
    }

    private GrpcEndpoint.MethodConfig methodToDomain(GrpcEndpointMongoEntity.MethodConfigEntity entity) {
        if (entity == null) return null;
        return GrpcEndpoint.MethodConfig.builder()
                .methodName(entity.getMethodName())
                .methodType(entity.getMethodType())
                .responseTemplate(entity.getResponseTemplate())
                .streamResponses(entity.getStreamResponses())
                .script(entity.getScript())
                .scriptLanguage(entity.getScriptLanguage())
                .delayMs(entity.getDelayMs())
                .statusCode(entity.getStatusCode())
                .errorMessage(entity.getErrorMessage())
                .matchers(entity.getMatchers())
                .build();
    }

    public GrpcEndpointMongoEntity toEntity(GrpcEndpoint domain) {
        if (domain == null) return null;
        return GrpcEndpointMongoEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .serviceName(domain.getServiceName())
                .protoSchema(domain.getProtoSchema())
                .port(domain.getPort())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .methods(domain.getMethods().stream().map(this::methodToEntity).collect(Collectors.toList()))
                .build();
    }

    private GrpcEndpointMongoEntity.MethodConfigEntity methodToEntity(GrpcEndpoint.MethodConfig domain) {
        if (domain == null) return null;
        return GrpcEndpointMongoEntity.MethodConfigEntity.builder()
                .methodName(domain.getMethodName())
                .methodType(domain.getMethodType())
                .responseTemplate(domain.getResponseTemplate())
                .streamResponses(domain.getStreamResponses())
                .script(domain.getScript())
                .scriptLanguage(domain.getScriptLanguage())
                .delayMs(domain.getDelayMs())
                .statusCode(domain.getStatusCode())
                .errorMessage(domain.getErrorMessage())
                .matchers(domain.getMatchers())
                .build();
    }
}
