package io.gentics.mesh.mcp.server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Constant-time bearer-token validator.
 *
 * <p>Compares the token presented in {@code Authorization: Bearer <token>}
 * against a static value loaded from {@code MCP_AUTH_TOKEN}. Comparison uses
 * {@link MessageDigest#isEqual(byte[], byte[])} so a timing side-channel cannot
 * leak the configured token byte by byte.
 *
 * @since 0.2.0
 */
public final class BearerTokenValidator implements AuthValidator {

    private static final String PREFIX = "Bearer ";

    private final byte[] expectedTokenBytes;

    /**
     * @param token the expected bearer token (must not be {@code null} or blank)
     */
    public BearerTokenValidator(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Bearer token must not be empty");
        }
        this.expectedTokenBytes = token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isAuthorized(String authorizationHeader) {
        if (authorizationHeader == null) return false;
        if (!authorizationHeader.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) return false;
        String provided = authorizationHeader.substring(PREFIX.length()).strip();
        if (provided.isEmpty()) return false;
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedTokenBytes, providedBytes);
    }
}
