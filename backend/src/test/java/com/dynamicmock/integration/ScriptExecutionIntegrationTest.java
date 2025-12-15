package com.dynamicmock.integration;

import com.dynamicmock.DynamicMockApplication;
import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.adapter.in.web.dto.RouteResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.DockerClientFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DynamicMockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=",
    "spring.data.redis.host=",
    "spring.data.redis.port="
})
class ScriptExecutionIntegrationTest {

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
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379).toString());
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void testPreScriptExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/script-test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"status\":\"{{vars.status}}\",\"message\":\"{{vars.message}}\"}");
        request.setPreScript("response.status = 201; vars.status = 'modified'; vars.message = 'Hello from script';");
        request.setScriptLanguage("js");
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RouteResponse route = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );
        
        mockMvc.perform(post("/api/routes/" + route.getId() + "/activate"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/mock/script-test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("modified"))
                .andExpect(jsonPath("$.message").value("Hello from script"));
    }
    
    @Test
    void testPostScriptExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/post-script-test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"initial\":\"value\" }");
        request.setPostScript("response.body = JSON.stringify({modified: true, timestamp: Date.now()});");
        request.setScriptLanguage("js");
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RouteResponse route = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );
        
        mockMvc.perform(post("/api/routes/" + route.getId() + "/activate"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/mock/post-script-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modified").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void testPythonScriptExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/python-test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"language\":\"python\"}");
        request.setPreScript("response['status'] = 202\nvars['lang'] = 'python'");
        request.setScriptLanguage("python");
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RouteResponse route = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );
        
        mockMvc.perform(post("/api/routes/" + route.getId() + "/activate"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/mock/python-test"))
                .andExpect(status().isAccepted());
    }
    
    @Test
    void testScriptWithQueryParams() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/query-test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"userId\":\"{{vars.userId}}\"}");
        request.setPreScript("vars.userId = request.queryParams.userId || 'default';");
        request.setScriptLanguage("js");
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RouteResponse route = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );
        
        mockMvc.perform(post("/api/routes/" + route.getId() + "/activate"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/mock/query-test?userId=12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("12345"));
    }
    
    @Test
    void testDelayExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/delay-test");
        request.setMethod("GET");
        request.setResponseTemplate("{\"test\":\"delay\"}");
        request.setDelayMs(100);
        
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RouteResponse route = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            RouteResponse.class
        );
        
        mockMvc.perform(post("/api/routes/" + route.getId() + "/activate"))
                .andExpect(status().isOk());
        
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/mock/delay-test"))
                .andExpect(status().isOk());
        long endTime = System.currentTimeMillis();
        
        assertTrue(endTime - startTime >= 100);
    }
}

