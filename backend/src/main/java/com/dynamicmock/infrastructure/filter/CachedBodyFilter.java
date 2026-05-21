package com.dynamicmock.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to cache request body for /mock/** requests
 */
@Component
public class CachedBodyFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/mock")) {
            CachedBodyHttpServletRequest cachedBodyRequest = new CachedBodyHttpServletRequest(request);
            filterChain.doFilter(cachedBodyRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}

