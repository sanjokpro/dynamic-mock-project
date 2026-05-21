package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.RouteVersion;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.RouteVersionMongoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Domain RouteVersion and MongoDB RouteVersionMongoEntity.
 */
@Component
public class RouteVersionMapper {

    public RouteVersion toDomain(RouteVersionMongoEntity entity) {
        if (entity == null) return null;
        return RouteVersion.builder()
                .id(entity.getId())
                .routeId(entity.getRouteId())
                .versionNumber(entity.getVersionNumber())
                .path(entity.getPath())
                .method(entity.getMethod())
                .matchers(entity.getMatchers())
                .responseTemplate(entity.getResponseTemplate())
                .responseStatus(entity.getResponseStatus())
                .responseHeaders(entity.getResponseHeaders())
                .preScript(entity.getPreScript())
                .postScript(entity.getPostScript())
                .scriptLanguage(entity.getScriptLanguage())
                .delayMs(entity.getDelayMs())
                .scenarioName(entity.getScenarioName())
                .changeDescription(entity.getChangeDescription())
                .changedBy(entity.getChangedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RouteVersionMongoEntity toEntity(RouteVersion domain) {
        if (domain == null) return null;
        return RouteVersionMongoEntity.builder()
                .id(domain.getId())
                .routeId(domain.getRouteId())
                .versionNumber(domain.getVersionNumber())
                .path(domain.getPath())
                .method(domain.getMethod())
                .matchers(domain.getMatchers())
                .responseTemplate(domain.getResponseTemplate())
                .responseStatus(domain.getResponseStatus())
                .responseHeaders(domain.getResponseHeaders())
                .preScript(domain.getPreScript())
                .postScript(domain.getPostScript())
                .scriptLanguage(domain.getScriptLanguage())
                .delayMs(domain.getDelayMs())
                .scenarioName(domain.getScenarioName())
                .changeDescription(domain.getChangeDescription())
                .changedBy(domain.getChangedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
