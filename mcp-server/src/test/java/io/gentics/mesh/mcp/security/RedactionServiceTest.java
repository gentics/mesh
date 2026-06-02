package io.gentics.mesh.mcp.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RedactionService}.
 *
 * <p>Covers {@link RedactionService#redact(Map)}, {@link RedactionService#htmlToPlainText(String)},
 * and {@link RedactionService#truncate(String, int)}.
 */
class RedactionServiceTest {

    // ══════════════════════════════════════════════════════════════════════
    // redact()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void redact_null_returnsNull() {
        assertNull(RedactionService.redact(null), "redact(null) must return null");
    }

    @Test
    void redact_emptyMap_returnsEmpty() {
        assertTrue(RedactionService.redact(Map.of()).isEmpty(),
                "redact(emptyMap) must return an empty map");
    }

    @Test
    void redact_nonSensitiveKey_passesThrough() {
        Map<String, Object> input = Map.of("title", "Hello World");
        Map<String, Object> result = RedactionService.redact(input);
        assertEquals("Hello World", result.get("title"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "password", "token", "secret", "apikey", "api_key",
        "authorization", "credential", "credentials",
        "private", "privatekey", "private_key",
        "sessionid", "session_id",
        "accesstoken", "access_token",
        "refreshtoken", "refresh_token",
        "bearer"
    })
    void redact_sensitiveLowercaseKey_redacts(String key) {
        Map<String, Object> input = Map.of(key, "super-secret-value");
        Map<String, Object> result = RedactionService.redact(input);
        assertEquals("[REDACTED]", result.get(key),
                "Key '" + key + "' should be redacted");
    }

    @Test
    void redact_sensitiveKeyUppercase_redacts() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("PASSWORD", "s3cr3t");
        input.put("TOKEN",    "abc123");
        Map<String, Object> result = RedactionService.redact(input);
        assertEquals("[REDACTED]", result.get("PASSWORD"));
        assertEquals("[REDACTED]", result.get("TOKEN"));
    }

    @Test
    void redact_mixedCaseSensitiveKey_redacts() {
        Map<String, Object> input = Map.of("AccessToken", "bearer-xyz");
        Map<String, Object> result = RedactionService.redact(input);
        assertEquals("[REDACTED]", result.get("AccessToken"));
    }

    @Test
    void redact_nestedMap_redactsSensitiveKeysRecursively() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("token", "secret-token");
        inner.put("name",  "allowed");
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("credentials", inner);

        Map<String, Object> result = RedactionService.redact(outer);

        // The outer key "credentials" is sensitive itself
        assertEquals("[REDACTED]", result.get("credentials"),
                "'credentials' is a sensitive key and should be redacted at the top level");
    }

    @Test
    void redact_nestedNonSensitiveWrapper_redactsInnerSensitiveKeys() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("token", "secret-token");
        inner.put("name",  "public-name");
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("metadata", inner);   // "metadata" is not sensitive

        Map<String, Object> result = RedactionService.redact(outer);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultInner = (Map<String, Object>) result.get("metadata");
        assertNotNull(resultInner, "non-sensitive wrapper key 'metadata' must survive redaction");
        assertEquals("[REDACTED]",  resultInner.get("token"));
        assertEquals("public-name", resultInner.get("name"));
    }

    @Test
    void redact_inputNotMutated() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("password", "original");
        RedactionService.redact(input);
        assertEquals("original", input.get("password"),
                "Original map must not be modified");
    }

    @Test
    void redact_insertionOrderPreserved() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("z", "last");
        input.put("a", "first");
        input.put("m", "middle");
        Map<String, Object> result = RedactionService.redact(input);
        List<String> keys = List.copyOf(result.keySet());
        assertEquals(List.of("z", "a", "m"), keys);
    }

    @Test
    void redact_listValue_passesThrough() {
        Map<String, Object> input = Map.of("tags", List.of("news", "tech"));
        Map<String, Object> result = RedactionService.redact(input);
        assertEquals(List.of("news", "tech"), result.get("tags"));
    }

    @Test
    void redact_nullValue_passesThrough() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("description", null);
        Map<String, Object> result = RedactionService.redact(input);
        assertTrue(result.containsKey("description"),
                "key 'description' must be present even when its value is null");
        assertNull(result.get("description"),
                "null value must pass through redaction unchanged");
    }

    // ══════════════════════════════════════════════════════════════════════
    // htmlToPlainText()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void htmlToPlainText_null_returnsEmpty() {
        assertEquals("", RedactionService.htmlToPlainText(null));
    }

    @Test
    void htmlToPlainText_emptyString_returnsEmpty() {
        assertEquals("", RedactionService.htmlToPlainText(""));
    }

    @Test
    void htmlToPlainText_stripsSimpleTags() {
        assertEquals("Hello World",
                RedactionService.htmlToPlainText("<p>Hello World</p>"));
    }

    @Test
    void htmlToPlainText_stripsNestedTags() {
        assertEquals("Hello World",
                RedactionService.htmlToPlainText("<div><p><b>Hello</b> World</p></div>"));
    }

    @Test
    void htmlToPlainText_decodesAmpersandEntity() {
        assertEquals("A & B", RedactionService.htmlToPlainText("A &amp; B"));
    }

    @Test
    void htmlToPlainText_decodesLtGt() {
        assertEquals("<script>", RedactionService.htmlToPlainText("&lt;script&gt;"));
    }

    @Test
    void htmlToPlainText_decodesQuoteAndApos() {
        assertEquals("Say \"hello\" it's fine",
                RedactionService.htmlToPlainText("Say &quot;hello&quot; it&#39;s fine"));
    }

    @Test
    void htmlToPlainText_stripsTagsAndDecodesEntities() {
        String html = "<p><b>Foo</b> &amp; <i>Bar</i></p>";
        assertEquals("Foo & Bar", RedactionService.htmlToPlainText(html));
    }

    @Test
    void htmlToPlainText_stripsResult() {
        assertEquals("trimmed", RedactionService.htmlToPlainText("  trimmed  "));
    }

    @Test
    void htmlToPlainText_plainTextPassesThrough() {
        assertEquals("No HTML here", RedactionService.htmlToPlainText("No HTML here"));
    }

    // ══════════════════════════════════════════════════════════════════════
    // truncate()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void truncate_null_returnsNull() {
        assertNull(RedactionService.truncate(null, 10), "truncate(null, n) must return null");
    }

    @Test
    void truncate_shortString_noTruncation() {
        assertEquals("Hello", RedactionService.truncate("Hello", 10));
    }

    @Test
    void truncate_exactLength_noTruncation() {
        assertEquals("abc", RedactionService.truncate("abc", 3));
    }

    @Test
    void truncate_longString_appendsEllipsis() {
        String result = RedactionService.truncate("Hello World", 5);
        assertEquals("Hello…", result);
        assertEquals(6, result.length(),
                "Truncated result must be maxLength chars + 1 ellipsis character = 6 total");
    }

    @Test
    void truncate_to300chars_worksCorrectly() {
        String long300 = "x".repeat(300);
        String long301 = "x".repeat(301);
        assertEquals(long300, RedactionService.truncate(long300, 300));
        assertEquals(long300 + "…", RedactionService.truncate(long301, 300));
    }

    @Test
    void truncate_emptyString_returnsEmpty() {
        assertEquals("", RedactionService.truncate("", 10));
    }
}
