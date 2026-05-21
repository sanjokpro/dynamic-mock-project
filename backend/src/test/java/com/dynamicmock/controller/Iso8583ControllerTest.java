package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.Iso8583Controller;
import com.dynamicmock.adapter.in.web.dto.Iso8583EndpointRequest;
import com.dynamicmock.application.service.Iso8583Service;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
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
class Iso8583ControllerTest {

    @Mock
    private Iso8583Service iso8583Service;

    @InjectMocks
    private Iso8583Controller iso8583Controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(iso8583Controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllEndpoints_shouldReturnEndpoints() throws Exception {
        // Given
        List<Iso8583Endpoint> endpoints = Arrays.asList(
            createEndpoint("1", "Payment Switch"),
            createEndpoint("2", "ATM Simulator")
        );
        when(iso8583Service.findAll()).thenReturn(endpoints);

        // When & Then
        mockMvc.perform(get("/api/iso8583/endpoints"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));

        verify(iso8583Service).findAll();
    }

    @Test
    void getEndpoint_shouldReturnEndpoint() throws Exception {
        // Given
        Iso8583Endpoint endpoint = createEndpoint("1", "Payment Switch");
        when(iso8583Service.findById("1")).thenReturn(endpoint);

        // When & Then
        mockMvc.perform(get("/api/iso8583/endpoints/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Payment Switch"));

        verify(iso8583Service).findById("1");
    }

    @Test
    void createEndpoint_shouldCreateAndReturnEndpoint() throws Exception {
        // Given
        Iso8583EndpointRequest request = new Iso8583EndpointRequest();
        request.setName("Payment Switch");
        request.setPort(8583);

        Iso8583Endpoint createdEndpoint = createEndpoint("1", "Payment Switch");
        when(iso8583Service.create(any(Iso8583EndpointRequest.class))).thenReturn(createdEndpoint);

        // When & Then
        mockMvc.perform(post("/api/iso8583/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Payment Switch"));

        verify(iso8583Service).create(any(Iso8583EndpointRequest.class));
    }

    @Test
    void updateEndpoint_shouldUpdateAndReturnEndpoint() throws Exception {
        // Given
        Iso8583EndpointRequest request = new Iso8583EndpointRequest();
        request.setName("Updated Payment Switch");

        Iso8583Endpoint updatedEndpoint = createEndpoint("1", "Updated Payment Switch");
        when(iso8583Service.update(eq("1"), any(Iso8583EndpointRequest.class))).thenReturn(updatedEndpoint);

        // When & Then
        mockMvc.perform(put("/api/iso8583/endpoints/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Updated Payment Switch"));

        verify(iso8583Service).update(eq("1"), any(Iso8583EndpointRequest.class));
    }

    @Test
    void deleteEndpoint_shouldDeleteEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/iso8583/endpoints/1"))
                .andExpect(status().isNoContent());

        verify(iso8583Service).delete("1");
    }

    @Test
    void activateEndpoint_shouldActivateEndpoint() throws Exception {
        // Given
        Iso8583Endpoint activatedEndpoint = createEndpoint("1", "Payment Switch");
        activatedEndpoint.setActive(true);
        when(iso8583Service.activate("1")).thenReturn(activatedEndpoint);

        // When & Then
        mockMvc.perform(post("/api/iso8583/endpoints/1/activate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(true));

        verify(iso8583Service).activate("1");
    }

    @Test
    void deactivateEndpoint_shouldDeactivateEndpoint() throws Exception {
        // Given
        Iso8583Endpoint deactivatedEndpoint = createEndpoint("1", "Payment Switch");
        deactivatedEndpoint.setActive(false);
        when(iso8583Service.deactivate("1")).thenReturn(deactivatedEndpoint);

        // When & Then
        mockMvc.perform(post("/api/iso8583/endpoints/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(false));

        verify(iso8583Service).deactivate("1");
    }

    private Iso8583Endpoint createEndpoint(String id, String name) {
        return Iso8583Endpoint.builder()
                .id(id)
                .name(name)
                .port(8583)
                .active(false)
                .build();
    }
}
