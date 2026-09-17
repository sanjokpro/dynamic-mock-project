package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.CollectionRequest;
import com.dynamicmock.adapter.in.web.dto.CollectionResponse;
import com.dynamicmock.application.service.WorkspaceCollectionService;
import com.dynamicmock.domain.entity.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final WorkspaceCollectionService service;

    @PostMapping
    public ResponseEntity<CollectionResponse> create(@RequestBody CollectionRequest request) {
        Collection collection = service.createCollection(request.getName(), request.getUserId(), request.getDescription());
        return ResponseEntity.ok(CollectionResponse.from(collection));
    }

    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAll(@RequestParam String userId) {
        List<CollectionResponse> responses = service.getCollectionsByUserId(userId).stream()
                .map(CollectionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getOne(@PathVariable String id) {
        return ResponseEntity.ok(CollectionResponse.from(service.getCollection(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> update(@PathVariable String id, @RequestBody CollectionRequest request) {
        Collection updated = Collection.builder()
                .name(request.getName())
                .description(request.getDescription())
                .items(request.getItems() == null ? null : request.getItems().stream()
                        .map(item -> Collection.CollectionItem.builder()
                                .id(item.getId())
                                .protocol(item.getProtocol())
                                .resourceId(item.getResourceId())
                                .name(item.getName())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(CollectionResponse.from(service.updateCollection(id, updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteCollection(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<CollectionResponse> addItem(@PathVariable String id, @RequestBody CollectionRequest.CollectionItemRequest itemRequest) {
        Collection.CollectionItem item = Collection.CollectionItem.builder()
                .protocol(itemRequest.getProtocol())
                .resourceId(itemRequest.getResourceId())
                .name(itemRequest.getName())
                .build();
        return ResponseEntity.ok(CollectionResponse.from(service.addItemToCollection(id, item)));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<String> export(@PathVariable String id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"collection-" + id + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.exportCollection(id));
    }

    @PostMapping("/import")
    public ResponseEntity<CollectionResponse> importCollection(@RequestBody String json, @RequestParam String userId) {
        return ResponseEntity.ok(CollectionResponse.from(service.importCollection(json, userId)));
    }
}
