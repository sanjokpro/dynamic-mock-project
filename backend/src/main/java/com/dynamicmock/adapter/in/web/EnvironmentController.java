package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.EnvironmentRequest;
import com.dynamicmock.adapter.in.web.dto.EnvironmentResponse;
import com.dynamicmock.application.service.WorkspaceEnvironmentService;
import com.dynamicmock.domain.entity.Environment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final WorkspaceEnvironmentService service;

    @PostMapping
    public ResponseEntity<EnvironmentResponse> create(@RequestBody EnvironmentRequest request) {
        Environment environment = service.createEnvironment(request.getName(), request.getVariables());
        return ResponseEntity.ok(EnvironmentResponse.from(environment));
    }

    @GetMapping
    public ResponseEntity<List<EnvironmentResponse>> getAll() {
        List<EnvironmentResponse> responses = service.getAllEnvironments().stream()
                .map(EnvironmentResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> getOne(@PathVariable String id) {
        return ResponseEntity.ok(EnvironmentResponse.from(service.getEnvironment(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentResponse> update(@PathVariable String id, @RequestBody EnvironmentRequest request) {
        Environment updated = Environment.builder()
                .name(request.getName())
                .variables(request.getVariables())
                .isDefault(request.getIsDefault())
                .build();
        return ResponseEntity.ok(EnvironmentResponse.from(service.updateEnvironment(id, updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.deleteEnvironment(id);
        return ResponseEntity.ok().build();
    }
}
