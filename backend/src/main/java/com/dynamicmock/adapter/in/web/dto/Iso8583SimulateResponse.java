package com.dynamicmock.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response body for the ISO8583 message simulator.
 * Contains the decoded response fields, raw hex, latency, and any error information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583SimulateResponse {

    /**
     * Whether the simulation succeeded (message sent and response received).
     */
    private boolean success;

    /**
     * The response MTI, e.g. "0110".
     */
    private String responseMti;

    /**
     * Decoded response fields keyed by field number string, e.g. { "39": "00", "38": "123456" }.
     */
    private Map<String, String> responseFields;

    /**
     * Raw outbound message in hexadecimal for debugging.
     */
    private String requestHex;

    /**
     * Raw inbound response message in hexadecimal for debugging.
     */
    private String responseHex;

    /**
     * Round-trip latency in milliseconds (TCP send + receive).
     */
    private long latencyMs;

    /**
     * Error message if success=false.
     */
    private String errorMessage;

    /**
     * Error type for structured frontend handling.
     * One of: ENDPOINT_INACTIVE, CONNECTION_REFUSED, TIMEOUT, PACK_ERROR, UNPACK_ERROR, INTERNAL_ERROR
     */
    private String errorType;
}
