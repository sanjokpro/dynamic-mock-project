package com.dynamicmock.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    /**
     * Canonical execution mode in v0.6.2: "sync" | "async"
     */
    @JsonProperty("executionMode")
    @JsonAlias({"mode"})
    private String executionMode;

    private Map<String, String> requestHeaders;
    private String requestBody;
    private int responseStatus;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private String errorMessage;

    @JsonProperty("blocking")
    public Boolean isBlocking() {
        return "sync".equalsIgnoreCase(this.executionMode);
    }

    public Boolean getBlocking() {
        return isBlocking();
    }

    public void setBlocking(Boolean blocking) {
        if (Boolean.TRUE.equals(blocking)) {
            this.executionMode = "sync";
        } else if (Boolean.FALSE.equals(blocking) && this.executionMode == null) {
            this.executionMode = "async";
        }
    }

    public static class WebhookExecutionDetailBuilder {
        public WebhookExecutionDetailBuilder executionMode(String executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public WebhookExecutionDetailBuilder blocking(Boolean blocking) {
            if (Boolean.TRUE.equals(blocking) && this.executionMode == null) {
                this.executionMode = "sync";
            } else if (Boolean.FALSE.equals(blocking) && this.executionMode == null) {
                this.executionMode = "async";
            }
            return this;
        }
    }
}
