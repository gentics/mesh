package io.gentics.mesh.mcp.security;

import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;

/**
 * Centralised security policy checks for the Gentics Mesh MCP Server.
 *
 * <p>All policy decisions are made <strong>before</strong> any upstream HTTP
 * call is initiated. This "fail-fast" approach follows the
 * <em>Defence in Depth</em> principle: if a request is not allowed, Mesh
 * never sees it.
 *
 * <h2>Design</h2>
 * Every public method in this class either returns {@code void} (success) or
 * throws an {@link AppError} with code {@link io.gentics.mesh.mcp.domain.ErrorCode#FORBIDDEN}.
 * Callers do not need to inspect return values.
 *
 * <h2>Call order in use-cases</h2>
 * <pre>{@code
 * // Recommended ordering in every use-case:
 * PolicyService.assertProjectAllowed(project, config);   // 1st – most common gate
 * PolicyService.assertSchemaAllowed(schema, config);     // 2nd – only if schema known
 * PolicyService.assertLanguageAllowed(language, config); // 3rd
 * PolicyService.assertWriteAllowed(config);              // only for write operations
 * }</pre>
 *
 * @see MeshConfig#meshAllowedProjects()
 * @see MeshConfig#meshAllowedSchemas()
 * @see MeshConfig#meshAllowedLanguages()
 * @see MeshConfig#meshReadOnly()
 * @since 0.1.0
 */
public final class PolicyService {

    /** Utility class – do not instantiate. */
    private PolicyService() {}

    /**
     * Asserts that the given project name is present in
     * {@link MeshConfig#meshAllowedProjects()}.
     *
     * <p>This is the primary security gate called by every use-case that
     * communicates with Mesh. Projects that are not in the allowlist will
     * never be exposed, even if they exist in Mesh.
     *
     * @param project the project name from the tool input
     * @param config  the current server configuration
     * @throws AppError(FORBIDDEN) if the project is not allowlisted
     */
    public static void assertProjectAllowed(String project, MeshConfig config) {
        if (!config.meshAllowedProjects().contains(project)) {
            throw AppError.forbidden(
                    "Projekt '" + project + "' ist nicht in der Allowlist. " +
                    "Erlaubt: " + config.meshAllowedProjects());
        }
    }

    /**
     * Asserts that the given schema name is present in
     * {@link MeshConfig#meshAllowedSchemas()}.
     *
     * <p>This check is <strong>skipped</strong> when the schema allowlist is
     * empty (i.e. when {@code MESH_ALLOWED_SCHEMAS} is not set), which means
     * all schemas in the project are accessible.
     *
     * @param schema the schema name to check
     * @param config the current server configuration
     * @throws AppError(FORBIDDEN) if a schema allowlist is configured and
     *                             the given schema is not in it
     */
    public static void assertSchemaAllowed(String schema, MeshConfig config) {
        var allowed = config.meshAllowedSchemas();
        if (!allowed.isEmpty() && !allowed.contains(schema)) {
            throw AppError.forbidden(
                    "Schema '" + schema + "' ist nicht in der Allowlist. " +
                    "Erlaubt: " + allowed);
        }
    }

    /**
     * Asserts that the given ISO 639-1 language code is present in
     * {@link MeshConfig#meshAllowedLanguages()}.
     *
     * @param language the language code from the tool input (e.g. {@code "de"})
     * @param config   the current server configuration
     * @throws AppError(FORBIDDEN) if the language is not allowlisted
     */
    public static void assertLanguageAllowed(String language, MeshConfig config) {
        if (!config.meshAllowedLanguages().contains(language)) {
            throw AppError.forbidden(
                    "Sprache '" + language + "' ist nicht in der Allowlist. " +
                    "Erlaubt: " + config.meshAllowedLanguages());
        }
    }

    /**
     * Asserts that write operations are permitted by the current configuration.
     *
     * <p>The server defaults to read-only mode ({@code MESH_READ_ONLY=true}).
     * Write operations (Phase 2 tools) must set {@code MESH_READ_ONLY=false}
     * and call this guard at the very beginning of their use-case.
     *
     * @param config the current server configuration
     * @throws AppError(FORBIDDEN) if {@link MeshConfig#meshReadOnly()} is {@code true}
     */
    public static void assertWriteAllowed(MeshConfig config) {
        if (config.meshReadOnly()) {
            throw AppError.forbidden(
                    "Schreibzugriff ist deaktiviert (MESH_READ_ONLY=true). " +
                    "Setze MESH_READ_ONLY=false um Write-Tools zu aktivieren.");
        }
    }
}
