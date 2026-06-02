package io.gentics.mesh.mcp.domain;

/**
 * Stable, machine-readable error codes used throughout the MCP server.
 *
 * <p>Every {@link AppError} carries exactly one of these codes. LLM clients
 * and monitoring systems can rely on these codes being stable across releases –
 * the human-readable {@link AppError#getMessage()} may change, but the code
 * will not.
 *
 * <p>Codes are intentionally stable across releases so that monitoring
 * dashboards, automated alerting, and LLM-side error handling can rely on
 * them. New codes are added at the bottom; existing ones are never renumbered.
 *
 * <table border="1" summary="Error codes and their HTTP equivalents">
 *   <tr><th>Code</th><th>HTTP equivalent</th><th>When thrown</th></tr>
 *   <tr>
 *     <td>{@link #VALIDATION_ERROR}</td><td>400</td>
 *     <td>Invalid input or startup mis-configuration</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #FORBIDDEN}</td><td>403</td>
 *     <td>Allowlist violation, read-only breach, or mutation attempt</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #NOT_FOUND}</td><td>404</td>
 *     <td>Node or path does not exist in Mesh</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #UPSTREAM_ERROR}</td><td>502</td>
 *     <td>Unexpected HTTP error from the Mesh API</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #TIMEOUT_ERROR}</td><td>504</td>
 *     <td>Request exceeded the configured timeout</td>
 *   </tr>
 *   <tr>
 *     <td>{@link #INTERNAL_ERROR}</td><td>500</td>
 *     <td>Unexpected exception (bug)</td>
 *   </tr>
 * </table>
 *
 * @see AppError
 * @since 0.1.0
 */
public enum ErrorCode {

    /**
     * Thrown when tool input fails validation or the startup configuration
     * contains invalid values (e.g. {@code MESH_GRAPHQL_MAX_DEPTH=99}).
     */
    VALIDATION_ERROR,

    /**
     * Thrown when a request violates a security policy:
     * <ul>
     *   <li>Project / schema / language not in allowlist</li>
     *   <li>Write operation attempted while {@code MESH_READ_ONLY=true}</li>
     *   <li>GraphQL mutation detected</li>
     * </ul>
     */
    FORBIDDEN,

    /**
     * Thrown when the requested node UUID or webroot path does not exist in
     * Gentics Mesh (HTTP 404 upstream).
     */
    NOT_FOUND,

    /**
     * Thrown when Gentics Mesh returns an unexpected HTTP error status
     * (any 4xx except 401/404, or any 5xx).
     */
    UPSTREAM_ERROR,

    /**
     * Thrown when a request exceeds the configured timeout and is aborted
     * by the {@code HttpRequest} timeout mechanism.
     */
    TIMEOUT_ERROR,

    /**
     * Thrown for unexpected exceptions that indicate a bug in the server.
     * Always logged with a full stack trace.
     */
    INTERNAL_ERROR
}
