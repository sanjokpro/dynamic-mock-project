package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.RedirectHop;
import com.dynamicmock.adapter.in.web.dto.WebhookExecutionDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrafficLogger {

    public static final int MAX_CAPTURED_BODY_CHARS = 32_768;
    public static final String BINARY_PAYLOAD_PLACEHOLDER = "<BINARY PAYLOAD OMITTED>";
    public static final String TRUNCATED_SUFFIX = " ... [TRUNCATED: payload exceeded 32KB limit]";

    private final SimpMessagingTemplate messagingTemplate;

    public void log(ExecutionEvent event) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }

        // Normalize durationMs to 2 decimal places for clean sub-millisecond display (e.g., 126.43)
        event.setDurationMs(Math.round(event.getDurationMs() * 100.0) / 100.0);

        String reqContentType = extractContentType(event.getRequestHeaders());
        String resContentType = extractContentType(event.getResponseHeaders());

        // Compute payloadSizeBytes before truncation if not already provided
        if (event.getPayloadSizeBytes() == null) {
            if (event.getResponseBody() != null && !BINARY_PAYLOAD_PLACEHOLDER.equals(event.getResponseBody())) {
                event.setPayloadSizeBytes((long) event.getResponseBody().getBytes(StandardCharsets.UTF_8).length);
            } else {
                event.setPayloadSizeBytes(0L);
            }
        }

        boolean reqTruncated = isExceedingLimit(event.getRequestBody(), reqContentType);
        boolean resTruncated = isExceedingLimit(event.getResponseBody(), resContentType);
        if (Boolean.TRUE.equals(event.getBodyTruncated()) || reqTruncated || resTruncated) {
            event.setBodyTruncated(true);
        } else if (event.getBodyTruncated() == null) {
            event.setBodyTruncated(false);
        }

        if (event.getRequestBody() != null) {
            event.setRequestBody(sanitizePayload(event.getRequestBody(), reqContentType));
        }
        if (event.getResponseBody() != null) {
            event.setResponseBody(sanitizePayload(event.getResponseBody(), resContentType));
        }

        messagingTemplate.convertAndSend("/topic/traffic", event);
    }

    public static String sanitizePayload(String body, String contentType) {
        if (isBinaryContentType(contentType)) {
            return BINARY_PAYLOAD_PLACEHOLDER;
        }
        if (body == null || body.isEmpty()) {
            return null;
        }
        if (body.length() > MAX_CAPTURED_BODY_CHARS) {
            if (body.endsWith(TRUNCATED_SUFFIX)) {
                return body;
            }
            return body.substring(0, MAX_CAPTURED_BODY_CHARS) + TRUNCATED_SUFFIX;
        }
        return body;
    }

    public static boolean isBinaryContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }
        String lower = contentType.toLowerCase().trim();
        return lower.startsWith("image/")
                || lower.startsWith("video/")
                || lower.startsWith("audio/")
                || lower.startsWith("multipart/")
                || lower.contains("application/octet-stream")
                || lower.contains("application/pdf")
                || lower.contains("application/zip")
                || lower.contains("application/gzip");
    }

    private static boolean isExceedingLimit(String body, String contentType) {
        if (body == null || isBinaryContentType(contentType)) {
            return false;
        }
        return body.length() > MAX_CAPTURED_BODY_CHARS;
    }

    private static String extractContentType(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && "content-type".equalsIgnoreCase(entry.getKey().trim())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionEvent {
        private String id;
        private LocalDateTime timestamp;
        private String protocol;
        private String resourceId;
        private String parentEventId;
        private String method;
        private String path;
        private String queryString;
        private Map<String, String> queryParams;
        private Map<String, String> requestHeaders;
        private String requestBody;
        private int status;
        private Map<String, String> responseHeaders;
        private String responseBody;
        private double durationMs;
        private String matchName;
        private List<RedirectHop> redirectChain;
        private WebhookExecutionDetail webhookDetail;
        private Long payloadSizeBytes;
        private Boolean bodyTruncated;
    }
}
