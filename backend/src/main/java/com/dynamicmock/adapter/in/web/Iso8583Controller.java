package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.Iso8583EndpointRequest;
import com.dynamicmock.adapter.in.web.dto.Iso8583EndpointResponse;
import com.dynamicmock.application.service.Iso8583Service;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/iso8583")
@RequiredArgsConstructor
public class Iso8583Controller {
    
    private final Iso8583Service iso8583Service;
    
    @GetMapping("/endpoints")
    public ResponseEntity<List<Iso8583EndpointResponse>> getAllEndpoints() {
        List<Iso8583EndpointResponse> endpoints = iso8583Service.findAll().stream()
                .map(Iso8583EndpointResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(endpoints);
    }
    
    @GetMapping("/endpoints/{id}")
    public ResponseEntity<Iso8583EndpointResponse> getEndpoint(@PathVariable String id) {
        Iso8583Endpoint endpoint = iso8583Service.findById(id);
        return ResponseEntity.ok(Iso8583EndpointResponse.from(endpoint));
    }
    
    @PostMapping("/endpoints")
    public ResponseEntity<Iso8583EndpointResponse> createEndpoint(
            @Valid @RequestBody Iso8583EndpointRequest request) {
        Iso8583Endpoint endpoint = iso8583Service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Iso8583EndpointResponse.from(endpoint));
    }
    
    @PutMapping("/endpoints/{id}")
    public ResponseEntity<Iso8583EndpointResponse> updateEndpoint(
            @PathVariable String id,
            @Valid @RequestBody Iso8583EndpointRequest request) {
        Iso8583Endpoint endpoint = iso8583Service.update(id, request);
        return ResponseEntity.ok(Iso8583EndpointResponse.from(endpoint));
    }
    
    @DeleteMapping("/endpoints/{id}")
    public ResponseEntity<Void> deleteEndpoint(@PathVariable String id) {
        iso8583Service.delete(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/endpoints/{id}/activate")
    public ResponseEntity<Iso8583EndpointResponse> activateEndpoint(@PathVariable String id) {
        Iso8583Endpoint endpoint = iso8583Service.activate(id);
        return ResponseEntity.ok(Iso8583EndpointResponse.from(endpoint));
    }
    
    @PostMapping("/endpoints/{id}/deactivate")
    public ResponseEntity<Iso8583EndpointResponse> deactivateEndpoint(@PathVariable String id) {
        Iso8583Endpoint endpoint = iso8583Service.deactivate(id);
        return ResponseEntity.ok(Iso8583EndpointResponse.from(endpoint));
    }

    /**
     * Upload a custom jPOS GenericPackager XML for an endpoint.
     * Validates the XML before persisting. Returns 400 on invalid XML.
     */
    @PostMapping("/endpoints/{id}/packager")
    public ResponseEntity<Iso8583EndpointResponse> uploadPackager(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "packager.xml";
        try {
            byte[] bytes = file.getBytes();
            Iso8583Endpoint endpoint = iso8583Service.uploadPackager(id, bytes, filename);
            return ResponseEntity.ok(Iso8583EndpointResponse.from(endpoint));
        } catch (IllegalArgumentException e) {
            log.warn("Packager upload rejected for endpoint {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Failed to read uploaded packager file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Remove the custom packager from an endpoint, reverting to the bundled default.
     */
    @DeleteMapping("/endpoints/{id}/packager")
    public ResponseEntity<Iso8583EndpointResponse> removePackager(@PathVariable String id) {
        Iso8583Endpoint endpoint = iso8583Service.removePackager(id);
        return ResponseEntity.ok(Iso8583EndpointResponse.from(endpoint));
    }
}

