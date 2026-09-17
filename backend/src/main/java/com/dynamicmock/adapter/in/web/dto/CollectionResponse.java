package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponse {
    private String id;
    private String name;
    private String userId;
    private String description;
    private List<CollectionItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionItemResponse {
        private String id;
        private String protocol;
        private String resourceId;
        private String name;
    }

    public static CollectionResponse from(Collection domain) {
        return CollectionResponse.builder()
                .id(domain.getId())
                .name(domain.getName())
                .userId(domain.getUserId())
                .description(domain.getDescription())
                .items(domain.getItems() == null ? null : domain.getItems().stream()
                        .map(item -> CollectionItemResponse.builder()
                                .id(item.getId())
                                .protocol(item.getProtocol())
                                .resourceId(item.getResourceId())
                                .name(item.getName())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
