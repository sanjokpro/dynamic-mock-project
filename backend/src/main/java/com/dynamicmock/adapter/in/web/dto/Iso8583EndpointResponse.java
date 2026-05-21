package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.Iso8583Endpoint;
import com.dynamicmock.domain.entity.Iso8583Endpoint.Iso8583Mock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583EndpointResponse {
    private String id;
    private String name;
    private String description;
    private Integer port;
    private Boolean isolatedPort;
    private List<Iso8583Mock> mocks;
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
    
    public static Iso8583EndpointResponse from(Iso8583Endpoint endpoint) {
        return Iso8583EndpointResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .description(endpoint.getDescription())
                .port(endpoint.getPort())
                .isolatedPort(endpoint.getIsolatedPort())
                .mocks(endpoint.getMocks())
                .interceptorScript(endpoint.getInterceptorScript())
                .interceptorScriptLanguage(endpoint.getInterceptorScriptLanguage())
                .interceptorEnabled(endpoint.getInterceptorEnabled())
                .customServerXml(endpoint.getCustomServerXml())
                .customXmlEnabled(endpoint.getCustomXmlEnabled())
                .headerLengthType(endpoint.getHeaderLengthType())
                .encoding(endpoint.getEncoding())
                .packagerConfig(endpoint.getPackagerConfig())
                .active(endpoint.getActive())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}

