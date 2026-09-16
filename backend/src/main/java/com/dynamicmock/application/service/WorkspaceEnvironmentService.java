package com.dynamicmock.application.service;

import com.dynamicmock.domain.entity.Environment;
import com.dynamicmock.domain.port.out.EnvironmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceEnvironmentService {

    private final EnvironmentRepository repository;

    public Environment createEnvironment(String name, Map<String, String> variables) {
        Environment environment = Environment.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .variables(variables)
                .isDefault(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return repository.save(environment);
    }

    public List<Environment> getAllEnvironments() {
        return repository.findAll();
    }

    public Environment getEnvironment(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Environment not found: " + id));
    }

    public Environment updateEnvironment(String id, Environment updatedEnvironment) {
        Environment existing = getEnvironment(id);
        existing.setName(updatedEnvironment.getName());
        existing.setVariables(updatedEnvironment.getVariables());
        existing.setIsDefault(updatedEnvironment.getIsDefault());
        existing.setUpdatedAt(LocalDateTime.now());
        
        if (Boolean.TRUE.equals(updatedEnvironment.getIsDefault())) {
            clearOtherDefaults(id);
        }
        
        return repository.save(existing);
    }

    public void deleteEnvironment(String id) {
        repository.deleteById(id);
    }

    private void clearOtherDefaults(String currentId) {
        repository.findAll().stream()
                .filter(e -> !e.getId().equals(currentId) && Boolean.TRUE.equals(e.getIsDefault()))
                .forEach(e -> {
                    e.setIsDefault(false);
                    repository.save(e);
                });
    }
}
