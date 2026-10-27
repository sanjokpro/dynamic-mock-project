package com.dynamicmock.infrastructure.persistence.mongodb.mapper;

import com.dynamicmock.domain.entity.OrganizationConfig;
import com.dynamicmock.infrastructure.persistence.mongodb.entity.OrganizationConfigMongoEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationConfigMapperTest {

    private OrganizationConfigMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrganizationConfigMapper();
    }

    @Test
    @DisplayName("toDomain and toEntity perform lossless bidirectional mapping")
    void testBidirectionalMapping() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 27, 12, 0);
        OrganizationConfig domain = OrganizationConfig.builder()
                .id(OrganizationConfig.SINGLETON_ID)
                .productName("FinTech Sandbox")
                .organizationName("FinTech Corp")
                .logoUrl("https://cdn.example.com/logo.svg")
                .faviconUrl("https://cdn.example.com/favicon.png")
                .primaryColor("#7c3aed")
                .secondaryColor("#f8fafc")
                .accentColor("#e2e8f0")
                .loginBackgroundUrl("https://cdn.example.com/bg.jpg")
                .footerText("FinTech Corp Legal Footer")
                .updatedAt(now)
                .build();

        OrganizationConfigMongoEntity entity = mapper.toEntity(domain);
        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo("instance");
        assertThat(entity.getProductName()).isEqualTo("FinTech Sandbox");
        assertThat(entity.getOrganizationName()).isEqualTo("FinTech Corp");
        assertThat(entity.getLogoUrl()).isEqualTo("https://cdn.example.com/logo.svg");
        assertThat(entity.getFaviconUrl()).isEqualTo("https://cdn.example.com/favicon.png");
        assertThat(entity.getPrimaryColor()).isEqualTo("#7c3aed");
        assertThat(entity.getSecondaryColor()).isEqualTo("#f8fafc");
        assertThat(entity.getAccentColor()).isEqualTo("#e2e8f0");
        assertThat(entity.getLoginBackgroundUrl()).isEqualTo("https://cdn.example.com/bg.jpg");
        assertThat(entity.getFooterText()).isEqualTo("FinTech Corp Legal Footer");
        assertThat(entity.getUpdatedAt()).isEqualTo(now);

        OrganizationConfig roundTrip = mapper.toDomain(entity);
        assertThat(roundTrip).isEqualTo(domain);
    }

    @Test
    @DisplayName("toDomain and toEntity return null when input is null")
    void testNullSafety() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
