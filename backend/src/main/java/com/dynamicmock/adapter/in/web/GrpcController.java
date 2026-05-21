package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.GrpcEndpointRequest;
import com.dynamicmock.adapter.in.web.dto.GrpcEndpointResponse;
import com.dynamicmock.application.service.GrpcService;
import com.dynamicmock.domain.entity.GrpcEndpoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/grpc")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GrpcController {
    
    private final GrpcService grpcService;
    
    @GetMapping("/endpoints")
    public ResponseEntity<List<GrpcEndpointResponse>> getAllEndpoints() {
        List<GrpcEndpointResponse> endpoints = grpcService.findAll().stream()
                .map(GrpcEndpointResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(endpoints);
    }
    
    @GetMapping("/endpoints/{id}")
    public ResponseEntity<GrpcEndpointResponse> getEndpoint(@PathVariable String id) {
        GrpcEndpoint endpoint = grpcService.findById(id);
        return ResponseEntity.ok(GrpcEndpointResponse.from(endpoint));
    }
    
    @PostMapping("/endpoints")
    public ResponseEntity<GrpcEndpointResponse> createEndpoint(
            @Valid @RequestBody GrpcEndpointRequest request) {
        GrpcEndpoint endpoint = grpcService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GrpcEndpointResponse.from(endpoint));
    }
    
    @PutMapping("/endpoints/{id}")
    public ResponseEntity<GrpcEndpointResponse> updateEndpoint(
            @PathVariable String id,
            @Valid @RequestBody GrpcEndpointRequest request) {
        GrpcEndpoint endpoint = grpcService.update(id, request);
        return ResponseEntity.ok(GrpcEndpointResponse.from(endpoint));
    }
    
    @DeleteMapping("/endpoints/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String id) {
        grpcService.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/endpoints/{id}/activate")
    public ResponseEntity<GrpcEndpointResponse> activateEndpoint(@PathVariable String id) {
        GrpcEndpoint endpoint = grpcService.activate(id);
        return ResponseEntity.ok(GrpcEndpointResponse.from(endpoint));
    }
    
    @PostMapping("/endpoints/{id}/deactivate")
    public ResponseEntity<GrpcEndpointResponse> deactivateEndpoint(@PathVariable String id) {
        GrpcEndpoint endpoint = grpcService.deactivate(id);
        return ResponseEntity.ok(GrpcEndpointResponse.from(endpoint));
    }
}

