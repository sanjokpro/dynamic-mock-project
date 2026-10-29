package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.RedirectHop;
import com.dynamicmock.adapter.in.web.dto.WebhookExecutionDetail;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AsyncWebhookService {

    public static final int MAX_REDIRECTS = 5;

    private final RestTemplate restTemplate;
    private final TrafficLogger trafficLogger;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${mock.route.max-delay-ms:30000}")
    private int maxDelayMs = 30000;

    public AsyncWebhookService(TrafficLogger trafficLogger) {
        this.trafficLogger = trafficLogger;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async("webhookTaskExecutor")
    public void triggerWebhook(String sourceRouteId, Map<String, Object> webhookConfig) {
        triggerWebhook(sourceRouteId, null, webhookConfig);
    }

    @Async("webhookTaskExecutor")
    public void triggerWebhook(String sourceRouteId, String parentEventId, Map<String, Object> webhookConfig) {
        if (webhookConfig == null || webhookConfig.isEmpty()) {
            return;
        }

        long startNano = System.nanoTime();
        String url = (String) webhookConfig.get("url");
        if (url == null || url.trim().isEmpty()) {
            log.warn("Webhook config missing URL: {}", webhookConfig);
            return;
        }

        String methodStr = (String) webhookConfig.getOrDefault("method", "POST");
        HttpMethod initialMethod;
        try {
            initialMethod = HttpMethod.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid webhook method '{}', defaulting to POST", methodStr);
            initialMethod = HttpMethod.POST;
        }
        
        Object body = webhookConfig.get("body");
        String requestBodyStr = serializeBodyToString(body);
        
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

        Map<String, String> reqHeadersMap = headers.toSingleValueMap();

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

        List<RedirectHop> redirectChain = new ArrayList<>();
        String currentUrl = url;
        HttpMethod currentMethod = initialMethod;
        Object currentBody = body;

        int status = 0;
        Map<String, String> resHeadersMap = Collections.emptyMap();
        String responseBodyStr = null;
        String errorMessage = null;

        try {
            for (int hop = 1; hop <= MAX_REDIRECTS + 1; hop++) {
                long hopStartNano = System.nanoTime();
                HttpEntity<Object> entity = new HttpEntity<>(currentBody, headers);

                log.debug("Triggering webhook (hop {}) to {} {}", hop, currentMethod, currentUrl);
                ResponseEntity<String> response = restTemplate.exchange(currentUrl, currentMethod, entity, String.class);

                double hopDurationMs = computeDurationMs(hopStartNano);
                status = response.getStatusCode().value();
                resHeadersMap = response.getHeaders() != null
                        ? response.getHeaders().toSingleValueMap()
                        : Collections.emptyMap();
                responseBodyStr = response.getBody();

                String location = extractLocationHeader(response.getHeaders());
                if (isRedirectStatus(status) && location != null && !location.isBlank() && hop <= MAX_REDIRECTS) {
                    redirectChain.add(RedirectHop.builder()
                            .step(hop)
                            .method(currentMethod.name())
                            .url(currentUrl)
                            .status(status)
                            .location(location)
                            .durationMs(hopDurationMs)
                            .build());

                    currentUrl = resolveRedirectUrl(currentUrl, location);
                    if (status == 301 || status == 302 || status == 303) {
                        currentMethod = HttpMethod.GET;
                        currentBody = null;
                    }
                } else {
                    if (!redirectChain.isEmpty()) {
                        redirectChain.add(RedirectHop.builder()
                                .step(hop)
                                .method(currentMethod.name())
                                .url(currentUrl)
                                .status(status)
                                .location(location)
                                .durationMs(hopDurationMs)
                                .build());
                    }
                    break;
                }
            }
            log.debug("Webhook response status: {}", status);
        } catch (Exception e) {
            log.error("Failed to trigger webhook to {}: {}", currentUrl, e.getMessage());
            status = 500;
            errorMessage = e.getMessage();
        }

        double totalDurationMs = computeDurationMs(startNano);

        WebhookExecutionDetail webhookDetail = WebhookExecutionDetail.builder()
                .sourceRouteId(sourceRouteId != null ? sourceRouteId : "unknown")
                .parentEventId(parentEventId)
                .targetUrl(url)
                .finalUrl(currentUrl)
                .method(initialMethod.name())
                .delayMs(delayMs)
                .requestHeaders(reqHeadersMap)
                .requestBody(TrafficLogger.sanitizePayload(requestBodyStr, reqHeadersMap.get("Content-Type")))
                .responseStatus(status)
                .responseHeaders(resHeadersMap)
                .responseBody(TrafficLogger.sanitizePayload(responseBodyStr, resHeadersMap.get("Content-Type")))
                .errorMessage(errorMessage)
                .build();

        // Log to traffic logger
        trafficLogger.log(TrafficLogger.ExecutionEvent.builder()
            .protocol("WEBHOOK")
            .resourceId(sourceRouteId != null ? sourceRouteId : "unknown")
            .parentEventId(parentEventId)
            .method(initialMethod.name())
            .path(url)
            .requestHeaders(reqHeadersMap)
            .requestBody(requestBodyStr)
            .status(status)
            .responseHeaders(resHeadersMap)
            .responseBody(responseBodyStr)
            .durationMs(totalDurationMs)
            .matchName("Async Webhook")
            .redirectChain(redirectChain)
            .webhookDetail(webhookDetail)
            .build());
    }

    private String serializeBodyToString(Object body) {
        if (body == null) return null;
        if (body instanceof String) return (String) body;
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    private static boolean isRedirectStatus(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private static String extractLocationHeader(HttpHeaders headers) {
        if (headers == null) return null;
        URI loc = headers.getLocation();
        if (loc != null) return loc.toString();
        for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
            if (entry.getKey() != null && "location".equalsIgnoreCase(entry.getKey())
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue().get(0);
            }
        }
        return null;
    }

    private static String resolveRedirectUrl(String baseUrl, String location) {
        try {
            return URI.create(baseUrl).resolve(location).toString();
        } catch (Exception e) {
            return location;
        }
    }

    private static double computeDurationMs(long startNano) {
        double rawMs = (System.nanoTime() - startNano) / 1_000_000.0;
        return Math.round(rawMs * 100.0) / 100.0;
    }
}
