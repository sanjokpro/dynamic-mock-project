package com.dynamicmock.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collection {
    private String id;
    private String name;
    private String userId;
    private String description;
    private List<CollectionItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionItem {
        private String id;
        private String protocol; // HTTP, GRAPHQL, GRPC, ISO8583
        private String resourceId;
        private String name;
    }
}
