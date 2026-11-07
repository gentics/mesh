package io.gentics.mesh.mcp.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Prevents accidental exposure of sensitive field values in MCP tool responses.
 *
 * <p>Gentics Mesh schemas may contain fields whose names suggest sensitive
 * content (e.g. {@code apiKey}, {@code accessToken}). This service intercepts
 * all node field maps before they are returned to the LLM client and replaces
 * the values of matching fields with the placeholder {@code "[REDACTED]"}.
 *
 * <h2>Matching strategy</h2>
 * Field names are matched <em>case-insensitively</em> against the
 * {@link #SENSITIVE_KEYS} set. The check is applied recursively so that
 * nested objects (e.g. a {@code credentials} field containing a sub-object
 * with a {@code token} key) are also redacted.
 *
 * <h2>Redacted key names</h2>
 * <pre>
 *   password, token, secret, apikey, api_key, authorization,
 *   credential, credentials, private, privatekey, private_key,
 *   sessionid, session_id, accesstoken, access_token,
 *   refreshtoken, refresh_token, bearer
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Map<String, Object> raw     = fetchFieldsFromMesh();
 * Map<String, Object> cleaned = RedactionService.redact(raw);
 * // "token" field → "[REDACTED]", all other fields unchanged
 * }</pre>
 *
 * @since 0.1.0
 */
public final class RedactionService {

    /**
     * Lowercase field names whose values will be replaced with {@link #REDACTED}.
     * Comparison is always done in lowercase (see {@link #redact(Map)}).
     */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "token", "secret", "apikey", "api_key",
            "authorization", "credential", "credentials",
            "private", "privatekey", "private_key",
            "sessionid", "session_id",
            "accesstoken", "access_token",
            "refreshtoken", "refresh_token",
            "bearer"
    );

    /** Replacement value for all sensitive fields. */
    static final String REDACTED = "[REDACTED]";

    /** Utility class – do not instantiate. */
    private RedactionService() {}

    /**
     * Returns a new map with all sensitive keys replaced by {@link #REDACTED}.
     * Works recursively on nested {@link Map} values. The input map is never
     * mutated; insertion order is preserved via {@link LinkedHashMap}.
     *
     * @param input the raw field map from Gentics Mesh; may be {@code null}
     * @return a new map with sensitive values redacted, or {@code null} if
     *         {@code input} was {@code null}
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> redact(Map<String, Object> input) {
        if (input == null) return null;
        var result = new LinkedHashMap<String, Object>(input.size());
        for (var entry : input.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (SENSITIVE_KEYS.contains(key.toLowerCase())) {
                result.put(key, REDACTED);
            } else if (val instanceof Map<?, ?> nested) {
                result.put(key, redact((Map<String, Object>) nested));
            } else {
                result.put(key, val);
            }
        }
        return result;
    }

    /**
     * Strips HTML tags from the given string and decodes the most common
     * HTML entities. Used to produce plain-text snippets from HTML body fields.
     *
     * <p>Decoded entities: {@code &amp; &lt; &gt; &quot; &#39; &nbsp;}
     *
     * @param html input HTML string; may be {@code null}
     * @return plain text with tags removed and entities decoded,
     *         or an empty string if {@code html} was {@code null}
     */
    public static String htmlToPlainText(String html) {
        if (html == null) return "";
        return html
                .replaceAll("<[^>]+>", "")
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replace("&nbsp;", " ")
                .strip();
    }

    /**
     * Truncates the given text to at most {@code maxLength} characters and
     * appends an ellipsis ({@code …}) if truncation occurred.
     *
     * @param text      input text; may be {@code null}
     * @param maxLength maximum number of characters to keep
     * @return the original string if it fits, or a truncated version with
     *         {@code …} appended; {@code null} if {@code text} was {@code null}
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "…";
    }
}
