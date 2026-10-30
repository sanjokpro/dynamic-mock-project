package com.dynamicmock.domain.model.execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DOMAIN LAYER - Value Object
 * Platform-level outbound interaction contract.
 * Defines how an outbound action synchronizes with the inbound request/response lifecycle.
 *
 * Future consumers (documented only; not implemented in v0.6.2):
 * - WebhookAction (first consumer in v0.6.2)
 * - Callback / Redirect flows
 * - HTTP Forwarding
 * - OAuth / 3DS / Message Bus flows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockActionContract {

    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.ASYNC;

    @Builder.Default
    private boolean followRedirects = true;

    @Builder.Default
    private boolean returnResponseToCaller = false;

    public boolean isSynchronous() {
        return executionMode == ExecutionMode.SYNC;
    }

    /**
     * Resolves a minimal MockActionContract from an untyped action/webhook map.
     */
    public static MockActionContract fromConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return MockActionContract.builder()
                    .executionMode(ExecutionMode.ASYNC)
                    .followRedirects(true)
                    .returnResponseToCaller(false)
                    .build();
        }

        ExecutionMode mode = ExecutionMode.resolve(config);

        boolean followRedirects = true;
        if (config.containsKey("followRedirects") && config.get("followRedirects") != null) {
            Boolean parsedFollow = parseBoolean(config.get("followRedirects"));
            if (parsedFollow != null) {
                followRedirects = parsedFollow;
            }
        }

        boolean returnResponseToCaller = mode.isSync();
        if (config.containsKey("returnResponseToCaller") && config.get("returnResponseToCaller") != null) {
            Boolean parsedReturn = parseBoolean(config.get("returnResponseToCaller"));
            if (parsedReturn != null) {
                returnResponseToCaller = parsedReturn;
            }
        } else if (config.containsKey("returnResponse") && config.get("returnResponse") != null) {
            Boolean parsedReturn = parseBoolean(config.get("returnResponse"));
            if (parsedReturn != null) {
                returnResponseToCaller = parsedReturn;
            }
        }

        return MockActionContract.builder()
                .executionMode(mode)
                .followRedirects(followRedirects)
                .returnResponseToCaller(returnResponseToCaller)
                .build();
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

