package com.dynamicmock.core.template;

import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.dynamicmock.application.service.PostmanImportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// PostmanImportService constructor: (RouteService, WorkspaceCollectionService,
// WorkspaceEnvironmentService, ObjectMapper) — all null-safe for unit tests that
// only exercise the pure translatePostmanTemplate method.

/**
 * Phase 1 – Postman Compatibility: Security Invariants & Template Engine Foundation
 *
 * <p>Tests all 9 required scenarios from the v0.6.2 implementation plan:</p>
 * <ol>
 *   <li>$timestampSeconds returns a 10-digit value</li>
 *   <li>$postmanTimestamp returns a 10-digit value</li>
 *   <li>Native $timestamp still returns a 13-digit value</li>
 *   <li>$queryParams reads an incoming query parameter from request context</li>
 *   <li>$queryParams returns empty string for absent parameter (no exception)</li>
 *   <li>$queryParams returns empty string when request context is missing entirely</li>
 *   <li>Import-time translation: Postman {{$timestamp}} → {{$timestampSeconds}} (10 digits)</li>
 *   <li>Native Dynamic Mock template {{$timestamp}} is NOT translated (still 13 digits)</li>
 *   <li>Existing helpers ($guid, $randomUUID) still resolve correctly</li>
 * </ol>
 *
 * <p>iframe sandbox invariants are separately covered by HtmlPreviewFrame.test.tsx in the
 * frontend test suite — no duplication here.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 1 – Postman Template Compatibility")
class Phase1PostmanCompatibilityTest {

    @Mock
    private ScriptEngine scriptEngine;

    private ResponseTemplateEngine engine;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        engine = new ResponseTemplateEngine(scriptEngine, objectMapper);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 1: $timestampSeconds → 10-digit epoch seconds
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("1. {{$timestampSeconds}} returns a 10-digit epoch-second value")
    void timestampSeconds_returns_10_digits() {
        String result = engine.render("{{$timestampSeconds}}", Map.of());

        assertNotNull(result, "$timestampSeconds must not be null");
        assertEquals(10, result.length(),
                "Expected 10-digit epoch seconds, got length " + result.length() + ": " + result);
        assertDoesNotThrow(() -> Long.parseLong(result), "$timestampSeconds must be a valid long");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 2: $postmanTimestamp → 10-digit epoch seconds (Postman alias)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("2. {{$postmanTimestamp}} returns a 10-digit epoch-second value")
    void postmanTimestamp_returns_10_digits() {
        String result = engine.render("{{$postmanTimestamp}}", Map.of());

        assertNotNull(result);
        assertEquals(10, result.length(),
                "Expected 10-digit epoch seconds, got length " + result.length() + ": " + result);
        assertDoesNotThrow(() -> Long.parseLong(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 3: Native {{$timestamp}} still returns 13-digit milliseconds
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("3. Native {{$timestamp}} still returns a 13-digit epoch-millisecond value (unchanged)")
    void nativeTimestamp_still_returns_13_digits() {
        String result = engine.render("{{$timestamp}}", Map.of());

        assertNotNull(result);
        assertEquals(13, result.length(),
                "Native {{$timestamp}} must remain 13-digit milliseconds, got: " + result);
        assertDoesNotThrow(() -> Long.parseLong(result));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 4: $queryParams reads a named query parameter from request context
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("4. {{$queryParams 'ACC'}} returns the ACC query-parameter value from request context")
    void queryParams_reads_named_param() {
        Map<String, Object> ctx = Map.of(
                "request", Map.of(
                        "queryParams", Map.of("ACC", "9801879262", "VM", "AV")
                )
        );

        String result = engine.render("{{$queryParams 'ACC'}}", ctx);
        assertEquals("9801879262", result);
    }

    @Test
    @DisplayName("4b. {{$queryParams 'VM'}} reads a second query parameter from the same context")
    void queryParams_reads_another_named_param() {
        Map<String, Object> ctx = Map.of(
                "request", Map.of(
                        "queryParams", Map.of("ACC", "9801879262", "VM", "AV")
                )
        );

        String result = engine.render("{{$queryParams 'VM'}}", ctx);
        assertEquals("AV", result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 5: $queryParams returns "" for an absent parameter (no exception)
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("5. {{$queryParams 'MISSING'}} returns empty string without throwing")
    void queryParams_absent_param_returns_empty_string() {
        Map<String, Object> ctx = Map.of(
                "request", Map.of(
                        "queryParams", Map.of("ACC", "9801879262")
                )
        );

        assertDoesNotThrow(() -> {
            String result = engine.render("{{$queryParams 'MISSING'}}", ctx);
            assertEquals("", result, "Absent param must return empty string");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 6: $queryParams returns "" when request context is missing
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("6. {{$queryParams 'ACC'}} returns empty string when no request context is present")
    void queryParams_no_context_returns_empty_string() {
        assertDoesNotThrow(() -> {
            String result = engine.render("{{$queryParams 'ACC'}}", Map.of());
            assertEquals("", result, "No request context must return empty string");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 7: Import-time translation — Postman {{$timestamp}} → {{$timestampSeconds}}
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("7. translatePostmanTemplate replaces {{$timestamp}} with {{$timestampSeconds}}")
    void translatePostmanTemplate_replaces_timestamp_placeholder() {
        PostmanImportService importService = new PostmanImportService(null, null, null, null);

        String postmanBody = "{\"time\": \"{{$timestamp}}\", \"id\": \"{{$guid}}\"}";
        String translated = importService.translatePostmanTemplate(postmanBody);

        // {{$timestamp}} must become {{$timestampSeconds}}
        assertTrue(translated.contains("{{$timestampSeconds}}"),
                "Expected {{$timestampSeconds}} after translation: " + translated);
        // Other helpers must remain untouched
        assertTrue(translated.contains("{{$guid}}"),
                "{{$guid}} must not be altered: " + translated);
        // No raw {{$timestamp}} left (but {{$timestampSeconds}} is fine — check for exact original)
        assertFalse(translated.matches("(?s).*\\{\\{\\s*\\$timestamp\\s*\\}\\}.*"),
                "Raw {{$timestamp}} must be gone after translation: " + translated);

        // The translated template must render a 10-digit timestamp when evaluated
        String rendered = engine.render(translated, Map.of());
        // Extract the "time" value — it appears between the first pair of quotes after "time":
        String timeValue = rendered.replaceAll(".*\"time\":\\s*\"([^\"]+)\".*", "$1");
        assertEquals(10, timeValue.length(),
                "Rendered translated timestamp must be 10 digits: " + timeValue);
    }

    @Test
    @DisplayName("7b. translatePostmanTemplate handles whitespace variants: {{ $timestamp }}")
    void translatePostmanTemplate_handles_whitespace_variants() {
        PostmanImportService importService = new PostmanImportService(null, null, null, null);

        String withSpaces = "{{ $timestamp }}";
        String translated = importService.translatePostmanTemplate(withSpaces);

        assertTrue(translated.contains("{{$timestampSeconds}}"),
                "Whitespace variant must also be replaced: " + translated);
        assertFalse(translated.matches("(?s).*\\{\\{\\s*\\$timestamp\\s*\\}\\}.*"),
                "No residual {{$timestamp}} after replacement: " + translated);
    }

    @Test
    @DisplayName("7c. translatePostmanTemplate returns null/empty input unchanged")
    void translatePostmanTemplate_handles_null_and_empty() {
        PostmanImportService importService = new PostmanImportService(null, null, null, null);

        assertNull(importService.translatePostmanTemplate(null));
        assertEquals("", importService.translatePostmanTemplate(""));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 8: Native Dynamic Mock template {{$timestamp}} is NOT translated
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("8. Native Dynamic Mock {{$timestamp}} is not rewritten — still renders 13-digit ms")
    void nativeDynamicMockTemplate_not_translated() {
        // A native Dynamic Mock template passed directly to the engine must remain unchanged
        String nativeTemplate = "{\"ts\": \"{{$timestamp}}\"}";

        // translatePostmanTemplate is ONLY called during Postman import — not here
        String rendered = engine.render(nativeTemplate, Map.of());
        String tsValue = rendered.replaceAll(".*\"ts\":\\s*\"([^\"]+)\".*", "$1");

        assertEquals(13, tsValue.length(),
                "Native Dynamic Mock {{$timestamp}} must still produce 13-digit ms: " + tsValue);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scenario 9: Existing helpers still resolve
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("9. Existing helper {{$guid}} still resolves to a UUID-formatted string")
    void existingHelper_guid_still_works() {
        String result = engine.render("{{$guid}}", Map.of());

        assertNotNull(result);
        assertFalse(result.isBlank());
        // UUID pattern: 8-4-4-4-12 hex digits
        assertTrue(result.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "{{$guid}} must be a UUID: " + result);
    }

    @Test
    @DisplayName("9b. Existing helper {{$randomUUID}} still resolves to a UUID-formatted string")
    void existingHelper_randomUUID_still_works() {
        String result = engine.render("{{$randomUUID}}", Map.of());

        assertNotNull(result);
        assertTrue(result.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "{{$randomUUID}} must be a UUID: " + result);
    }

    @Test
    @DisplayName("9c. Existing helper {{$isoTimestamp}} still resolves to an ISO-8601 string")
    void existingHelper_isoTimestamp_still_works() {
        String result = engine.render("{{$isoTimestamp}}", Map.of());

        assertNotNull(result);
        // ISO-8601: 2026-11-12T05:40:00Z (at minimum 20 chars)
        assertTrue(result.length() >= 20, "ISO timestamp must be at least 20 chars: " + result);
        assertTrue(result.contains("T"), "ISO timestamp must contain 'T' separator: " + result);
        assertTrue(result.endsWith("Z"), "ISO timestamp must end with 'Z': " + result);
    }
}
