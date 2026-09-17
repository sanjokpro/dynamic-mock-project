package com.dynamicmock.application.service;

import com.dynamicmock.domain.entity.Collection;
import com.dynamicmock.domain.port.out.CollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceCollectionService {

    private final CollectionRepository repository;

    public Collection createCollection(String name, String userId, String description) {
        Collection collection = Collection.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .userId(userId)
                .description(description)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.save(collection);
    }

    public List<Collection> getCollectionsByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    public Collection getCollection(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Collection not found: " + id));
    }

    public Collection updateCollection(String id, Collection updatedCollection) {
        Collection existing = getCollection(id);
        existing.setName(updatedCollection.getName());
        existing.setDescription(updatedCollection.getDescription());
        existing.setItems(updatedCollection.getItems());
        existing.setUpdatedAt(LocalDateTime.now());
        return repository.save(existing);
    }

    public void deleteCollection(String id) {
        repository.deleteById(id);
    }

    public Collection addItemToCollection(String collectionId, Collection.CollectionItem item) {
        Collection collection = getCollection(collectionId);
        if (collection.getItems() == null) {
            collection.setItems(new java.util.ArrayList<>());
        }
        if (item.getId() == null) {
            item.setId(UUID.randomUUID().toString());
        }
        collection.getItems().add(item);
        collection.setUpdatedAt(LocalDateTime.now());
        return repository.save(collection);
    }

    public String exportCollection(String id) {
        try {
            Collection collection = getCollection(id);
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(collection);
        } catch (Exception e) {
            throw new RuntimeException("Export failed", e);
        }
    }

    public Collection importCollection(String json, String userId) {
        try {
            Collection collection = new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .readValue(json, Collection.class);
            collection.setId(UUID.randomUUID().toString());
            collection.setUserId(userId);
            collection.setCreatedAt(LocalDateTime.now());
            collection.setUpdatedAt(LocalDateTime.now());
            return repository.save(collection);
        } catch (Exception e) {
            throw new RuntimeException("Import failed", e);
        }
    }
}
