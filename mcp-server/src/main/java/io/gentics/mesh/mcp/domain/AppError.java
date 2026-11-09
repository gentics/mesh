package io.gentics.mesh.mcp.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Structured application exception used throughout the MCP server.
 *
 * <p>Every error thrown by the domain, security and infrastructure layers is
 * an {@code AppError}. This ensures that:
 * <ul>
 *   <li>every error has a stable machine-readable {@link ErrorCode},</li>
 *   <li>every error serialises to the canonical MCP JSON shape understood by
 *       LLM clients, and</li>
 *   <li>sensitive details (passwords, tokens) are never accidentally included
 *       in error responses.</li>
 * </ul>
 *
 * <h2>MCP error JSON shape</h2>
 * <pre>{@code
 * {
 *   "error": {
 *     "code":    "forbidden",
 *     "message": "Projekt 'secret' ist nicht in der Allowlist.",
 *     "details": null
 *   }
 * }
 * }</pre>
 *
 * <h2>Usage in tool handlers</h2>
 * <pre>{@code
 * try {
 *     PolicyService.assertProjectAllowed(project, config);
 *     // ...
 * } catch (AppError e) {
 *     return new CallToolResult(List.of(new TextContent(e.toMcpJson())), true);
 * }
 * }</pre>
 *
 * @see ErrorCode
 * @see io.gentics.mesh.mcp.security.PolicyService
 * @since 0.1.0
 */
public class AppError extends RuntimeException {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ErrorCode code;
    private final Object    details;

    /**
     * Creates an {@code AppError} without additional detail data.
     *
     * @param code    machine-readable error classification
     * @param message human-readable description
     */
    public AppError(ErrorCode code, String message) {
        super(message);
        this.code    = code;
        this.details = null;
    }

    /**
     * Creates an {@code AppError} with structured detail data that will be
     * included in the MCP JSON response.
     *
     * @param code    machine-readable error classification
     * @param message human-readable description
     * @param details arbitrary detail object (must be Jackson-serialisable)
     */
    public AppError(ErrorCode code, String message, Object details) {
        super(message);
        this.code    = code;
        this.details = details;
    }

    /**
     * Creates an {@code AppError} that wraps an underlying cause.
     * The cause is included for logging but not serialised into the MCP response.
     *
     * @param code    machine-readable error classification
     * @param message human-readable description
     * @param cause   the underlying exception
     */
    public AppError(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code    = code;
        this.details = null;
    }

    /**
     * Returns the machine-readable error classification.
     *
     * @return the {@link ErrorCode}, never {@code null}
     */
    public ErrorCode getCode() { return code; }

    /**
     * Returns the optional detail object that was provided at construction time.
     *
     * @return detail object, or {@code null} if none was provided
     */
    public Object getDetails() { return details; }

    /**
     * Serialises this error to the canonical MCP tool-error JSON:
     * <pre>{@code {"error":{"code":"...","message":"...","details":...}}}</pre>
     *
     * <p>The {@code code} field is the lowercase enum name of {@link #getCode()}.
     * This method never throws – any serialisation failure falls back to a
     * safe hard-coded string.
     *
     * @return JSON string representation suitable for inclusion in a
     *         {@code CallToolResult} with {@code isError = true}
     */
    public String toMcpJson() {
        ObjectNode root  = MAPPER.createObjectNode();
        ObjectNode error = MAPPER.createObjectNode();
        error.put("code",    code.name().toLowerCase());
        error.put("message", getMessage());
        if (details != null) {
            error.putPOJO("details", details);
        } else {
            error.putNull("details");
        }
        root.set("error", error);
        try {
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"error\":{\"code\":\"internal_error\"," +
                   "\"message\":\"Serialisation failed\",\"details\":null}}";
        }
    }

    // ── Static factory methods ────────────────────────────────────────────

    /**
     * Convenience factory for {@link ErrorCode#FORBIDDEN}.
     *
     * @param msg human-readable reason
     * @return new {@code AppError}
     */
    public static AppError forbidden(String msg) {
        return new AppError(ErrorCode.FORBIDDEN, msg);
    }

    /**
     * Convenience factory for {@link ErrorCode#NOT_FOUND}.
     *
     * @param msg human-readable description of what was not found
     * @return new {@code AppError}
     */
    public static AppError notFound(String msg) {
        return new AppError(ErrorCode.NOT_FOUND, msg);
    }

    /**
     * Convenience factory for {@link ErrorCode#TIMEOUT_ERROR}.
     *
     * @param msg human-readable timeout description
     * @return new {@code AppError}
     */
    public static AppError timeout(String msg) {
        return new AppError(ErrorCode.TIMEOUT_ERROR, msg);
    }

    /**
     * Convenience factory for {@link ErrorCode#UPSTREAM_ERROR}.
     *
     * @param status HTTP status code returned by Mesh
     * @param body   response body (may be truncated in the message)
     * @return new {@code AppError}
     */
    public static AppError upstream(int status, String body) {
        return new AppError(ErrorCode.UPSTREAM_ERROR,
                "Mesh-API Fehler (HTTP " + status + "): " + body);
    }

    /**
     * Maps an HTTP status code to the appropriate {@code AppError} type.
     *
     * <ul>
     *   <li>404 → {@link ErrorCode#NOT_FOUND}</li>
     *   <li>401, 403 → {@link ErrorCode#FORBIDDEN}</li>
     *   <li>anything else → {@link ErrorCode#UPSTREAM_ERROR}</li>
     * </ul>
     *
     * @param status HTTP status code
     * @param body   response body for the error message
     * @return mapped {@code AppError}
     */
    public static AppError fromHttpStatus(int status, String body) {
        return switch (status) {
            case 404       -> notFound("Nicht gefunden: " + body);
            case 401, 403  -> forbidden("Zugriff verweigert: " + body);
            default        -> upstream(status, body);
        };
    }
}
