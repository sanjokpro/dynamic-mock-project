package com.dynamicmock.adapter.in.web;

import com.dynamicmock.adapter.in.web.dto.RedirectHop;
import com.dynamicmock.application.service.WorkspaceEnvironmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestClientRedirectTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WorkspaceEnvironmentService environmentService;

    private TestClientController controller;

    @BeforeEach
    void setUp() {
        controller = new TestClientController(restTemplate, environmentService);
    }

    @Test
    void execute_shouldReturnEmptyRedirectChainWhenNoRedirectOccurs() {
        when(restTemplate.exchange(
                eq("http://localhost:8080/mock/hello"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("{\"status\":\"ok\"}"));

        TestClientController.TestRequest request = TestClientController.TestRequest.builder()
                .url("{{BASE_URL}}/hello")
                .method("GET")
                .build();

        ResponseEntity<TestClientController.TestResponse> result = controller.execute(request, null);

        assertEquals(200, result.getStatusCode().value());
        TestClientController.TestResponse body = result.getBody();
        assertNotNull(body);
        assertEquals(200, body.getStatus());
        assertEquals("{\"status\":\"ok\"}", body.getBody());
        assertNotNull(body.getRedirectChain());
        assertTrue(body.getRedirectChain().isEmpty());
    }

    @Test
    void execute_shouldFollow302RedirectAndPopulateRedirectChain() {
        HttpHeaders redirectHeaders = new HttpHeaders();
        redirectHeaders.setLocation(URI.create("http://localhost:8080/mock/return-url-final-destination"));

        ResponseEntity<String> hop1Response = new ResponseEntity<>("", redirectHeaders, HttpStatus.FOUND);
        ResponseEntity<String> hop2Response = ResponseEntity.ok("{\"finalDestination\":true}");

        when(restTemplate.exchange(
                eq("http://localhost:8080/mock/return-url-redirector"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(hop1Response);

        when(restTemplate.exchange(
                eq("http://localhost:8080/mock/return-url-final-destination"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(hop2Response);

        TestClientController.TestRequest request = TestClientController.TestRequest.builder()
                .url("http://localhost:8080/mock/return-url-redirector")
                .method("GET")
                .build();

        ResponseEntity<TestClientController.TestResponse> result = controller.execute(request, null);

        TestClientController.TestResponse body = result.getBody();
        assertNotNull(body);
        assertEquals(200, body.getStatus());
        assertEquals("{\"finalDestination\":true}", body.getBody());

        List<RedirectHop> chain = body.getRedirectChain();
        assertNotNull(chain);
        assertEquals(2, chain.size());

        RedirectHop hop1 = chain.get(0);
        assertEquals(1, hop1.getStep());
        assertEquals("GET", hop1.getMethod());
        assertEquals("http://localhost:8080/mock/return-url-redirector", hop1.getUrl());
        assertEquals(302, hop1.getStatus());
        assertEquals("http://localhost:8080/mock/return-url-final-destination", hop1.getLocation());

        RedirectHop hop2 = chain.get(1);
        assertEquals(2, hop2.getStep());
        assertEquals("GET", hop2.getMethod());
        assertEquals("http://localhost:8080/mock/return-url-final-destination", hop2.getUrl());
        assertEquals(200, hop2.getStatus());
        assertNull(hop2.getLocation());
    }

    @Test
    void execute_shouldSwitchPostToGetOn302Redirect() {
        HttpHeaders redirectHeaders = new HttpHeaders();
        redirectHeaders.setLocation(URI.create("/mock/landing"));

        when(restTemplate.exchange(
                eq("http://localhost:8080/mock/submit"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(new ResponseEntity<>("", redirectHeaders, HttpStatus.FOUND));

        when(restTemplate.exchange(
                eq("http://localhost:8080/mock/landing"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("Landing Page"));

        TestClientController.TestRequest request = TestClientController.TestRequest.builder()
                .url("http://localhost:8080/mock/submit")
                .method("POST")
                .body("{\"user\":\"alice\"}")
                .headers(Map.of("Content-Type", "application/json"))
                .build();

        ResponseEntity<TestClientController.TestResponse> result = controller.execute(request, null);

        TestClientController.TestResponse body = result.getBody();
        assertNotNull(body);
        assertEquals(200, body.getStatus());
        assertEquals(2, body.getRedirectChain().size());
        assertEquals("POST", body.getRedirectChain().get(0).getMethod());
        assertEquals("GET", body.getRedirectChain().get(1).getMethod());

        verify(restTemplate).exchange(
                eq("http://localhost:8080/mock/landing"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }
}
