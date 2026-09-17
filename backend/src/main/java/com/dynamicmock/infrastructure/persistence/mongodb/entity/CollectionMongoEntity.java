package com.dynamicmock.infrastructure.persistence.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "collections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionMongoEntity {
    @Id
    private String id;
    private String name;
    private String userId;
    private String description;
    private List<CollectionItemEntity> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionItemEntity {
        private String id;
        private String protocol;
        private String resourceId;
        private String name;
    }
}
