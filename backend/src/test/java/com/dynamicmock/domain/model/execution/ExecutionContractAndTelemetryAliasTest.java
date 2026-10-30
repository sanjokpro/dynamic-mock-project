package com.dynamicmock.domain.model.execution;

import com.dynamicmock.adapter.in.web.dto.WebhookExecutionDetail;
import com.dynamicmock.adapter.out.script.ScriptContext;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.application.service.TrafficLogger;
import com.dynamicmock.domain.entity.MockRoute;
import com.dynamicmock.infrastructure.filter.RequestMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * v0.6.2 Execution Contract & Telemetry Alias Standardization Verification Suite.
 */
class ExecutionContractAndTelemetryAliasTest {

    @Test
    @DisplayName("1. executionMode canonical normalization and 6-step precedence resolution")
    void shouldNormalizeExecutionModeWithDeterministicPrecedence() {
        // Default is ASYNC when null or empty
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(null));
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(Map.of()));

        // Canonical executionMode values
        assertEquals(ExecutionMode.SYNC, ExecutionMode.resolve(Map.of("executionMode", "sync")));
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(Map.of("executionMode", "async")));

        // Precedence 1 > 2: executionMode overrides mode
        Map<String, Object> execOverridesMode = new LinkedHashMap<>();
        execOverridesMode.put("executionMode", "async");
        execOverridesMode.put("mode", "sync");
        execOverridesMode.put("blocking", true);
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(execOverridesMode));

        // Precedence 2 > 3: mode overrides blocking
        Map<String, Object> modeOverridesBlocking = new LinkedHashMap<>();
        modeOverridesBlocking.put("mode", "async");
        modeOverridesBlocking.put("blocking", true);
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(modeOverridesBlocking));

        // Precedence 3 > 4: blocking overrides sync
        Map<String, Object> blockingOverridesSync = new LinkedHashMap<>();
        blockingOverridesSync.put("blocking", false);
        blockingOverridesSync.put("sync", true);
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(blockingOverridesSync));

        // Precedence 4 > 5: sync overrides async
        Map<String, Object> syncOverridesAsync = new LinkedHashMap<>();
        syncOverridesAsync.put("sync", true);
        syncOverridesAsync.put("async", true);
        assertEquals(ExecutionMode.SYNC, ExecutionMode.resolve(syncOverridesAsync));

        // Precedence 5: async boolean flag
        assertEquals(ExecutionMode.SYNC, ExecutionMode.resolve(Map.of("async", false)));
        assertEquals(ExecutionMode.ASYNC, ExecutionMode.resolve(Map.of("async", true)));
    }

    @Test
    @DisplayName("2. blocking compatibility: blocking=true -> SYNC, blocking=false -> ASYNC")
    void shouldMaintainBlockingBackwardCompatibility() {
        MockActionContract syncContract = MockActionContract.fromConfig(Map.of(
            "url", "http://localhost:8080/mock/callback",
            "blocking", true
        ));
        assertEquals(ExecutionMode.SYNC, syncContract.getExecutionMode());
        assertTrue(syncContract.isSynchronous());
        assertTrue(syncContract.isFollowRedirects());
        assertTrue(syncContract.isReturnResponseToCaller());

        MockActionContract asyncContract = MockActionContract.fromConfig(Map.of(
            "url", "http://localhost:8080/mock/callback",
            "blocking", false
        ));
        assertEquals(ExecutionMode.ASYNC, asyncContract.getExecutionMode());
        assertFalse(asyncContract.isSynchronous());

        // WebhookExecutionDetail DTO synchronizes executionMode and blocking
        WebhookExecutionDetail detailFromBlocking = WebhookExecutionDetail.builder()
            .blocking(true)
            .build();
        assertEquals("sync", detailFromBlocking.getExecutionMode());
        assertEquals(Boolean.TRUE, detailFromBlocking.getBlocking());

        WebhookExecutionDetail detailFromMode = WebhookExecutionDetail.builder()
            .executionMode("async")
            .build();
        assertEquals("async", detailFromMode.getExecutionMode());
        assertEquals(Boolean.FALSE, detailFromMode.getBlocking());
    }

    @Test
    @DisplayName("3. mode alias compatibility: mode=sync, mode=blocking, mode=async")
    void shouldSupportModeAliasCompatibility() {
        assertEquals(ExecutionMode.SYNC, MockActionContract.fromConfig(Map.of("mode", "sync")).getExecutionMode());
        assertEquals(ExecutionMode.SYNC, MockActionContract.fromConfig(Map.of("mode", "blocking")).getExecutionMode());
        assertEquals(ExecutionMode.ASYNC, MockActionContract.fromConfig(Map.of("mode", "async")).getExecutionMode());
    }

    @Test
    @DisplayName("4. matchedRoute dual emission with matchName in TrafficLogger.ExecutionEvent")
    void shouldDualEmitMatchedRouteAndMatchName() {
        // When only matchedRoute is provided, matchName is automatically populated
        TrafficLogger.ExecutionEvent fromCanonical = TrafficLogger.ExecutionEvent.builder()
            .id("evt-1")
            .matchedRoute("GET /mobile-banking/login")
            .build();
        assertEquals("GET /mobile-banking/login", fromCanonical.getMatchedRoute());
        assertEquals("GET /mobile-banking/login", fromCanonical.getMatchName());

        // When only matchName is provided, matchedRoute is automatically populated
        TrafficLogger.ExecutionEvent fromLegacy = TrafficLogger.ExecutionEvent.builder()
            .id("evt-2")
            .matchName("POST /legacy/webhook")
            .build();
        assertEquals("POST /legacy/webhook", fromLegacy.getMatchedRoute());
        assertEquals("POST /legacy/webhook", fromLegacy.getMatchName());

        // Setters also synchronize when counterpart is null
        TrafficLogger.ExecutionEvent setterEvent = new TrafficLogger.ExecutionEvent();
        setterEvent.setMatchedRoute("PUT /items/{id}");
        assertEquals("PUT /items/{id}", setterEvent.getMatchName());
    }

    @Test
    @DisplayName("5. formData canonical and formParams alias behavior in RequestMatcher and ScriptContext")
    void shouldSupportFormDataAndFormParamsAlias() {
        RequestMatcher matcher = new RequestMatcher();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mock/login");
        request.setContentType("application/x-www-form-urlencoded");
        String body = "username=alice&returnUrl=http%3A%2F%2Flocalhost%2Fmock%2Fdone";

        // Route using canonical formData matcher
        MockRoute canonicalRoute = MockRoute.builder()
            .id("r-1")
            .matchers(Map.of("formData", Map.of("username", "alice", "returnUrl", "http://localhost/mock/done")))
            .build();
        assertTrue(matcher.matches(request, canonicalRoute, body));

        // Route using legacy formParams alias matcher
        MockRoute aliasRoute = MockRoute.builder()
            .id("r-2")
            .matchers(Map.of("formParams", Map.of("username", "alice")))
            .build();
        assertTrue(matcher.matches(request, aliasRoute, body));

        // Canonical formData takes precedence over formParams when both specify the same key
        Map<String, Object> bothMatchers = new HashMap<>();
        bothMatchers.put("formParams", Map.of("username", "wrong_user"));
        bothMatchers.put("formData", Map.of("username", "alice"));
        MockRoute precedenceRoute = MockRoute.builder()
            .id("r-3")
            .matchers(bothMatchers)
            .build();
        assertTrue(matcher.matches(request, precedenceRoute, body));
    }

    @Test
    @DisplayName("6. legacy script compatibility and formData access in GraalVM ScriptEngine")
    void shouldSupportLegacyWebhookScriptAndFormDataInScriptEngine() {
        ScriptEngine scriptEngine = new ScriptEngine(5, 128, false, false);
        Map<String, String> parsedForm = RequestMatcher.parseFormUrlEncodedBody("action=login&returnUrl=http%3A%2F%2Flocalhost%2Fmock%2Fcb");

        ScriptContext context = ScriptContext.builder()
            .method("POST")
            .path("/mock/mobile-banking/login")
            .formData(parsedForm)
            .status(200)
            .responseHeaders(new HashMap<>())
            .variables(new HashMap<>())
            .state(new HashMap<>())
            .build();

        String legacyScript = """
            var urlFromFormData = request.formData.returnUrl;
            var urlFromFormParams = request.formParams.returnUrl;
            if (urlFromFormData === urlFromFormParams) {
              webhook = {
                url: urlFromFormData,
                blocking: true
              };
            }
            """;

        ScriptEngine.ScriptExecutionResult result = scriptEngine.execute(legacyScript, "js", context);
        Map<String, Object> webhookConfig = result.getContext().getWebhook();
        assertNotNull(webhookConfig);
        assertEquals("http://localhost/mock/cb", webhookConfig.get("url"));

        MockActionContract contract = MockActionContract.fromConfig(webhookConfig);
        assertEquals(ExecutionMode.SYNC, contract.getExecutionMode());
        assertTrue(contract.isFollowRedirects());
        assertTrue(contract.isReturnResponseToCaller());
    }
}

