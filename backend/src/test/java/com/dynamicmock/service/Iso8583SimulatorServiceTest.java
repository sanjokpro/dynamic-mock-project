package com.dynamicmock.service;

import com.dynamicmock.adapter.in.web.dto.Iso8583SimulateRequest;
import com.dynamicmock.adapter.in.web.dto.Iso8583SimulateResponse;
import com.dynamicmock.adapter.out.protocol.iso8583.Iso8583PackagerFactory;
import com.dynamicmock.application.service.Iso8583SimulatorService;
import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.port.out.Iso8583EndpointRepository;
import org.jpos.iso.ISOPackager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Iso8583SimulatorServiceTest {

    @Mock
    private Iso8583EndpointRepository repository;

    @Mock
    private Iso8583PackagerFactory packagerFactory;

    @InjectMocks
    private Iso8583SimulatorService simulatorService;

    private Iso8583Endpoint activeEndpoint;
    private Iso8583Endpoint inactiveEndpoint;

    @BeforeEach
    void setUp() {
        activeEndpoint = Iso8583Endpoint.builder()
                .id("active-id")
                .name("Active Endpoint")
                .port(18583) // high port for tests — won't conflict
                .active(true)
                .headerLengthType("2BYTE")
                .build();

        inactiveEndpoint = Iso8583Endpoint.builder()
                .id("inactive-id")
                .name("Inactive Endpoint")
                .port(18584)
                .active(false)
                .headerLengthType("2BYTE")
                .build();
    }

    @Test
    void simulate_returnsEndpointInactiveError_whenEndpointNotActive() {
        when(repository.findById("inactive-id")).thenReturn(Optional.of(inactiveEndpoint));

        Iso8583SimulateRequest request = Iso8583SimulateRequest.builder()
                .mti("0100")
                .fields(Map.of("2", "4111111111111111"))
                .build();

        Iso8583SimulateResponse response = simulatorService.simulate("inactive-id", request);

        assertFalse(response.isSuccess());
        assertEquals("ENDPOINT_INACTIVE", response.getErrorType());
        assertNotNull(response.getErrorMessage());
        verifyNoInteractions(packagerFactory);
    }

    @Test
    void simulate_returnsConnectionRefusedError_whenNoServerListening() {
        when(repository.findById("active-id")).thenReturn(Optional.of(activeEndpoint));
        // Return a real ISO87APackager so pack() works
        when(packagerFactory.create(activeEndpoint)).thenReturn(new org.jpos.iso.packager.ISO87APackager());

        Iso8583SimulateRequest request = Iso8583SimulateRequest.builder()
                .mti("0100")
                .fields(Map.of("2", "4111111111111111", "3", "000000", "4", "000000010000"))
                .timeoutMs(2000)
                .build();

        // Port 18583 should not have anything listening in tests
        Iso8583SimulateResponse response = simulatorService.simulate("active-id", request);

        assertFalse(response.isSuccess());
        assertEquals("CONNECTION_REFUSED", response.getErrorType());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void simulate_throwsException_whenEndpointNotFound() {
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        Iso8583SimulateRequest request = Iso8583SimulateRequest.builder()
                .mti("0100")
                .fields(Map.of())
                .build();

        assertThrows(RuntimeException.class, () -> simulatorService.simulate("missing-id", request));
    }

    @Test
    void simulate_usesPackagerFactory() {
        when(repository.findById("active-id")).thenReturn(Optional.of(activeEndpoint));
        when(packagerFactory.create(activeEndpoint)).thenReturn(new org.jpos.iso.packager.ISO87APackager());

        Iso8583SimulateRequest request = Iso8583SimulateRequest.builder()
                .mti("0100")
                .fields(Map.of("2", "4111111111111111"))
                .timeoutMs(1000)
                .build();

        simulatorService.simulate("active-id", request);

        // Verify the factory was consulted — ensures packager consistency
        verify(packagerFactory).create(activeEndpoint);
    }
}
