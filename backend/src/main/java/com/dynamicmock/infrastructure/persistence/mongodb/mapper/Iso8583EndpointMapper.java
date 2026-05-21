package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.Iso8583EndpointMongoEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper between Domain Iso8583Endpoint and MongoDB Iso8583EndpointMongoEntity.
 */
@Component
public class Iso8583EndpointMapper {

    public Iso8583Endpoint toDomain(Iso8583EndpointMongoEntity entity) {
        if (entity == null) return null;
        return Iso8583Endpoint.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .port(entity.getPort())
                .isolatedPort(entity.getIsolatedPort())
                .interceptorScript(entity.getInterceptorScript())
                .interceptorScriptLanguage(entity.getInterceptorScriptLanguage())
                .interceptorEnabled(entity.getInterceptorEnabled())
                .customServerXml(entity.getCustomServerXml())
                .customXmlEnabled(entity.getCustomXmlEnabled())
                .headerLengthType(entity.getHeaderLengthType())
                .encoding(entity.getEncoding())
                .packagerConfig(entity.getPackagerConfig())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .mocks(entity.getMocks().stream().map(this::mockToDomain).collect(Collectors.toList()))
                .build();
    }

    private Iso8583Endpoint.Iso8583Mock mockToDomain(Iso8583EndpointMongoEntity.Iso8583MockEntity entity) {
        if (entity == null) return null;
        return Iso8583Endpoint.Iso8583Mock.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .mti(entity.getMti())
                .responseMti(entity.getResponseMti())
                .matchers(entity.getMatchers())
                .priority(entity.getPriority())
                .responseFields(entity.getResponseFields())
                .responseCode(entity.getResponseCode())
                .script(entity.getScript())
                .scriptLanguage(entity.getScriptLanguage())
                .scriptEnabled(entity.getScriptEnabled())
                .delayMs(entity.getDelayMs())
                .enabled(entity.getEnabled())
                .build();
    }

    public Iso8583EndpointMongoEntity toEntity(Iso8583Endpoint domain) {
        if (domain == null) return null;
        return Iso8583EndpointMongoEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .port(domain.getPort())
                .isolatedPort(domain.getIsolatedPort())
                .interceptorScript(domain.getInterceptorScript())
                .interceptorScriptLanguage(domain.getInterceptorScriptLanguage())
                .interceptorEnabled(domain.getInterceptorEnabled())
                .customServerXml(domain.getCustomServerXml())
                .customXmlEnabled(domain.getCustomXmlEnabled())
                .headerLengthType(domain.getHeaderLengthType())
                .encoding(domain.getEncoding())
                .packagerConfig(domain.getPackagerConfig())
                .active(domain.getActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .mocks(domain.getMocks().stream().map(this::mockToEntity).collect(Collectors.toList()))
                .build();
    }

    private Iso8583EndpointMongoEntity.Iso8583MockEntity mockToEntity(Iso8583Endpoint.Iso8583Mock domain) {
        if (domain == null) return null;
        return Iso8583EndpointMongoEntity.Iso8583MockEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .mti(domain.getMti())
                .responseMti(domain.getResponseMti())
                .matchers(domain.getMatchers())
                .priority(domain.getPriority())
                .responseFields(domain.getResponseFields())
                .responseCode(domain.getResponseCode())
                .script(domain.getScript())
                .scriptLanguage(domain.getScriptLanguage())
                .scriptEnabled(domain.getScriptEnabled())
                .delayMs(domain.getDelayMs())
                .enabled(domain.getEnabled())
                .build();
    }
}
