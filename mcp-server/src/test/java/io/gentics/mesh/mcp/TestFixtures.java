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
        return baseConfig(true, 6);
    }

    /** Like {@link #readOnlyConfig()} but with write operations permitted. */
    public static MeshConfig writeEnabledConfig() {
        return baseConfig(false, 6);
    }

    /** Config with a specific schema allowlist. */
    public static MeshConfig configWithSchemaAllowlist(String... schemas) {
        return new MeshConfig(
                "http://localhost:8080",
                "admin",
                "admin",
                List.of("demo"),
                List.of(schemas),
                List.of("de", "en"),
                true,
                6,
                5_000L,
                10_000L,
                "info",
                "stdio",
                "0.0.0.0",
                3001,
                null,
                true,
                List.of()
        );
    }

    /** Config with a shallow GraphQL depth limit (for testing depth guard). */
    public static MeshConfig shallowDepthConfig(int maxDepth) {
        return baseConfig(true, maxDepth);
    }

    private static MeshConfig baseConfig(boolean readOnly, int graphqlMaxDepth) {
        return new MeshConfig(
                "http://localhost:8080",
                "admin",
                "admin",
                List.of("demo"),
                List.of(),
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
                List.of()
        );
    }
}
