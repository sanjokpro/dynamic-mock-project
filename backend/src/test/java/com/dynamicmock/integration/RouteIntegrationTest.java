package com.dynamicmock.integration;

import com.dynamicmock.DynamicMockApplication;
import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.in.web.dto.UpdateRouteRequest;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = DynamicMockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration,org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration"})
class RouteIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockRouteRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        repository.deleteAll();
    }

    @Test
    void testCreateAndGetRoute() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"message\":\"Hello World\"}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.path").value("/test"))
                .andExpect(jsonPath("$.method").value("GET"))
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );

        mockMvc.perform(get("/api/routes/" + createdRoute.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdRoute.getId()))
                .andExpect(jsonPath("$.path").value("/test"));
    }

    @Test
    void testListRoutes() throws Exception {
        CreateRouteRequest request1 = new CreateRouteRequest();
        request1.setPath("/test1");
        request1.setMethod("GET");
        request1.setResponseTemplate("{\"message\":\"Test 1\"}");
        request1.setResponseStatus(200);

        CreateRouteRequest request2 = new CreateRouteRequest();
        request2.setPath("/test2");
        request2.setMethod("POST");
        request2.setResponseTemplate("{\"message\":\"Test 2\"}");
        request2.setResponseStatus(201);

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testUpdateRoute() throws Exception {
        CreateRouteRequest createRequest = new CreateRouteRequest();
        createRequest.setPath("/test");
        createRequest.setMethod("GET");
        createRequest.setResponseTemplate("{\"message\":\"Original\"}");
        createRequest.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );

        UpdateRouteRequest updateRequest = new UpdateRouteRequest();
        updateRequest.setPath("/test-updated");
        updateRequest.setMethod("POST");
        updateRequest.setResponseTemplate("{\"message\":\"Updated\"}");
        updateRequest.setResponseStatus(201);

        mockMvc.perform(put("/api/routes/" + createdRoute.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("/test-updated"))
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.responseStatus").value(201));
    }

    @Test
    void testDeleteRoute() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"message\":\"Test\"}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );

        mockMvc.perform(delete("/api/routes/" + createdRoute.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/routes/" + createdRoute.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeactivateRoute() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"message\":\"Test\"}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/routes/" + createdRoute.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void testActivateAndCallMockEndpoint() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/test-call");
        request.setMethod("GET");
        request.setResponseTemplate("{\"message\":\"Mock Response\"}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/mock/test-call"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mock Response"));
    }

    @Test
    void testMockEndpointWithPathVariables() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/users/{id}");
        request.setMethod("GET");
        request.setResponseTemplate("{\"userId\":\"{{id}}\",\"message\":\"User data\"}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/mock/users/123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("123"))
                .andExpect(jsonPath("$.message").value("User data"));
    }

    @Test
    void testMockEndpointNotFound() throws Exception {
        mockMvc.perform(get("/mock/nonexistent"))
                .andExpect(status().isNotFound());
    }
}