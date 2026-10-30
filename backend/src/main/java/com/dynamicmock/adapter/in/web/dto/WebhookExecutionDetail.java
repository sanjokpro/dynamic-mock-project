package com.dynamicmock.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookExecutionDetail {
    private String sourceRouteId;
    private String parentEventId;
    private String targetUrl;
    private String finalUrl;
    private String method;
    private int delayMs;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private int responseStatus;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String errorMessage;
}

