package io.gentics.mesh.mcp;

import io.gentics.mesh.mcp.config.MeshConfig;

import java.util.List;

/**
 * Shared test fixtures and factory methods for the MCP server unit tests.
 *
 * <p>All test classes should obtain {@link MeshConfig} instances from this
 * class to keep setup consistent and changes centralised.
 */
public final class TestFixtures {

    private TestFixtures() {}

    /** Creates a standard read-only config with the {@code demo} project allowed. */
    public static MeshConfig readOnlyConfig() {
        return baseConfig(true, 6, List.of());
    }

    /** Like {@link #readOnlyConfig()} but with write operations permitted. */
    public static MeshConfig writeEnabledConfig() {
        return baseConfig(false, 6, List.of());
    }

    /** Config with a specific schema allowlist. */
    public static MeshConfig configWithSchemaAllowlist(String... schemas) {
        return baseConfig(true, 6, List.of(schemas));
    }

    /** Config with a shallow GraphQL depth limit (for testing depth guard). */
    public static MeshConfig shallowDepthConfig(int maxDepth) {
        return baseConfig(true, maxDepth, List.of());
    }

    private static MeshConfig baseConfig(boolean readOnly, int graphqlMaxDepth,
                                         List<String> schemaAllowlist) {
        return new MeshConfig(
                "http://localhost:8080",
                "admin",
                "admin",
                List.of("demo"),
                schemaAllowlist,
                List.of("de", "en"),
                readOnly,
                graphqlMaxDepth,
                5_000L,
                10_000L,
                "info",
                "stdio",
                "0.0.0.0",
                3001,
                null,
                true,
                List.of(),
                // ── Protection layer (test defaults: liberal so unrelated tests don't hit limits)
                10_000,    // mcpRateLimitPerSecond
                10_000,    // mcpRateLimitPerTokenPerSecond
                1_000,     // mcpMeshMaxConcurrent
                false,     // mcpCircuitBreakerEnabled – off by default in tests
                50,        // mcpCircuitBreakerFailureRatePct
                20,        // mcpCircuitBreakerWindowSize
                30_000L,   // mcpCircuitBreakerOpenDurationMs
                4_000      // mcpGraphqlMaxQueryLength
        );
    }
}
