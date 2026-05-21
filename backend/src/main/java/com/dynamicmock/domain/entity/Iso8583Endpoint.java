package com.dynamicmock.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: ISO8583 financial message server configuration.
 * Encapsulates message routing and response generation rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583Endpoint {
    
    private String id;
    
    private String name;
    private String description;
    
    /**
     * TCP port for ISO8583 server.
     */
    private Integer port;
    
    /**
     * Whether this server runs on an isolated port.
     */
    @Builder.Default
    private Boolean isolatedPort = false;
    
    /**
     * Mock scenarios for this server.
     */
    private List<Iso8583Mock> mocks;
    
    /**
     * Global interceptor script (GraalVM - JS/Python).
     */
    private String interceptorScript;
    private String interceptorScriptLanguage;
    
    /**
     * Whether interceptor is enabled
     */
    @Builder.Default
    private Boolean interceptorEnabled = false;
    
    /**
     * Advanced: Custom jPOS server XML configuration.
     */
    private String customServerXml;
    
    /**
     * Whether custom XML editor is enabled
     */
    @Builder.Default
    private Boolean customXmlEnabled = false;
    
    /**
     * Header length type: NONE, 2BYTE (default), 4BYTE
     */
    @Builder.Default
    private String headerLengthType = "2BYTE";
    
    /**
     * Character encoding: ASCII (default), EBCDIC
     */
    @Builder.Default
    private String encoding = "ASCII";
    
    /**
     * Custom packager XML path (relative to cfg/).
     */
    private String packagerConfig;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Individual mock scenario within a server.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Iso8583Mock {
        
        private String id;
        private String name;
        private String description;
        private String mti;
        private String responseMti;
        private Map<String, String> matchers;
        @Builder.Default
        private Integer priority = 0;
        private Map<Integer, String> responseFields;
        private String responseCode;
        private String script;
        private String scriptLanguage;
        @Builder.Default
        private Boolean scriptEnabled = false;
        private Integer delayMs;
        @Builder.Default
        private Boolean enabled = true;
    }
}
