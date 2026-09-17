package com.dynamicmock.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequest {
    private String name;
    private String userId;
    private String description;
    private List<CollectionItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionItemRequest {
        private String id;
        private String protocol;
        private String resourceId;
        private String name;
    }
}
