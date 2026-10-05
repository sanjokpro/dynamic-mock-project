package com.dynamicmock.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request body for the ISO8583 message simulator.
 * Contains the MTI and field values to send as a test message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583SimulateRequest {

    /**
     * Message Type Indicator, e.g. "0100"
     */
    private String mti;

    /**
     * ISO8583 field values keyed by field number (as string, e.g. "2", "11", "41").
     */
    private Map<String, String> fields;

    /**
     * Optional timeout in milliseconds. Defaults to 30000 (30 seconds) if not set.
     */
    private Integer timeoutMs;
}
