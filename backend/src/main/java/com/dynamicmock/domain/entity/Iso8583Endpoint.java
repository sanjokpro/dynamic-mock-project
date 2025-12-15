package com.dynamicmock.domain.entity;

/**
 * DOMAIN LAYER - Entity
 * Clean Architecture: ISO8583 financial message server configuration.
 * Encapsulates message routing and response generation rules.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ISO8583 Server Configuration
 * 
 * Unlike REST APIs, ISO8583 doesn't have "endpoints" - it's message-based.
 * Multiple mocks share the same port, differentiated by MTI + field matchers.
 * 
 * Architecture:
 * ┌─────────────────────────────────────────────────────────────┐
 * │  ISO8583 Server (Port: 8583)                                │
 * │  ┌───────────────────────────────────────────────────────┐  │
 * │  │ Interceptor Script (Optional - GraalVM)               │  │
 * │  │ Runs for ALL messages before routing                  │  │
 * │  └───────────────────────────────────────────────────────┘  │
 * │                           │                                  │
 * │                           ▼                                  │
 * │  ┌─────────────────────────────────────────────────────┐    │
 * │  │ Message Router (MTI + field matchers)               │    │
 * │  │                                                      │    │
 * │  │  Mock A: MTI=0100, PAN starts with "4111"           │    │
 * │  │  Mock B: MTI=0100, PAN starts with "5500"           │    │
 * │  │  Mock C: MTI=0200, any                              │    │
 * │  │  Mock D: MTI=0800, network management               │    │
 * │  └─────────────────────────────────────────────────────┘    │
 * └─────────────────────────────────────────────────────────────┘
 */
@Document(collection = "iso8583_servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583Endpoint {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    
    /**
     * TCP port for ISO8583 server.
     * Multiple mocks can share the same port (default behavior).
     */
    @Indexed
    private Integer port;
    
    /**
     * Whether this server runs on an isolated port.
     * - false (default): Shares port with other servers, mocks routed by MTI/matchers
     * - true: Dedicated port for this server only
     */
    @Builder.Default
    private Boolean isolatedPort = false;
    
    /**
     * Mock scenarios for this server.
     * Each mock handles specific message types based on MTI + matchers.
     */
    private List<Iso8583Mock> mocks;
    
    /**
     * Global interceptor script (GraalVM - JS/Python).
     * Runs for ALL messages BEFORE routing to specific mocks.
     * 
     * Use cases:
     * - Logging all transactions
     * - Transform/enrich messages
     * - Global validation
     * - Custom routing logic
     * 
     * Available in script context:
     * - request: ISOMsg fields as map
     * - mti: Message type indicator
     * - response: Mutable response object
     * - state: Shared state across requests
     */
    private String interceptorScript;
    private String interceptorScriptLanguage;
    
    /**
     * Whether interceptor is enabled (disabled by default)
     */
    @Builder.Default
    private Boolean interceptorEnabled = false;
    
    /**
     * Advanced: Custom jPOS server XML configuration.
     * For users who want full control over jPOS settings.
     * 
     * Leave null to use auto-generated config.
     * Only shown in UI when "Advanced Mode" is enabled.
     */
    private String customServerXml;
    
    /**
     * Whether custom XML editor is enabled (disabled by default)
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
     * Leave null to use default packager.xml
     */
    private String packagerConfig;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Individual mock scenario within a server.
     * Handles specific message types based on MTI + field matchers.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Iso8583Mock {
        
        private String id;
        private String name;
        private String description;
        
        /**
         * Message Type Indicator to match.
         * 0100 - Authorization Request
         * 0200 - Financial Request  
         * 0400 - Reversal Request
         * 0800 - Network Management
         */
        private String mti;
        
        /**
         * Response MTI (auto-calculated if null: 0100→0110)
         */
        private String responseMti;
        
        /**
         * Field matchers for routing.
         * Examples:
         * - "field.2": "^4111.*"     (PAN starts with 4111)
         * - "field.18": "5411"        (MCC = grocery)
         * - "field.41": "TERM001"     (specific terminal)
         * 
         * Supports regex patterns.
         */
        private Map<String, String> matchers;
        
        /**
         * Priority for matching (higher = checked first).
         * Default: 0
         */
        @Builder.Default
        private Integer priority = 0;
        
        /**
         * Response field values.
         * Key: field number
         * Value: literal value or Handlebars template
         * 
         * Examples:
         * - 39: "00"                           (approved)
         * - 38: "{{$randomAlphanumeric 6}}"   (auth code)
         * - 54: "{{request.4}}"               (echo amount)
         */
        private Map<Integer, String> responseFields;
        
        /**
         * Response code (field 39).
         * Common values:
         * - "00" = Approved
         * - "05" = Do not honor
         * - "14" = Invalid card number
         * - "51" = Insufficient funds
         * - "54" = Expired card
         */
        private String responseCode;
        
        /**
         * Response script (GraalVM - JS/Python).
         * For complex response logic beyond templates.
         * 
         * Available in script context:
         * - request: ISOMsg fields as map
         * - response: Mutable response fields map
         * - mti: Message type indicator
         * - state: Shared state across requests
         */
        private String script;
        private String scriptLanguage;
        
        /**
         * Whether script is enabled (disabled by default)
         */
        @Builder.Default
        private Boolean scriptEnabled = false;
        
        /**
         * Simulated delay in milliseconds
         */
        private Integer delayMs;
        
        /**
         * Whether this mock is enabled
         */
        @Builder.Default
        private Boolean enabled = true;
    }
    
    // Legacy field - keeping for backward compatibility
    @Deprecated
    private List<MessageConfig> messageConfigs;
    
    @Deprecated
    private Map<Integer, FieldDefinition> fieldDefinitions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated
    public static class MessageConfig {
        private String mti;
        private String responseMti;
        private Map<Integer, String> responseFields;
        private String script;
        private String scriptLanguage;
        private Integer delayMs;
        private Map<String, Object> matchers;
        private String responseCode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Deprecated
    public static class FieldDefinition {
        private Integer fieldNumber;
        private String name;
        private String dataType;
        private String lengthType;
        private Integer length;
    }
}
