package com.dynamicmock.infrastructure.filter;

import com.dynamicmock.domain.entity.MockRoute;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Matches incoming requests against route matchers (headers, query params, body)
 */
@Slf4j
@Component
public class RequestMatcher {

    /**
     * Check if the request matches the route's matchers
     * @param request the incoming HTTP request
     * @param route the route to match against
     * @param requestBody the request body (pre-read)
     * @return true if the request matches all matchers, false otherwise
     */
    public boolean matches(HttpServletRequest request, MockRoute route, String requestBody) {
        Map<String, Object> matchers = route.getMatchers();
        
        if (matchers == null || matchers.isEmpty()) {
            return true; // No matchers = match all
        }

        // Check header matchers
        @SuppressWarnings("unchecked")
        Map<String, String> headerMatchers = (Map<String, String>) matchers.get("headers");
        if (headerMatchers != null && !matchHeaders(request, headerMatchers)) {
            log.debug("Header matching failed for route {}", route.getId());
            return false;
        }

        // Check query param matchers
        @SuppressWarnings("unchecked")
        Map<String, String> queryMatchers = (Map<String, String>) matchers.get("queryParams");
        if (queryMatchers != null && !matchQueryParams(request, queryMatchers)) {
            log.debug("Query param matching failed for route {}", route.getId());
            return false;
        }

        // Check body matchers
        String bodyMatchType = (String) matchers.get("bodyMatchType");
        String bodyMatchPattern = (String) matchers.get("bodyMatchPattern");
        if (bodyMatchType != null && !"none".equals(bodyMatchType) && bodyMatchPattern != null) {
            if (!matchBody(requestBody, bodyMatchType, bodyMatchPattern)) {
                log.debug("Body matching failed for route {}", route.getId());
                return false;
            }
        }

        return true;
    }

    private boolean matchHeaders(HttpServletRequest request, Map<String, String> headerMatchers) {
        for (Map.Entry<String, String> entry : headerMatchers.entrySet()) {
            String headerName = entry.getKey();
            String expectedPattern = entry.getValue();
            String actualValue = request.getHeader(headerName);

            if (actualValue == null) {
                log.debug("Header '{}' not found in request", headerName);
                return false;
            }

            if (!matchesPattern(actualValue, expectedPattern)) {
                log.debug("Header '{}' value '{}' does not match pattern '{}'", 
                    headerName, actualValue, expectedPattern);
                return false;
            }
        }
        return true;
    }

    private boolean matchQueryParams(HttpServletRequest request, Map<String, String> queryMatchers) {
        for (Map.Entry<String, String> entry : queryMatchers.entrySet()) {
            String paramName = entry.getKey();
            String expectedPattern = entry.getValue();
            String actualValue = request.getParameter(paramName);

            if (actualValue == null) {
                log.debug("Query param '{}' not found in request", paramName);
                return false;
            }

            if (!matchesPattern(actualValue, expectedPattern)) {
                log.debug("Query param '{}' value '{}' does not match pattern '{}'", 
                    paramName, actualValue, expectedPattern);
                return false;
            }
        }
        return true;
    }

    private boolean matchBody(String requestBody, String matchType, String pattern) {
        if (requestBody == null || requestBody.isEmpty()) {
            return false;
        }

        switch (matchType.toLowerCase()) {
            case "equals":
                return requestBody.trim().equals(pattern.trim());

            case "contains":
                return requestBody.contains(pattern);

            case "regex":
                return matchesPattern(requestBody, pattern);

            case "jsonpath":
                return matchJsonPath(requestBody, pattern);

            default:
                log.warn("Unknown body match type: {}", matchType);
                return false;
        }
    }

    private boolean matchesPattern(String value, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        
        try {
            // First try exact match
            if (value.equals(pattern)) {
                return true;
            }
            // Then try regex
            return Pattern.compile(pattern).matcher(value).matches();
        } catch (Exception e) {
            log.warn("Invalid regex pattern '{}': {}", pattern, e.getMessage());
            // Fall back to exact match
            return value.equals(pattern);
        }
    }

    private boolean matchJsonPath(String requestBody, String jsonPathExpression) {
        try {
            // Support expressions like "$.user.id == 123" or just "$.user.name"
            if (jsonPathExpression.contains("==")) {
                String[] parts = jsonPathExpression.split("==", 2);
                String path = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                Object actualValue = JsonPath.read(requestBody, path);
                String actualString = actualValue != null ? actualValue.toString() : "";
                
                // Remove quotes from expected value if present
                if (expectedValue.startsWith("\"") && expectedValue.endsWith("\"")) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                if (expectedValue.startsWith("'") && expectedValue.endsWith("'")) {
                    expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                }
                
                return actualString.equals(expectedValue);
            } else if (jsonPathExpression.contains("!=")) {
                String[] parts = jsonPathExpression.split("!=", 2);
                String path = parts[0].trim();
                String expectedValue = parts[1].trim();
                
                Object actualValue = JsonPath.read(requestBody, path);
                String actualString = actualValue != null ? actualValue.toString() : "";
                
                return !actualString.equals(expectedValue.replace("\"", "").replace("'", ""));
            } else {
                // Just check if the path exists and returns a non-null value
                Object result = JsonPath.read(requestBody, jsonPathExpression);
                return result != null;
            }
        } catch (PathNotFoundException e) {
            log.debug("JSONPath '{}' not found in body", jsonPathExpression);
            return false;
        } catch (Exception e) {
            log.warn("JSONPath evaluation failed for '{}': {}", jsonPathExpression, e.getMessage());
            return false;
        }
    }
}

