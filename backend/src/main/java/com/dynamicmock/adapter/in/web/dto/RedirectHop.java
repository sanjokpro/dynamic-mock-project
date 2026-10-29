package com.dynamicmock.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedirectHop {
    private int step;
    private String method;
    private String url;
    private int status;
    private String location;
    private double durationMs;
}
