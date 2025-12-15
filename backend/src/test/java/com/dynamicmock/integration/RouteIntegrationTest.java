package com.dynamicmock.integration;

import com.dynamicmock.DynamicMockApplication;
import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.dynamicmock.adapter.in.web.dto.UpdateRouteRequest;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DynamicMockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=",
    "spring.data.redis.host=",
    "spring.data.redis.port="
})
class RouteIntegrationTest {

    @BeforeAll
    static void ensureDockerAvailable() {
        Assumptions.assumeTrue(
            DockerClientFactory.instance().isDockerAvailable(),
            "Skipping: Docker not available for Testcontainers"
        );
    }
    
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
    
    static final GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        mongoDBContainer.start();
        redisContainer.start();
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure containers are started before setting properties
        if (!mongoDBContainer.isRunning()) {
            mongoDBContainer.start();
        }
        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private MockRouteRepository repository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
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
        request1.setPath("/route1");
        request1.setMethod("GET");
        request1.setResponseTemplate("{\"test\":1}");
        
        CreateRouteRequest request2 = new CreateRouteRequest();
        request2.setPath("/route2");
        request2.setMethod("POST");
        request2.setResponseTemplate("{\"test\":2}");
        
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
        createRequest.setPath("/update-test");
        createRequest.setMethod("GET");
        createRequest.setResponseTemplate("{\"old\":\"value\"}");
        
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
        updateRequest.setResponseTemplate("{\"new\":\"value\"}");
        updateRequest.setResponseStatus(201);
        
        mockMvc.perform(put("/api/routes/" + createdRoute.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseStatus").value(201));
    }
    
    @Test
    void testDeleteRoute() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/delete-test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"test\":\"delete\"}");
        
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
    void testActivateAndCallMockEndpoint() throws Exception {
        // Create route
        CreateRouteRequest createRequest = new CreateRouteRequest();
        createRequest.setPath("/mock-test");
        createRequest.setMethod("GET");
        createRequest.setResponseTemplate("{\"message\":\"{{$randomString}}\",\"timestamp\":{{$timestamp}} }");
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
        
        // Activate route
        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        
        // Call mock endpoint
        mockMvc.perform(get("/mock/mock-test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testMockEndpointWithPathVariables() throws Exception {
        CreateRouteRequest createRequest = new CreateRouteRequest();
        createRequest.setPath("/users/{id}");
        createRequest.setMethod("GET");
        createRequest.setResponseTemplate("{\"userId\":\"{{request.pathVariables.id}}\",\"name\":\"{{$randomString}}\"}");
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
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
                .andExpect(jsonPath("$.name").exists());
    }
    
    @Test
    void testMockEndpointNotFound() throws Exception {
        mockMvc.perform(get("/mock/nonexistent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testDeactivateRoute() throws Exception {
        CreateRouteRequest createRequest = new CreateRouteRequest();
        createRequest.setPath("/deactivate-test");
        createRequest.setMethod("GET");
        createRequest.setResponseTemplate("{\"test\":\"value\"}");
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );
        
        // Activate
        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());
        
        // Deactivate
        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        
        // Mock endpoint should not be available
        mockMvc.perform(get("/mock/deactivate-test"))
                .andExpect(status().isNotFound());
    }
}

