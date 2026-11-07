package io.gentics.mesh.mcp.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AppError}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Constructor and accessor correctness</li>
 *   <li>{@link AppError#toMcpJson()} structure and field values</li>
 *   <li>All static factory methods</li>
 *   <li>{@link AppError#fromHttpStatus(int, String)} mapping</li>
 * </ul>
 */
class AppErrorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Constructor / accessor ────────────────────────────────────────────

    @Test
    void constructor_setsCodeAndMessage() {
        var err = new AppError(ErrorCode.FORBIDDEN, "not allowed");
        assertEquals(ErrorCode.FORBIDDEN, err.getCode());
        assertEquals("not allowed", err.getMessage());
        assertNull(err.getDetails(), "details must be null when not provided");
    }

    @Test
    void constructor_withDetails_setsDetails() {
        var err = new AppError(ErrorCode.VALIDATION_ERROR, "bad input", "some-detail");
        assertEquals("some-detail", err.getDetails());
    }

    @Test
    void constructor_withCause_storesCause() {
        var cause = new RuntimeException("root cause");
        var err   = new AppError(ErrorCode.INTERNAL_ERROR, "wrapped", cause);
        assertEquals(cause, err.getCause());
        assertNull(err.getDetails(), "cause must not bleed into details");
    }

    @Test
    void isRuntimeException() {
        assertTrue(new AppError(ErrorCode.NOT_FOUND, "x") instanceof RuntimeException,
                "AppError must extend RuntimeException");
    }

    // ── toMcpJson ─────────────────────────────────────────────────────────

    @Test
    void toMcpJson_producesCorrectStructure() throws Exception {
        var err  = new AppError(ErrorCode.FORBIDDEN, "forbidden message");
        JsonNode root = MAPPER.readTree(err.toMcpJson());

        assertTrue(root.has("error"), "toMcpJson() must produce a top-level 'error' key");
        JsonNode error = root.get("error");
        assertEquals("forbidden",         error.get("code").asText());
        assertEquals("forbidden message", error.get("message").asText());
        assertTrue(error.get("details").isNull(), "details must be JSON null when not set");
    }

    @Test
    void toMcpJson_codeIsLowercase() throws Exception {
        for (ErrorCode code : ErrorCode.values()) {
            var err  = new AppError(code, "msg");
            JsonNode root = MAPPER.readTree(err.toMcpJson());
            String actual = root.get("error").get("code").asText();
            assertEquals(code.name().toLowerCase(), actual,
                    "Code must be lowercase for " + code);
        }
    }

    @Test
    void toMcpJson_includesDetailsWhenPresent() throws Exception {
        var err  = new AppError(ErrorCode.VALIDATION_ERROR, "bad", "extra-detail");
        JsonNode root  = MAPPER.readTree(err.toMcpJson());
        JsonNode detail = root.get("error").get("details");
        assertFalse(detail.isNull(), "details should not be null when provided");
        assertEquals("extra-detail", detail.asText());
    }

    @Test
    void toMcpJson_isValidJson() {
        var err = new AppError(ErrorCode.UPSTREAM_ERROR, "boom");
        String json = err.toMcpJson();
        assertDoesNotThrow(() -> MAPPER.readTree(json), "toMcpJson() must return valid JSON");
    }

    // ── Factory methods ───────────────────────────────────────────────────

    @Test
    void forbidden_hasCorrectCode() {
        assertEquals(ErrorCode.FORBIDDEN, AppError.forbidden("x").getCode());
    }

    @Test
    void notFound_hasCorrectCode() {
        assertEquals(ErrorCode.NOT_FOUND, AppError.notFound("x").getCode());
    }

    @Test
    void timeout_hasCorrectCode() {
        assertEquals(ErrorCode.TIMEOUT_ERROR, AppError.timeout("x").getCode());
    }

    @Test
    void upstream_hasCorrectCodeAndMessage() {
        var err = AppError.upstream(503, "Service Unavailable");
        assertEquals(ErrorCode.UPSTREAM_ERROR, err.getCode());
        assertTrue(err.getMessage().contains("503"),
                "upstream() message must include the HTTP status code");
        assertTrue(err.getMessage().contains("Service Unavailable"),
                "upstream() message must include the HTTP reason phrase");
    }

    // ── fromHttpStatus ────────────────────────────────────────────────────

    @Test
    void fromHttpStatus_404_mapsToNotFound() {
        assertEquals(ErrorCode.NOT_FOUND,
                AppError.fromHttpStatus(404, "Not Found").getCode());
    }

    @Test
    void fromHttpStatus_401_mapsToForbidden() {
        assertEquals(ErrorCode.FORBIDDEN,
                AppError.fromHttpStatus(401, "Unauthorized").getCode());
    }

    @Test
    void fromHttpStatus_403_mapsToForbidden() {
        assertEquals(ErrorCode.FORBIDDEN,
                AppError.fromHttpStatus(403, "Forbidden").getCode());
    }

    @Test
    void fromHttpStatus_500_mapsToUpstreamError() {
        assertEquals(ErrorCode.UPSTREAM_ERROR,
                AppError.fromHttpStatus(500, "Internal Server Error").getCode());
    }

    @Test
    void fromHttpStatus_503_mapsToUpstreamError() {
        assertEquals(ErrorCode.UPSTREAM_ERROR,
                AppError.fromHttpStatus(503, "Service Unavailable").getCode());
    }

    @Test
    void fromHttpStatus_418_mapsToUpstreamError() {
        assertEquals(ErrorCode.UPSTREAM_ERROR,
                AppError.fromHttpStatus(418, "I'm a teapot").getCode());
    }
}
