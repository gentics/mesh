package io.gentics.mesh.mcp.security;

import io.gentics.mesh.mcp.TestFixtures;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PolicyService}.
 *
 * <p>Every {@code assertXxx()} method either completes normally (allowed) or
 * throws {@link AppError} with {@link ErrorCode#FORBIDDEN} (denied).
 * These tests verify both paths for all four guards.
 */
class PolicyServiceTest {

    // ── assertProjectAllowed ──────────────────────────────────────────────

    @Test
    void assertProjectAllowed_allowedProject_noException() {
        MeshConfig config = TestFixtures.readOnlyConfig(); // allows "demo"
        assertDoesNotThrow(() -> PolicyService.assertProjectAllowed("demo", config));
    }

    @Test
    void assertProjectAllowed_unknownProject_throwsForbidden() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        AppError err = assertThrows(AppError.class,
                () -> PolicyService.assertProjectAllowed("secret-project", config));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "An unknown project must yield FORBIDDEN");
        assertTrue(err.getMessage().contains("secret-project"),
                "Error message must mention the rejected project name");
    }

    @Test
    void assertProjectAllowed_multipleProjects_bothAllowed() {
        MeshConfig config = new MeshConfig(
                "http://localhost:8080", "u", "p",
                List.of("alpha", "beta"),
                List.of(), List.of("de"), true, 6, 5000L, 10000L, "info",
                "stdio", "0.0.0.0", 3001, null, true, List.of());
        assertDoesNotThrow(() -> PolicyService.assertProjectAllowed("alpha", config));
        assertDoesNotThrow(() -> PolicyService.assertProjectAllowed("beta", config));
    }

    @Test
    void assertProjectAllowed_caseSensitiveMatch() {
        MeshConfig config = TestFixtures.readOnlyConfig(); // allows "demo"
        AppError err = assertThrows(AppError.class,
                () -> PolicyService.assertProjectAllowed("Demo", config));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "Project check must be case-sensitive ('Demo' != 'demo')");
    }

    // ── assertSchemaAllowed ───────────────────────────────────────────────

    @Test
    void assertSchemaAllowed_emptyAllowlist_permitsAll() {
        MeshConfig config = TestFixtures.readOnlyConfig(); // empty schema allowlist
        // Any schema should pass when the allowlist is empty
        assertDoesNotThrow(() -> PolicyService.assertSchemaAllowed("anything", config));
        assertDoesNotThrow(() -> PolicyService.assertSchemaAllowed("article",  config));
    }

    @Test
    void assertSchemaAllowed_allowlistedSchema_noException() {
        MeshConfig config = TestFixtures.configWithSchemaAllowlist("article", "blog");
        assertDoesNotThrow(() -> PolicyService.assertSchemaAllowed("article", config));
        assertDoesNotThrow(() -> PolicyService.assertSchemaAllowed("blog",    config));
    }

    @Test
    void assertSchemaAllowed_notAllowlistedSchema_throwsForbidden() {
        MeshConfig config = TestFixtures.configWithSchemaAllowlist("article");
        AppError err = assertThrows(AppError.class,
                () -> PolicyService.assertSchemaAllowed("admin-only-schema", config));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted schema must yield FORBIDDEN");
        assertTrue(err.getMessage().contains("admin-only-schema"),
                "Error message must mention the rejected schema name");
    }

    // ── assertLanguageAllowed ─────────────────────────────────────────────

    @Test
    void assertLanguageAllowed_allowedLanguage_noException() {
        MeshConfig config = TestFixtures.readOnlyConfig(); // allows "de", "en"
        assertDoesNotThrow(() -> PolicyService.assertLanguageAllowed("de", config));
        assertDoesNotThrow(() -> PolicyService.assertLanguageAllowed("en", config));
    }

    @Test
    void assertLanguageAllowed_unknownLanguage_throwsForbidden() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        AppError err = assertThrows(AppError.class,
                () -> PolicyService.assertLanguageAllowed("fr", config));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "A non-allowlisted language must yield FORBIDDEN");
        assertTrue(err.getMessage().contains("fr"),
                "Error message must mention the rejected language code");
    }

    @Test
    void assertLanguageAllowed_caseSensitiveMatch() {
        MeshConfig config = TestFixtures.readOnlyConfig(); // "de" is lowercase
        AppError err = assertThrows(AppError.class,
                () -> PolicyService.assertLanguageAllowed("DE", config));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "Language check must be case-sensitive ('DE' != 'de')");
    }

    // ── assertWriteAllowed ────────────────────────────────────────────────

    @Test
    void assertWriteAllowed_readOnlyFalse_noException() {
        MeshConfig config = TestFixtures.writeEnabledConfig();
        assertDoesNotThrow(() -> PolicyService.assertWriteAllowed(config));
    }

    @Test
    void assertWriteAllowed_readOnlyTrue_throwsForbidden() {
        MeshConfig config = TestFixtures.readOnlyConfig();
        AppError err = assertThrows(AppError.class,
                () -> PolicyService.assertWriteAllowed(config));
        assertEquals(ErrorCode.FORBIDDEN, err.getCode(),
                "Write access in read-only mode must yield FORBIDDEN");
        assertTrue(err.getMessage().contains("MESH_READ_ONLY"),
                "Error message must reference the MESH_READ_ONLY config key");
    }
}
