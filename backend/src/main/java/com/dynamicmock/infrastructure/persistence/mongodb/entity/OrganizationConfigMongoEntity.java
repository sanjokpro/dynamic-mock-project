package com.dynamicmock.infrastructure.persistence.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB persistence entity for organization branding configuration.
 * Stored in collection "organization_config" with singleton document id = "instance".
 *
 * Spring Data annotations MUST NOT appear in the domain layer.
 * This class is infrastructure-only.
 */
@Document(collection = "organization_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationConfigMongoEntity {

    @Id
    private String id;

    private String productName;
    private String organizationName;
    private String logoUrl;
    private String faviconUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String loginBackgroundUrl;
    private String footerText;
    private LocalDateTime updatedAt;
}
