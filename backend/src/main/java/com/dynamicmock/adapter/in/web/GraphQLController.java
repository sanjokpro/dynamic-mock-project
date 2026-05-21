package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.GraphQLEndpointRequest;
import com.dynamicmock.adapter.in.web.dto.GraphQLEndpointResponse;
import com.dynamicmock.application.service.GraphQLService;
import com.dynamicmock.domain.entity.GraphQLEndpoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/graphql")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GraphQLController {
    
    private final GraphQLService graphQLService;
    
    @GetMapping("/endpoints")
    public ResponseEntity<List<GraphQLEndpointResponse>> getAllEndpoints() {
        List<GraphQLEndpointResponse> endpoints = graphQLService.findAll().stream()
                .map(GraphQLEndpointResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(endpoints);
    }
    
    @GetMapping("/endpoints/{id}")
    public ResponseEntity<GraphQLEndpointResponse> getEndpoint(@PathVariable String id) {
        GraphQLEndpoint endpoint = graphQLService.findById(id);
        return ResponseEntity.ok(GraphQLEndpointResponse.from(endpoint));
    }
    
    @PostMapping("/endpoints")
    public ResponseEntity<GraphQLEndpointResponse> createEndpoint(
            @Valid @RequestBody GraphQLEndpointRequest request) {
        GraphQLEndpoint endpoint = graphQLService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GraphQLEndpointResponse.from(endpoint));
    }
    
    @PutMapping("/endpoints/{id}")
    public ResponseEntity<GraphQLEndpointResponse> updateEndpoint(
            @PathVariable String id,
            @Valid @RequestBody GraphQLEndpointRequest request) {
        GraphQLEndpoint endpoint = graphQLService.update(id, request);
        return ResponseEntity.ok(GraphQLEndpointResponse.from(endpoint));
    }
    
    @DeleteMapping("/endpoints/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String id) {
        graphQLService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/endpoints/{id}/activate")
    public ResponseEntity<GraphQLEndpointResponse> activateEndpoint(@PathVariable String id) {
        GraphQLEndpoint endpoint = graphQLService.activate(id);
        return ResponseEntity.ok(GraphQLEndpointResponse.from(endpoint));
    }
    
    @PostMapping("/endpoints/{id}/deactivate")
    public ResponseEntity<GraphQLEndpointResponse> deactivateEndpoint(@PathVariable String id) {
        GraphQLEndpoint endpoint = graphQLService.deactivate(id);
        return ResponseEntity.ok(GraphQLEndpointResponse.from(endpoint));
    }
    
    /**
     * Execute a GraphQL query against an endpoint
     */
    @PostMapping("/endpoints/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeQuery(
            @PathVariable String id,
            @RequestBody GraphQLQueryRequest request) {
        Map<String, Object> result = graphQLService.execute(
                id,
                request.getQuery(),
                request.getOperationName(),
                request.getVariables()
        );
        return ResponseEntity.ok(result);
    }
    
    @lombok.Data
    public static class GraphQLQueryRequest {
        private String query;
        private String operationName;
        private Map<String, Object> variables;
    }
}

