package io.gentics.mesh.mcp.mesh;

/**
 * The Mesh identity to use for a single tool call.
 *
 * <p>Which identity applies is decided per transport:
 * <ul>
 *   <li><strong>stdio</strong> – {@link #NONE}. The shared {@link MeshClient} is
 *       already authenticated from configuration ({@code MESH_API_KEY} or
 *       {@code MESH_USERNAME}/{@code MESH_PASSWORD}), so individual calls add
 *       nothing.</li>
 *   <li><strong>HTTP with MCP auth</strong> – {@link #NONE}. The bearer token in
 *       the request belongs to the MCP server, not to Mesh; forwarding it would
 *       leak an unrelated credential. Mesh serves these calls anonymously.</li>
 *   <li><strong>HTTP without MCP auth</strong> – {@link #bearer(String)} when the
 *       caller supplied an {@code Authorization} header. That token is a Mesh
 *       token: Mesh decides what the caller may see.</li>
 * </ul>
 *
 * <p>The token is applied per request via {@code MeshRequest.setHeader(...)} and
 * is never stored on the shared client, so concurrent calls cannot inherit each
 * other's identity.
 *
 * @param bearerToken raw Mesh bearer token, or {@code null} for anonymous access
 * @since 0.3.0
 */
public record MeshAuth(String bearerToken) {

    /** No per-request identity: anonymous, or already authenticated at client level. */
    public static final MeshAuth NONE = new MeshAuth(null);

    /**
     * @param token raw bearer token; blank or {@code null} yields {@link #NONE}
     */
    public static MeshAuth bearer(String token) {
        return (token == null || token.isBlank()) ? NONE : new MeshAuth(token.strip());
    }

    /** @return {@code true} if a token should be attached to outgoing requests */
    public boolean hasToken() {
        return bearerToken != null;
    }
}
