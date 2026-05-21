package com.dynamicmock.infrastructure.persistence.mongodb.entity;

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
 * Infrastructure Layer - MongoDB Entity for ISO8583 Server.
 */
@Document(collection = "iso8583_servers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583EndpointMongoEntity {
    
    @Id
    private String id;
    
    private String name;
    private String description;
    
    @Indexed
    private Integer port;
    
    private Boolean isolatedPort;
    
    private List<Iso8583MockEntity> mocks;
    
    private String interceptorScript;
    private String interceptorScriptLanguage;
    
    private Boolean interceptorEnabled;
    
    private String customServerXml;
    
    private Boolean customXmlEnabled;
    
    private String headerLengthType;
    
    private String encoding;
    
    private String packagerConfig;
    
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Iso8583MockEntity {
        private String id;
        private String name;
        private String description;
        private String mti;
        private String responseMti;
        private Map<String, String> matchers;
        private Integer priority;
        private Map<Integer, String> responseFields;
        private String responseCode;
        private String script;
        private String scriptLanguage;
        private Boolean scriptEnabled;
        private Integer delayMs;
        private Boolean enabled;
    }
}
