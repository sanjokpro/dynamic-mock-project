package com.dynamicmock.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {
    @PostConstruct
    public void tweakRootLogging() {
        // Ensure org.jpos logs show up at DEBUG when root is DEBUG
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
    }
}

