package com.dynamicmock.infrastructure.config;

import com.dynamicmock.application.service.GraphQLService;
import com.dynamicmock.application.service.GrpcService;
import com.dynamicmock.application.service.Iso8583Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Initializes protocol services on application startup
 * Loads all active GraphQL, gRPC, and ISO8583 endpoints
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolInitializer {
    
    private final GraphQLService graphQLService;
    private final GrpcService grpcService;
    private final Iso8583Service iso8583Service;
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Initializing protocol services...");
        
        try {
            graphQLService.reloadActiveEndpoints();
        } catch (Exception e) {
            log.error("Failed to initialize GraphQL endpoints", e);
        }
        
        try {
            grpcService.reloadActiveEndpoints();
        } catch (Exception e) {
            log.error("Failed to initialize gRPC endpoints", e);
        }
        
        try {
            iso8583Service.reloadActiveEndpoints();
        } catch (Exception e) {
            log.error("Failed to initialize ISO8583 endpoints", e);
        }
        
        log.info("Protocol services initialization complete");
    }
}

