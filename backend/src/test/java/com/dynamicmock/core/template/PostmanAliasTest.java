package com.dynamicmock.core.template;

import com.dynamicmock.adapter.out.template.ResponseTemplateEngine;
import com.dynamicmock.adapter.out.script.ScriptEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Postman-compatible template variable aliases.
 * Verifies that Postman-standard dynamic variables resolve correctly
 * and that aliases never shadow route-level context variables.
 */
@ExtendWith(MockitoExtension.class)
class PostmanAliasTest {

    private ResponseTemplateEngine templateEngine;

    @Mock
    private ScriptEngine scriptEngine;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        templateEngine = new ResponseTemplateEngine(scriptEngine, objectMapper);
    }

    private String extractQuotedValue(String result, String key) {
        assertTrue(result.contains("\"" + key + "\":\""), "Key '" + key + "' not found in: " + result);
        return result.replaceAll(".*\"" + key + "\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void testGuidAlias() {
        // Postman {{$guid}} must resolve to a UUID in the same format as {{$randomUUID}}
        String result = templateEngine.render("{\"id\":\"{{$guid}}\"}");
        String guid = extractQuotedValue(result, "id");
        assertDoesNotThrow(() -> UUID.fromString(guid));
    }

    @Test
    void testIsoTimestampAlias() {
        // Postman {{$isoTimestamp}} must be an ISO-8601 instant
        String result = templateEngine.render("{\"ts\":\"{{$isoTimestamp}}\"}");
        String ts = extractQuotedValue(result, "ts");
        assertDoesNotThrow(() -> Instant.parse(ts));
    }

    @Test
    void testNowAlias() {
        // Postman {{now}} must resolve to a numeric epoch-milliseconds timestamp
        String result = templateEngine.render("{\"now\":{{now}} }");
        assertTrue(result.contains("\"now\":"), "Key 'now' not found in: " + result);
        String num = result.replaceAll(".*\"now\":(\\d+).*", "$1");
        long value = Long.parseLong(num);
        assertTrue(value > 0);
    }

    @Test
    void testUuidAliasGeneratesWhenAbsentFromContext() {
        // {{uuid}} with no context entry must generate a random UUID
        String result = templateEngine.render("{\"uuid\":\"{{uuid}}\"}");
        String uuid = extractQuotedValue(result, "uuid");
        assertDoesNotThrow(() -> UUID.fromString(uuid));
    }

    @Test
    void testUuidDoesNotShadowContextVariable() {
        // A route-level variable named 'uuid' (path variable / script var)
        // must win over the alias helper
        Map<String, Object> context = Map.of("uuid", "route-uuid-literal");
        String result = templateEngine.render("{\"uuid\":\"{{uuid}}\"}", context);
        String uuid = extractQuotedValue(result, "uuid");
        assertEquals("route-uuid-literal", uuid);
    }

    @Test
    void testNowDoesNotShadowContextVariable() {
        // Same shadowing rule for 'now'
        Map<String, Object> context = Map.of("now", "route-now-literal");
        String result = templateEngine.render("{\"now\":\"{{now}}\"}", context);
        String now = extractQuotedValue(result, "now");
        assertEquals("route-now-literal", now);
    }

    @Test
    void testRandomFirstNameAlias() {
        String result = templateEngine.render("{\"first\":\"{{$randomFirstName}}\"}");
        String first = extractQuotedValue(result, "first");
        assertFalse(first.isEmpty());
        assertFalse(first.contains("@"), "First name must not be an email");
    }

    @Test
    void testRandomLastNameAlias() {
        String result = templateEngine.render("{\"last\":\"{{$randomLastName}}\"}");
        String last = extractQuotedValue(result, "last");
        assertFalse(last.isEmpty());
    }
}
