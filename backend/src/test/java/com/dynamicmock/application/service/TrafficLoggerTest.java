package com.dynamicmock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrafficLoggerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private TrafficLogger trafficLogger;

    @BeforeEach
    void setUp() {
        trafficLogger = new TrafficLogger(messagingTemplate);
    }

    @Test
    void log_shouldPopulateDefaultsAndRoundDurationMsToTwoDecimals() {
        TrafficLogger.ExecutionEvent event = TrafficLogger.ExecutionEvent.builder()
                .protocol("HTTP")
                .method("GET")
                .path("/mobile-banking-login")
                .queryString("action=login&returnUrl=http://localhost/mock/dest")
                .queryParams(Map.of("action", "login", "returnUrl", "http://localhost/mock/dest"))
                .status(200)
                .durationMs(126.43456)
                .responseHeaders(Map.of("Content-Type", "text/html; charset=utf-8"))
                .responseBody("<html><body>Login</body></html>")
                .build();

        trafficLogger.log(event);

        ArgumentCaptor<TrafficLogger.ExecutionEvent> captor = ArgumentCaptor.forClass(TrafficLogger.ExecutionEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/traffic"), captor.capture());

        TrafficLogger.ExecutionEvent logged = captor.getValue();
        assertNotNull(logged.getId());
        assertNotNull(logged.getTimestamp());
        assertEquals(126.43, logged.getDurationMs());
        assertEquals("action=login&returnUrl=http://localhost/mock/dest", logged.getQueryString());
        assertEquals("login", logged.getQueryParams().get("action"));
        assertEquals(31L, logged.getPayloadSizeBytes());
        assertFalse(logged.getBodyTruncated());
    }

    @Test
    void log_shouldOmitBinaryPayloadsForImageVideoMultipartAndOctetStream() {
        String[] binaryTypes = {
                "image/png",
                "video/mp4",
                "multipart/form-data; boundary=---123",
                "application/octet-stream"
        };

        for (String contentType : binaryTypes) {
            TrafficLogger.ExecutionEvent event = TrafficLogger.ExecutionEvent.builder()
                    .protocol("HTTP")
                    .method("POST")
                    .path("/upload")
                    .requestHeaders(Map.of("Content-Type", contentType))
                    .requestBody("raw-binary-bytes-12345")
                    .responseHeaders(Map.of("content-type", contentType))
                    .responseBody("raw-binary-response-67890")
                    .status(200)
                    .durationMs(12.5)
                    .build();

            trafficLogger.log(event);

            assertEquals("<BINARY PAYLOAD OMITTED>", event.getRequestBody(),
                    "Expected binary request body omitted for " + contentType);
            assertEquals("<BINARY PAYLOAD OMITTED>", event.getResponseBody(),
                    "Expected binary response body omitted for " + contentType);
            assertFalse(event.getBodyTruncated());
        }
    }

    @Test
    void log_shouldTruncatePayloadsExceeding32KBWithIndicator() {
        String largePayload = "A".repeat(TrafficLogger.MAX_CAPTURED_BODY_CHARS + 500);

        TrafficLogger.ExecutionEvent event = TrafficLogger.ExecutionEvent.builder()
                .protocol("HTTP")
                .method("GET")
                .path("/large-html")
                .responseHeaders(Map.of("Content-Type", "text/html"))
                .responseBody(largePayload)
                .status(200)
                .durationMs(45.67)
                .build();

        trafficLogger.log(event);

        assertTrue(event.getBodyTruncated());
        assertEquals((long) largePayload.length(), event.getPayloadSizeBytes());
        assertTrue(event.getResponseBody().endsWith(TrafficLogger.TRUNCATED_SUFFIX));
        assertEquals(
                TrafficLogger.MAX_CAPTURED_BODY_CHARS + TrafficLogger.TRUNCATED_SUFFIX.length(),
                event.getResponseBody().length()
        );
    }
}
