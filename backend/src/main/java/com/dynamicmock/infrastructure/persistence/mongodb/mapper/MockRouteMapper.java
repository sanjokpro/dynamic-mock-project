package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.MockRouteMongoEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper between Domain MockRoute and MongoDB MockRouteMongoEntity.
 */
@Component
public class MockRouteMapper {

    public MockRoute toDomain(MockRouteMongoEntity entity) {
        if (entity == null) return null;
        return MockRoute.builder()
                .id(entity.getId())
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
                .version(entity.getVersion())
                .active(entity.getActive())
                .scenarioName(entity.getScenarioName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public MockRouteMongoEntity toEntity(MockRoute domain) {
        if (domain == null) return null;
        return MockRouteMongoEntity.builder()
                .id(domain.getId())
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
                .version(domain.getVersion())
                .active(domain.getActive())
                .scenarioName(domain.getScenarioName())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
