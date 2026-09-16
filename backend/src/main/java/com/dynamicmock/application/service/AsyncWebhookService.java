package com.dynamicmock.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class AsyncWebhookService {

    private final RestTemplate restTemplate;
    private final TrafficLogger trafficLogger;
    
    @Value("${mock.route.max-delay-ms:30000}")
    private int maxDelayMs;

    public AsyncWebhookService(TrafficLogger trafficLogger) {
        this.trafficLogger = trafficLogger;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async("webhookTaskExecutor")
    public void triggerWebhook(String sourceRouteId, Map<String, Object> webhookConfig) {
        if (webhookConfig == null || webhookConfig.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        String url = (String) webhookConfig.get("url");
        if (url == null || url.trim().isEmpty()) {
            log.warn("Webhook config missing URL: {}", webhookConfig);
            return;
        }

        String methodStr = (String) webhookConfig.getOrDefault("method", "POST");
        HttpMethod method;
        try {
            method = HttpMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid webhook method '{}', defaulting to POST", methodStr);
            method = HttpMethod.POST;
        }
        
        Object body = webhookConfig.get("body");
        
        int delayMs = 0;
        if (webhookConfig.containsKey("delayMs")) {
            Object delayObj = webhookConfig.get("delayMs");
            if (delayObj instanceof Number) {
                delayMs = ((Number) delayObj).intValue();
            } else if (delayObj instanceof String) {
                try {
                    delayMs = Integer.parseInt((String) delayObj);
                } catch (NumberFormatException e) {
                    log.warn("Invalid delayMs for webhook: {}", delayObj);
                }
            }
        }
        
        if (delayMs > maxDelayMs) {
            log.warn("Webhook delay {} exceeds maximum allowed {}, capping at max", delayMs, maxDelayMs);
            delayMs = maxDelayMs;
        }

        HttpHeaders headers = new HttpHeaders();
        if (body != null) {
            headers.add("Content-Type", "application/json");
        }
        
        Object customHeaders = webhookConfig.get("headers");
        if (customHeaders instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> headerMap = (Map<String, String>) customHeaders;
            headerMap.forEach(headers::add);
        }

        // Apply delay
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Webhook delay interrupted");
                return;
            }
        }

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        int status = 0;
        try {
            log.debug("Triggering webhook to {} {}", method, url);
            ResponseEntity<String> response = restTemplate.exchange(url, method, entity, String.class);
            status = response.getStatusCode().value();
            log.debug("Webhook response status: {}", status);
        } catch (Exception e) {
            log.error("Failed to trigger webhook to {}: {}", url, e.getMessage());
            status = 500;
        }

        // Log to traffic logger
        trafficLogger.log(TrafficLogger.ExecutionEvent.builder()
            .protocol("WEBHOOK")
            .resourceId(sourceRouteId != null ? sourceRouteId : "unknown")
            .method(method.name())
            .path(url)
            .status(status)
            .durationMs(System.currentTimeMillis() - startTime)
            .matchName("Async Webhook")
            .build());
    }
}
