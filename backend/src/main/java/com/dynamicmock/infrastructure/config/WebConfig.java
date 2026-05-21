package com.dynamicmock.infrastructure.config;

import com.dynamicmock.infrastructure.filter.DynamicRouteDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration to register the dynamic route dispatcher interceptor
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    
    private final DynamicRouteDispatcher dynamicRouteDispatcher;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dynamicRouteDispatcher)
            .addPathPatterns("/mock/**");
    }
}

