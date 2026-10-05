package com.dynamicmock.application.service;

import com.dynamicmock.adapter.in.web.dto.Iso8583SimulateRequest;
import com.dynamicmock.adapter.in.web.dto.Iso8583SimulateResponse;
import com.dynamicmock.adapter.out.protocol.iso8583.Iso8583PackagerFactory;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.port.out.Iso8583EndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for the ISO8583 Message Simulator.
 *
 * Opens a TCP connection to the running mock server on the endpoint's port,
 * sends a test ISO8583 message constructed from the request, and returns the decoded response.
 *
 * Uses Iso8583PackagerFactory to ensure the same packager is used as the mock server,
 * preventing encoding/decoding mismatches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Iso8583SimulatorService {

    private final Iso8583EndpointRepository repository;
    private final Iso8583PackagerFactory packagerFactory;

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    /**
     * Simulate an ISO8583 message against the given endpoint.
     *
     * @param endpointId the endpoint ID to simulate against
     * @param request    MTI and fields to send
     * @return decoded response or structured error
     */
    public Iso8583SimulateResponse simulate(String endpointId, Iso8583SimulateRequest request) {
        Iso8583Endpoint endpoint = repository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("ISO8583 endpoint not found: " + endpointId));

        // Must be active — the mock server must be listening on the port
        if (!Boolean.TRUE.equals(endpoint.getActive())) {
            return Iso8583SimulateResponse.builder()
                    .success(false)
                    .errorType("ENDPOINT_INACTIVE")
                    .errorMessage("Endpoint '" + endpoint.getName() + "' is not active. "
                            + "Activate it before simulating.")
                    .build();
        }

        int port = endpoint.getPort();
        int timeoutMs = request.getTimeoutMs() != null ? request.getTimeoutMs() : DEFAULT_TIMEOUT_MS;

        ISOPackager packager = packagerFactory.create(endpoint);

        // Pack the outbound message
        byte[] requestBytes;
        String requestHex;
        try {
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.setMTI(request.getMti());
            if (request.getFields() != null) {
                for (Map.Entry<String, String> entry : request.getFields().entrySet()) {
                    try {
                        int fieldNum = Integer.parseInt(entry.getKey());
                        msg.set(fieldNum, entry.getValue());
                    } catch (NumberFormatException e) {
                        log.warn("Skipping non-numeric field key: {}", entry.getKey());
                    }
                }
            }
            requestBytes = msg.pack();
            requestHex = toHex(requestBytes);
            log.debug("Simulator packed message: MTI={} hex={}", request.getMti(), requestHex);
        } catch (Exception e) {
            log.warn("Simulator pack error for endpoint '{}': {}", endpoint.getName(), e.getMessage());
            return Iso8583SimulateResponse.builder()
                    .success(false)
                    .errorType("PACK_ERROR")
                    .errorMessage("Failed to encode ISO8583 message: " + e.getMessage())
                    .build();
        }

        // Send over TCP and receive response
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Write length header + message
            writeLength(out, requestBytes.length, endpoint.getHeaderLengthType());
            out.write(requestBytes);
            out.flush();

            // Read response
            int responseLength = readLength(in, endpoint.getHeaderLengthType());
            byte[] responseBytes = new byte[responseLength];
            in.readFully(responseBytes);

            long latencyMs = System.currentTimeMillis() - start;
            String responseHex = toHex(responseBytes);

            // Unpack response
            ISOMsg responseMsg = new ISOMsg();
            responseMsg.setPackager(packager);
            responseMsg.unpack(responseBytes);

            String responseMti = responseMsg.getMTI();
            Map<String, String> responseFields = new HashMap<>();
            for (int i = 0; i <= 128; i++) {
                if (responseMsg.hasField(i)) {
                    responseFields.put(String.valueOf(i), responseMsg.getString(i));
                }
            }

            log.info("Simulator: endpoint='{}' port={} MTI={} -> {} latency={}ms",
                    endpoint.getName(), port, request.getMti(), responseMti, latencyMs);

            return Iso8583SimulateResponse.builder()
                    .success(true)
                    .responseMti(responseMti)
                    .responseFields(responseFields)
                    .requestHex(requestHex)
                    .responseHex(responseHex)
                    .latencyMs(latencyMs)
                    .build();

        } catch (ConnectException e) {
            log.warn("Simulator connection refused to port {}: {}", port, e.getMessage());
            return Iso8583SimulateResponse.builder()
                    .success(false)
                    .errorType("CONNECTION_REFUSED")
                    .errorMessage("Connection refused on port " + port
                            + ". Ensure the endpoint is active and the server has started.")
                    .build();
        } catch (SocketTimeoutException e) {
            long latencyMs = System.currentTimeMillis() - start;
            log.warn("Simulator timeout after {}ms for endpoint '{}'", latencyMs, endpoint.getName());
            return Iso8583SimulateResponse.builder()
                    .success(false)
                    .errorType("TIMEOUT")
                    .errorMessage("Response timeout after " + timeoutMs
                            + "ms. The mock server may be misconfigured or not responding.")
                    .latencyMs(latencyMs)
                    .build();
        } catch (EOFException e) {
            log.warn("Simulator received EOF from port {}: {}", port, e.getMessage());
            return Iso8583SimulateResponse.builder()
                    .success(false)
                    .errorType("UNPACK_ERROR")
                    .errorMessage("Server closed connection before sending a complete response.")
                    .build();
        } catch (Exception e) {
            log.error("Simulator error for endpoint '{}': {}", endpoint.getName(), e.getMessage(), e);
            return Iso8583SimulateResponse.builder()
                    .success(false)
                    .errorType("INTERNAL_ERROR")
                    .errorMessage("Simulation failed: " + e.getMessage())
                    .build();
        }
    }

    private int readLength(DataInputStream in, String headerType) throws java.io.IOException {
        return switch (headerType != null ? headerType : "2BYTE") {
            case "4BYTE" -> in.readInt();
            case "NONE" -> 1024;
            default -> in.readUnsignedShort();
        };
    }

    private void writeLength(DataOutputStream out, int length, String headerType)
            throws java.io.IOException {
        switch (headerType != null ? headerType : "2BYTE") {
            case "4BYTE" -> out.writeInt(length);
            case "NONE" -> { /* no header */ }
            default -> out.writeShort(length);
        }
    }

    private String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
