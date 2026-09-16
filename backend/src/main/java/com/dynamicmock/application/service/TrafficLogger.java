package com.dynamicmock.application.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrafficLogger {

    private final SimpMessagingTemplate messagingTemplate;

    public void log(ExecutionEvent event) {
        if (event.getId() == null) {
            event.setId(UUID.randomUUID().toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        messagingTemplate.convertAndSend("/topic/traffic", event);
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
        private String method;
        private String path;
        private int status;
        private long durationMs;
        private String matchName;
    }
}
