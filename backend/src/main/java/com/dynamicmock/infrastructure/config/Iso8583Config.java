package com.dynamicmock.infrastructure.config;

import com.dynamicmock.adapter.out.protocol.iso8583.DynamicMockRequestListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ISO8583 jPOS integration.
 * 
 * Provides Spring ApplicationContext to jPOS request listeners
 * so they can access Spring beans (TemplateEngine, ScriptEngine, etc.)
 */
@Slf4j
@Configuration
public class Iso8583Config implements ApplicationContextAware {
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        // Share Spring context with jPOS request listeners
        DynamicMockRequestListener.setApplicationContext(applicationContext);
        log.debug("Configured ISO8583 request listener with Spring context");
    }
}

