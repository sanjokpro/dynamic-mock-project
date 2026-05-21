package com.dynamicmock.controller;

import com.dynamicmock.adapter.in.web.GraphQLController;
import com.dynamicmock.adapter.in.web.dto.GraphQLEndpointRequest;
import com.dynamicmock.application.service.GraphQLService;
import com.dynamicmock.domain.entity.GraphQLEndpoint;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GraphQLControllerTest {

    @Mock
    private GraphQLService graphQLService;

    @InjectMocks
    private GraphQLController graphQLController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(graphQLController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllEndpoints_shouldReturnEndpoints() throws Exception {
        // Given
        List<GraphQLEndpoint> endpoints = Arrays.asList(
            createEndpoint("1", "User Service"),
            createEndpoint("2", "Product Service")
        );
        when(graphQLService.findAll()).thenReturn(endpoints);

        // When & Then
        mockMvc.perform(get("/api/graphql/endpoints"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[1].id").value("2"));

        verify(graphQLService).findAll();
    }

    @Test
    void getEndpoint_shouldReturnEndpoint() throws Exception {
        // Given
        GraphQLEndpoint endpoint = createEndpoint("1", "User Service");
        when(graphQLService.findById("1")).thenReturn(endpoint);

        // When & Then
        mockMvc.perform(get("/api/graphql/endpoints/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("User Service"));

        verify(graphQLService).findById("1");
    }

    @Test
    void createEndpoint_shouldCreateAndReturnEndpoint() throws Exception {
        // Given
        GraphQLEndpointRequest request = new GraphQLEndpointRequest();
        request.setName("User Service");
        request.setSchema("type Query { user(id: ID!): User }");

        GraphQLEndpoint createdEndpoint = createEndpoint("1", "User Service");
        when(graphQLService.create(any(GraphQLEndpointRequest.class))).thenReturn(createdEndpoint);

        // When & Then
        mockMvc.perform(post("/api/graphql/endpoints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("User Service"));

        verify(graphQLService).create(any(GraphQLEndpointRequest.class));
    }

    @Test
    void updateEndpoint_shouldUpdateAndReturnEndpoint() throws Exception {
        // Given
        GraphQLEndpointRequest request = new GraphQLEndpointRequest();
        request.setName("Updated User Service");
        request.setSchema("type Query { hello: String }");

        GraphQLEndpoint updatedEndpoint = createEndpoint("1", "Updated User Service");
        when(graphQLService.update(eq("1"), any(GraphQLEndpointRequest.class))).thenReturn(updatedEndpoint);

        // When & Then
        mockMvc.perform(put("/api/graphql/endpoints/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("Updated User Service"));

        verify(graphQLService).update(eq("1"), any(GraphQLEndpointRequest.class));
    }

    @Test
    void deleteEndpoint_shouldDeleteEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/graphql/endpoints/1"))
                .andExpect(status().isNoContent());

        verify(graphQLService).delete("1");
    }

    @Test
    void activateEndpoint_shouldActivateEndpoint() throws Exception {
        // Given
        GraphQLEndpoint activatedEndpoint = createEndpoint("1", "User Service");
        activatedEndpoint.setActive(true);
        when(graphQLService.activate("1")).thenReturn(activatedEndpoint);

        // When & Then
        mockMvc.perform(post("/api/graphql/endpoints/1/activate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(true));

        verify(graphQLService).activate("1");
    }

    @Test
    void deactivateEndpoint_shouldDeactivateEndpoint() throws Exception {
        // Given
        GraphQLEndpoint deactivatedEndpoint = createEndpoint("1", "User Service");
        deactivatedEndpoint.setActive(false);
        when(graphQLService.deactivate("1")).thenReturn(deactivatedEndpoint);

        // When & Then
        mockMvc.perform(post("/api/graphql/endpoints/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.active").value(false));

        verify(graphQLService).deactivate("1");
    }

    @Test
    void executeQuery_shouldExecuteQueryAndReturnResult() throws Exception {
        // Given
        String query = "{ user(id: \"123\") { name } }";
        String expectedResult = "{\"data\":{\"user\":{\"name\":\"John\"}}}";

        when(graphQLService.execute(eq("1"), anyString(), any(), any()))
            .thenReturn(new ObjectMapper().readValue(expectedResult, Map.class));

        GraphQLController.GraphQLQueryRequest queryRequest = new GraphQLController.GraphQLQueryRequest();
        queryRequest.setQuery(query);

        // When & Then
        mockMvc.perform(post("/api/graphql/endpoints/1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedResult));

        verify(graphQLService).execute(eq("1"), anyString(), any(), any());
    }

    private GraphQLEndpoint createEndpoint(String id, String name) {
        return GraphQLEndpoint.builder()
                .id(id)
                .name(name)
                .schema("type Query { user(id: ID!): User }")
                .active(false)
                .build();
    }
}
