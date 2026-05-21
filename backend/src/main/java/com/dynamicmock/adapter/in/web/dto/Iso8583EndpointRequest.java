package com.dynamicmock.adapter.in.web.dto;

import com.dynamicmock.domain.entity.Iso8583Endpoint.Iso8583Mock;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Iso8583EndpointRequest {
    @NotBlank(message = "Name is required")
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
}

