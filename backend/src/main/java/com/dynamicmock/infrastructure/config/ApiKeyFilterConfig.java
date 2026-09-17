package com.dynamicmock.infrastructure.config;

import com.dynamicmock.infrastructure.filter.ApiKeyAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyFilterConfig {

    @Value("${app.api.key:default-dev-key}")
    private String apiKey;

    @Bean
    public FilterRegistrationBean<ApiKeyAuthFilter> apiKeyAuthFilterRegistration() {
        FilterRegistrationBean<ApiKeyAuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ApiKeyAuthFilter(apiKey));
        registrationBean.addUrlPatterns("/api", "/api/*");
        registrationBean.setOrder(1); // Run early in the chain
        return registrationBean;
    }
}
