package io.gentics.mesh.mcp.server;

/**
 * Validates an incoming HTTP {@code Authorization} header.
 *
 * <p>This interface is the extension point for future authentication modes
 * (OAuth 2.1 resource-server validation, JWT introspection, mTLS, …).
 * Version 0.2.0 ships with a single implementation, {@link BearerTokenValidator}.
 *
 * @since 0.2.0
 */
@FunctionalInterface
public interface AuthValidator {

    /**
     * Returns {@code true} if the request bearing the given {@code Authorization}
     * header value is permitted to invoke MCP methods.
     *
     * @param authorizationHeader value of the {@code Authorization} request header;
     *                            may be {@code null} if the client sent no header
     * @return {@code true} when the request is authenticated and may proceed
     */
    boolean isAuthorized(String authorizationHeader);

    /** Permissive validator that accepts every request – use only when an upstream proxy enforces auth. */
    AuthValidator ALLOW_ALL = header -> true;
}
