package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.GrpcController;
import com.dynamicmock.adapter.in.web.dto.GrpcEndpointRequest;
import com.dynamicmock.application.service.GrpcService;
import com.dynamicmock.domain.entity.GrpcEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GrpcControllerTest {

    @Mock
    private GrpcService grpcService;

    @InjectMocks
    private GrpcController grpcController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(grpcController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllEndpoints_shouldReturnEndpoints() throws Exception {
        // Given
        List<GrpcEndpoint> endpoints = Arrays.asList(
            createEndpoint("1", "User Service"),
            createEndpoint("2", "Product Service")
        );
        when(grpcService.findAll()).thenReturn(endpoints);

        // When & Then
        mockMvc.perform(get("/api/grpc/endpoints"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));

        verify(grpcService).findAll();
    }

    @Test
    void getEndpoint_shouldReturnEndpoint() throws Exception {
        // Given
        GrpcEndpoint endpoint = createEndpoint("1", "User Service");
        when(grpcService.findById("1")).thenReturn(endpoint);

        // When & Then
        mockMvc.perform(get("/api/grpc/endpoints/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("User Service"));

        verify(grpcService).findById("1");
    }

    @Test
    void createEndpoint_shouldCreateAndReturnEndpoint() throws Exception {
        // Given
        GrpcEndpointRequest request = new GrpcEndpointRequest();
        request.setName("User Service");
        request.setServiceName("com.example.UserService");

        GrpcEndpoint createdEndpoint = createEndpoint("1", "User Service");
        when(grpcService.create(any(GrpcEndpointRequest.class))).thenReturn(createdEndpoint);

        // When & Then
        mockMvc.perform(post("/api/grpc/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("User Service"));

        verify(grpcService).create(any(GrpcEndpointRequest.class));
    }

    @Test
    void updateEndpoint_shouldUpdateAndReturnEndpoint() throws Exception {
        // Given
        GrpcEndpointRequest request = new GrpcEndpointRequest();
        request.setName("Updated User Service");
        request.setServiceName("com.example.UserService");
        request.setProtoSchema("syntax = \"proto3\";");
        request.setPort(9090);
        request.setMethods(java.util.Collections.emptyList());

        GrpcEndpoint updatedEndpoint = createEndpoint("1", "Updated User Service");
        when(grpcService.update(eq("1"), any(GrpcEndpointRequest.class))).thenReturn(updatedEndpoint);

        // When & Then
        mockMvc.perform(put("/api/grpc/endpoints/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Updated User Service"));

        verify(grpcService).update(eq("1"), any(GrpcEndpointRequest.class));
    }

    @Test
    void deleteEndpoint_shouldDeleteEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/grpc/endpoints/1"))
                .andExpect(status().isNoContent());

        verify(grpcService).delete("1");
    }

    @Test
    void activateEndpoint_shouldActivateEndpoint() throws Exception {
        // Given
        GrpcEndpoint activatedEndpoint = createEndpoint("1", "User Service");
        activatedEndpoint.setActive(true);
        when(grpcService.activate("1")).thenReturn(activatedEndpoint);

        // When & Then
        mockMvc.perform(post("/api/grpc/endpoints/1/activate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(true));

        verify(grpcService).activate("1");
    }

    @Test
    void deactivateEndpoint_shouldDeactivateEndpoint() throws Exception {
        // Given
        GrpcEndpoint deactivatedEndpoint = createEndpoint("1", "User Service");
        deactivatedEndpoint.setActive(false);
        when(grpcService.deactivate("1")).thenReturn(deactivatedEndpoint);

        // When & Then
        mockMvc.perform(post("/api/grpc/endpoints/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(false));

        verify(grpcService).deactivate("1");
    }

    private GrpcEndpoint createEndpoint(String id, String name) {
        return GrpcEndpoint.builder()
                .id(id)
                .name(name)
                .serviceName("com.example.UserService")
                .port(9090)
                .active(false)
                .build();
    }
}
