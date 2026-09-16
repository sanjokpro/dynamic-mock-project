package com.dynamicmock.integration;

import com.dynamicmock.DynamicMockApplication;
import com.dynamicmock.adapter.in.web.dto.CreateRouteRequest;
import com.dynamicmock.domain.port.out.MockRouteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.dynamicmock.application.service.AsyncWebhookService;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@SpringBootTest(classes = DynamicMockApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration,org.springframework.boot.autoconfigure.graphql.observation.GraphQlObservationAutoConfiguration",
        "graalvm.script.allow-host-access=true"
    })
@ActiveProfiles("test")
class ScriptExecutionIntegrationTest {

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

    @MockitoBean
    private AsyncWebhookService asyncWebhookService;

    @Test
    void testPreScriptExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/script-test");
        request.setMethod("GET");
        request.setResponseTemplate("{{#script}}return { message: 'Hello from script', timestamp: new Date().toISOString() };{{/script}}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/mock/script-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello from script"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testPostScriptExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/post-script");
        request.setMethod("POST");
        request.setResponseTemplate("{{#script}}return { received: request.body, method: request.method };{{/script}}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/mock/post-script")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"test\":\"data\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.received.test").value("data"));
    }

    @Test
    void testScriptWithQueryParams() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/query-script");
        request.setMethod("GET");
        request.setResponseTemplate("{{#script}}return { params: request.query, paramCount: Object.keys(request.query).length };{{/script}}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/mock/query-script?param1=value1&param2=value2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paramCount").value(2))
                .andExpect(jsonPath("$.params.param1").value("value1"))
                .andExpect(jsonPath("$.params.param2").value("value2"));
    }

    @Test
    void testDelayExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/delay-script");
        request.setMethod("GET");
        request.setResponseTemplate("{{#script}}Java.type('java.lang.Thread').sleep(100); return { delayed: true, timestamp: new Date().toISOString() };{{/script}}");
        request.setResponseStatus(200);
        request.setDelayMs(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/mock/delay-script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delayed").value(true));
        long endTime = System.currentTimeMillis();

        // Should take at least 300ms (100ms script + 200ms delay)
        org.junit.jupiter.api.Assertions.assertTrue(endTime - startTime >= 300);
    }

    @Test
    void testPythonScriptExecution() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/python-script");
        request.setMethod("GET");
        request.setResponseTemplate("{{#script language=\"python\"}}return {\"message\": \"Hello from Python\", \"language\": \"python\"}{{/script}}");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/mock/python-script"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello from Python"))
                .andExpect(jsonPath("$.language").value("python"));
    }

    @Test
    void testWebhookExtractionFromJs() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/webhook-script");
        request.setMethod("POST");
        request.setPostScript("webhook = { url: 'http://example.com/callback', method: 'POST', body: '{\"test\":123}', delayMs: 1000 }; response.body = '{\"status\":\"ok\"}';");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/mock/webhook-script")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> webhookCaptor = ArgumentCaptor.forClass((Class) Map.class);

        verify(asyncWebhookService, timeout(1000)).triggerWebhook(eq(createdRoute.getId()), webhookCaptor.capture());

        Map<String, Object> webhookConfig = webhookCaptor.getValue();
        assertEquals("http://example.com/callback", webhookConfig.get("url"));
        assertEquals("POST", webhookConfig.get("method"));
        assertEquals("{\"test\":123}", webhookConfig.get("body"));
        assertEquals(1000, ((Number) webhookConfig.get("delayMs")).intValue());
    }

    @Test
    void testWebhookExtractionFromPython() throws Exception {
        CreateRouteRequest request = new CreateRouteRequest();
        request.setPath("/webhook-script-py");
        request.setMethod("POST");
        request.setPostScript("webhook = { 'url': 'http://example.com/callback-py', 'method': 'POST', 'body': '{\"test\":123}', 'delayMs': 1000 }\nresponse = {'body': '{\"status\":\"ok\"}'}");
        request.setScriptLanguage("python");
        request.setResponseStatus(200);

        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        com.dynamicmock.adapter.in.web.dto.RouteResponse createdRoute = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            com.dynamicmock.adapter.in.web.dto.RouteResponse.class
        );

        mockMvc.perform(post("/api/routes/" + createdRoute.getId() + "/activate"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/mock/webhook-script-py")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> webhookCaptor = ArgumentCaptor.forClass((Class) Map.class);

        verify(asyncWebhookService, timeout(1000)).triggerWebhook(eq(createdRoute.getId()), webhookCaptor.capture());

        Map<String, Object> webhookConfig = webhookCaptor.getValue();
        assertEquals("http://example.com/callback-py", webhookConfig.get("url"));
        assertEquals("POST", webhookConfig.get("method"));
        assertEquals("{\"test\":123}", webhookConfig.get("body"));
        assertEquals(1000, ((Number) webhookConfig.get("delayMs")).intValue());
    }
}
