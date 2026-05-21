package com.dynamicmock.config;

import com.dynamicmock.application.service.GraphQLService;
import com.dynamicmock.application.service.GrpcService;
import com.dynamicmock.application.service.Iso8583Service;
import com.dynamicmock.infrastructure.config.ProtocolInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProtocolInitializerTest {

    @Mock
    private GraphQLService graphQLService;

    @Mock
    private GrpcService grpcService;

    @Mock
    private Iso8583Service iso8583Service;

    @InjectMocks
    private ProtocolInitializer protocolInitializer;

    @Test
    void onApplicationReady_shouldReloadAllProtocols() {
        // When
        protocolInitializer.onApplicationReady();

        // Then
        verify(graphQLService).reloadActiveEndpoints();
        verify(grpcService).reloadActiveEndpoints();
        verify(iso8583Service).reloadActiveEndpoints();
    }

    @Test
    void onApplicationReady_shouldContinueOnErrors() {
        // Given
        doThrow(new RuntimeException("GraphQL error")).when(graphQLService).reloadActiveEndpoints();

        // When
        protocolInitializer.onApplicationReady();

        // Then - even if GraphQL fails, others are attempted
        verify(graphQLService).reloadActiveEndpoints();
        verify(grpcService).reloadActiveEndpoints();
        verify(iso8583Service).reloadActiveEndpoints();
    }
}

