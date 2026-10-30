package com.dynamicmock.domain.model.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

/**
 * DOMAIN LAYER - Value Object
 * Platform-level execution mode defining how outbound actions synchronize
 * with the inbound request/response lifecycle.
 */
public enum ExecutionMode {

    /**
     * Action executes inline before response commit and may mutate the client response.
     */
    SYNC("sync"),

    /**
     * Action executes independently of the response and cannot delay the client response.
     */
    ASYNC("async");

    private final String value;

    ExecutionMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public boolean isSync() {
        return this == SYNC;
    }

    public boolean isAsync() {
        return this == ASYNC;
    }

    @JsonCreator
    public static ExecutionMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return ASYNC;
        }
        String normalized = raw.trim().toLowerCase();
        if ("sync".equals(normalized)
                || "synchronous".equals(normalized)
                || "blocking".equals(normalized)
                || "forward".equals(normalized)) {
            return SYNC;
        }
        return ASYNC;
    }

    /**
     * Resolves the canonical ExecutionMode from an untyped action/webhook configuration map
     * using strict v0.6.2 precedence:
     * 1. executionMode ("sync" | "async")
     * 2. mode ("sync" | "blocking" | "synchronous" | "forward" | "async" | "background")
     * 3. blocking (true -> SYNC, false -> ASYNC)
     * 4. sync (true -> SYNC, false -> ASYNC)
     * 5. async (false -> SYNC, true -> ASYNC)
     * 6. default ASYNC
     */
    public static ExecutionMode resolve(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return ASYNC;
        }

        // 1. Canonical: executionMode
        Object execModeObj = config.get("executionMode");
        if (execModeObj != null) {
            String execModeStr = String.valueOf(execModeObj).trim().toLowerCase();
            if ("sync".equals(execModeStr) || "synchronous".equals(execModeStr) || "blocking".equals(execModeStr)) {
                return SYNC;
            }
            if ("async".equals(execModeStr) || "asynchronous".equals(execModeStr)) {
                return ASYNC;
            }
        }

        // 2. Alias: mode
        Object modeObj = config.get("mode");
        if (modeObj != null) {
            String modeStr = String.valueOf(modeObj).trim().toLowerCase();
            if ("sync".equals(modeStr)
                    || "blocking".equals(modeStr)
                    || "synchronous".equals(modeStr)
                    || "forward".equals(modeStr)) {
                return SYNC;
            }
            if ("async".equals(modeStr)
                    || "asynchronous".equals(modeStr)
                    || "background".equals(modeStr)) {
                return ASYNC;
            }
        }

        // 3. Alias: blocking
        Object blockingObj = config.get("blocking");
        Boolean blockingBool = parseBoolean(blockingObj);
        if (blockingBool != null) {
            return blockingBool ? SYNC : ASYNC;
        }

        // 4. Alias: sync
        Object syncObj = config.get("sync");
        Boolean syncBool = parseBoolean(syncObj);
        if (syncBool != null) {
            return syncBool ? SYNC : ASYNC;
        }

        // 5. Alias: async
        Object asyncObj = config.get("async");
        Boolean asyncBool = parseBoolean(asyncObj);
        if (asyncBool != null) {
            return asyncBool ? ASYNC : SYNC;
        }

        // 6. Default: ASYNC
        return ASYNC;
    }

    private static Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = String.valueOf(value).trim().toLowerCase();
        if ("true".equals(str)) {
            return Boolean.TRUE;
        }
        if ("false".equals(str)) {
            return Boolean.FALSE;
        }
        return null;
    }
}

