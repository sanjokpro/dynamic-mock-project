package com.dynamicmock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AsyncWebhookServiceTest {

    @Mock
    private TrafficLogger trafficLogger;

    @Mock
    private RestTemplate restTemplate;

    private AsyncWebhookService asyncWebhookService;

    @BeforeEach
    void setUp() {
        asyncWebhookService = new AsyncWebhookService(trafficLogger);
        ReflectionTestUtils.setField(asyncWebhookService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(asyncWebhookService, "maxDelayMs", 30000);
    }

    @Test
    void triggerWebhook_shouldSendPostRequestByDefault() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "http://example.com/hook");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        asyncWebhookService.triggerWebhook("route-123", config);

        verify(restTemplate).exchange(eq("http://example.com/hook"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        verify(trafficLogger).log(any(TrafficLogger.ExecutionEvent.class));
    }

    @Test
    void triggerWebhook_shouldRespectConfiguredMethodAndBody() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "http://example.com/hook");
        config.put("method", "PUT");
        config.put("body", "{\"data\":1}");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        asyncWebhookService.triggerWebhook("route-123", config);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Object>> entityCaptor = ArgumentCaptor.forClass((Class) HttpEntity.class);

        verify(restTemplate).exchange(eq("http://example.com/hook"), eq(HttpMethod.PUT), entityCaptor.capture(), eq(String.class));
        
        HttpEntity<Object> entity = entityCaptor.getValue();
        assertEquals("{\"data\":1}", entity.getBody());
        assertEquals("application/json", entity.getHeaders().getFirst("Content-Type"));
    }

    @Test
    void triggerWebhook_shouldCapDelayAtMax() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "http://example.com/hook");
        config.put("delayMs", 50000); // Exceeds 30000 max

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("Success"));

        long start = System.currentTimeMillis();
        
        // Temporarily lower maxDelay to speed up test
        ReflectionTestUtils.setField(asyncWebhookService, "maxDelayMs", 100);
        asyncWebhookService.triggerWebhook("route-123", config);
        
        long duration = System.currentTimeMillis() - start;

        // Since it's capped at 100, it should take at least 100ms but not much more (definitely not 50000)
        assertTrue(duration >= 100 && duration < 5000);
        
        verify(restTemplate).exchange(eq("http://example.com/hook"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
    }
    
    private void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("Condition failed");
    }

    @Test
    void triggerWebhook_shouldHandleRestTemplateExceptionGracefully() {
        Map<String, Object> config = new HashMap<>();
        config.put("url", "http://example.com/error");

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // Should not throw
        asyncWebhookService.triggerWebhook("route-123", config);

        verify(restTemplate).exchange(eq("http://example.com/error"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        
        ArgumentCaptor<TrafficLogger.ExecutionEvent> logCaptor = ArgumentCaptor.forClass(TrafficLogger.ExecutionEvent.class);
        verify(trafficLogger).log(logCaptor.capture());
        
        assertEquals(500, logCaptor.getValue().getStatus());
        assertEquals("WEBHOOK", logCaptor.getValue().getProtocol());
    }
}
