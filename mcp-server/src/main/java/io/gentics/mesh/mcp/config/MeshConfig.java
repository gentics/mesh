package io.gentics.mesh.mcp.config;

import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable, validated configuration for the Gentics Mesh MCP Server.
 *
 * <p>All values are read exclusively from <em>environment variables</em> via
 * {@link #load()}. Using a Java {@code record} guarantees immutability after
 * construction – no setter, no partial state.
 *
 * <h2>Validation contract</h2>
 * {@link #load()} collects <em>all</em> validation errors before throwing,
 * so operators see the complete list of problems in one startup run rather
 * than fixing them one by one.
 *
 * <h2>Environment variable reference</h2>
 * <table border="1" summary="Environment variables">
 *   <tr><th>Variable</th><th>Required</th><th>Default</th><th>Description</th></tr>
 *   <tr><td>MESH_BASE_URL</td><td>yes</td><td>–</td><td>Base URL of the Mesh instance</td></tr>
 *   <tr><td>MESH_USERNAME</td><td>yes</td><td>–</td><td>Service-account username</td></tr>
 *   <tr><td>MESH_PASSWORD</td><td>yes</td><td>–</td><td>Service-account password (never logged)</td></tr>
 *   <tr><td>MESH_ALLOWED_PROJECTS</td><td>yes</td><td>–</td><td>Comma-separated project allowlist</td></tr>
 *   <tr><td>MESH_ALLOWED_SCHEMAS</td><td>no</td><td>(all)</td><td>Comma-separated schema allowlist</td></tr>
 *   <tr><td>MESH_ALLOWED_LANGUAGES</td><td>no</td><td>de,en</td><td>Comma-separated language codes</td></tr>
 *   <tr><td>MESH_READ_ONLY</td><td>no</td><td>true</td><td>Disable write operations</td></tr>
 *   <tr><td>MESH_GRAPHQL_MAX_DEPTH</td><td>no</td><td>6</td><td>Max GraphQL brace-nesting depth (1–20)</td></tr>
 *   <tr><td>MESH_GRAPHQL_TIMEOUT_MS</td><td>no</td><td>5000</td><td>GraphQL request timeout in ms (≥100)</td></tr>
 *   <tr><td>LOG_LEVEL</td><td>no</td><td>info</td><td>debug | info | warn | error</td></tr>
 *   <tr><td>MCP_TRANSPORT</td><td>no</td><td>stdio</td><td>stdio | http – selects how MCP clients connect</td></tr>
 *   <tr><td>MCP_HTTP_HOST</td><td>no</td><td>0.0.0.0</td><td>Bind host for HTTP transport</td></tr>
 *   <tr><td>MCP_HTTP_PORT</td><td>no</td><td>3001</td><td>Bind port for HTTP transport (1–65535)</td></tr>
 *   <tr><td>MCP_AUTH_TOKEN</td><td>conditional</td><td>–</td><td>Required in HTTP mode unless explicitly disabled via MCP_AUTH_DISABLED=true</td></tr>
 *   <tr><td>MCP_AUTH_DISABLED</td><td>no</td><td>false</td><td>Set to {@code true} only when an external proxy/ingress handles authentication</td></tr>
 *   <tr><td>MCP_CORS_ORIGINS</td><td>no</td><td>(none)</td><td>Comma-separated allowlist of CORS origins; {@code *} permits all</td></tr>
 * </table>
 *
 * @param meshBaseUrl           Base URL of the Gentics Mesh instance (required)
 * @param meshUsername          Service-account username (required)
 * @param meshPassword          Service-account password – never logged (required)
 * @param meshAllowedProjects   Allowlisted project names (required, non-empty)
 * @param meshAllowedSchemas    Allowlisted schema names; empty list = all allowed
 * @param meshAllowedLanguages  Allowlisted ISO 639-1 language codes
 * @param meshReadOnly          When {@code true} (default), write operations throw FORBIDDEN
 * @param meshGraphqlMaxDepth   Maximum GraphQL query depth by brace counting
 * @param meshGraphqlTimeoutMs  Timeout for GraphQL requests in milliseconds
 * @param meshRestTimeoutMs     Timeout for REST requests in milliseconds
 * @param logLevel              Logback log level string
 * @param mcpTransport          MCP transport mode: {@code "stdio"} (default) or {@code "http"}
 * @param mcpHttpHost           Bind host for HTTP transport (default {@code 0.0.0.0})
 * @param mcpHttpPort           Bind port for HTTP transport (default {@code 3001})
 * @param mcpAuthToken          Bearer token for HTTP transport ({@code null} when auth disabled)
 * @param mcpAuthDisabled       When {@code true}, HTTP requests are accepted without authentication
 * @param mcpCorsOrigins        Allowed CORS origins; empty list disables CORS headers
 *
 * @see io.gentics.mesh.mcp.security.PolicyService
 * @since 0.1.0
 */
public record MeshConfig(
        String meshBaseUrl,
        String meshUsername,
        String meshPassword,
        List<String> meshAllowedProjects,
        List<String> meshAllowedSchemas,
        List<String> meshAllowedLanguages,
        boolean meshReadOnly,
        int meshGraphqlMaxDepth,
        long meshGraphqlTimeoutMs,
        long meshRestTimeoutMs,
        String logLevel,
        String mcpTransport,
        String mcpHttpHost,
        int mcpHttpPort,
        String mcpAuthToken,
        boolean mcpAuthDisabled,
        List<String> mcpCorsOrigins
) {

    // ── Defaults ────────────────────────────────────────────────────────────
    private static final List<String> DEFAULT_LANGUAGES     = List.of("de", "en");
    private static final int          DEFAULT_GRAPHQL_DEPTH   = 6;
    private static final long         DEFAULT_GRAPHQL_TIMEOUT = 5_000L;
    private static final long         DEFAULT_REST_TIMEOUT    = 10_000L;
    private static final Set<String>  VALID_LOG_LEVELS =
            Set.of("debug", "info", "warn", "error");
    private static final String       DEFAULT_TRANSPORT = "stdio";
    private static final Set<String>  VALID_TRANSPORTS = Set.of("stdio", "http");
    private static final String       DEFAULT_HTTP_HOST = "0.0.0.0";
    private static final int          DEFAULT_HTTP_PORT = 3001;

    /**
     * Load and validate configuration from environment variables.
     *
     * <p>Collects all validation errors before throwing so that operators
     * can fix every problem in one deployment cycle.
     *
     * @return a fully validated, immutable {@link MeshConfig}
     * @throws AppError with code {@link ErrorCode#VALIDATION_ERROR} if any
     *                  required variable is missing or any value is out of range
     */
    public static MeshConfig load() {
        var errors = new StringBuilder();

        String baseUrl  = requireEnv("MESH_BASE_URL",         errors);
        String username = requireEnv("MESH_USERNAME",         errors);
        String password = requireEnv("MESH_PASSWORD",         errors);
        String projects = requireEnv("MESH_ALLOWED_PROJECTS", errors);

        // Validate URL format
        if (baseUrl != null) {
            try { URI.create(baseUrl); }
            catch (IllegalArgumentException e) {
                errors.append("MESH_BASE_URL muss eine gültige URL sein. ");
            }
        }

        // Optional fields with defaults
        String schemasEnv    = System.getenv("MESH_ALLOWED_SCHEMAS");
        String languagesEnv  = System.getenv("MESH_ALLOWED_LANGUAGES");
        String readOnlyEnv   = System.getenv("MESH_READ_ONLY");
        String maxDepthEnv   = System.getenv("MESH_GRAPHQL_MAX_DEPTH");
        String gqlTimeoutEnv = System.getenv("MESH_GRAPHQL_TIMEOUT_MS");
        String logLevelEnv   = System.getenv("LOG_LEVEL");

        int graphqlMaxDepth = DEFAULT_GRAPHQL_DEPTH;
        if (maxDepthEnv != null) {
            try {
                graphqlMaxDepth = Integer.parseInt(maxDepthEnv);
                if (graphqlMaxDepth < 1 || graphqlMaxDepth > 20)
                    errors.append("MESH_GRAPHQL_MAX_DEPTH muss zwischen 1 und 20 liegen. ");
            } catch (NumberFormatException e) {
                errors.append("MESH_GRAPHQL_MAX_DEPTH muss eine Ganzzahl sein. ");
            }
        }

        long graphqlTimeout = DEFAULT_GRAPHQL_TIMEOUT;
        if (gqlTimeoutEnv != null) {
            try {
                graphqlTimeout = Long.parseLong(gqlTimeoutEnv);
                if (graphqlTimeout < 100)
                    errors.append("MESH_GRAPHQL_TIMEOUT_MS muss >= 100 sein. ");
            } catch (NumberFormatException e) {
                errors.append("MESH_GRAPHQL_TIMEOUT_MS muss eine Ganzzahl sein. ");
            }
        }

        String logLevel = Optional.ofNullable(logLevelEnv).orElse("info");
        if (!VALID_LOG_LEVELS.contains(logLevel))
            errors.append("LOG_LEVEL muss debug|info|warn|error sein. ");

        // ── MCP transport configuration ────────────────────────────────────
        String transportEnv  = System.getenv("MCP_TRANSPORT");
        String httpHostEnv   = System.getenv("MCP_HTTP_HOST");
        String httpPortEnv   = System.getenv("MCP_HTTP_PORT");
        String authTokenEnv  = System.getenv("MCP_AUTH_TOKEN");
        String authDisabled  = System.getenv("MCP_AUTH_DISABLED");
        String corsEnv       = System.getenv("MCP_CORS_ORIGINS");

        String transport = (transportEnv != null && !transportEnv.isBlank())
                ? transportEnv.strip().toLowerCase()
                : DEFAULT_TRANSPORT;
        if (!VALID_TRANSPORTS.contains(transport)) {
            errors.append("MCP_TRANSPORT muss 'stdio' oder 'http' sein. ");
        }

        int httpPort = DEFAULT_HTTP_PORT;
        if (httpPortEnv != null && !httpPortEnv.isBlank()) {
            try {
                httpPort = Integer.parseInt(httpPortEnv.strip());
                if (httpPort < 1 || httpPort > 65535) {
                    errors.append("MCP_HTTP_PORT muss zwischen 1 und 65535 liegen. ");
                }
            } catch (NumberFormatException e) {
                errors.append("MCP_HTTP_PORT muss eine Ganzzahl sein. ");
            }
        }

        String httpHost = (httpHostEnv != null && !httpHostEnv.isBlank())
                ? httpHostEnv.strip()
                : DEFAULT_HTTP_HOST;

        boolean disabled = "true".equalsIgnoreCase(authDisabled);
        String token = (authTokenEnv != null && !authTokenEnv.isBlank())
                ? authTokenEnv.strip()
                : null;

        // Auth is only required for HTTP transport. stdio implicitly trusts the spawning process.
        if ("http".equals(transport) && token == null && !disabled) {
            errors.append("MCP_AUTH_TOKEN ist im HTTP-Modus erforderlich " +
                    "(oder MCP_AUTH_DISABLED=true setzen, wenn ein Reverse-Proxy die Auth übernimmt). ");
        }

        List<String> corsOrigins = corsEnv != null && !corsEnv.isBlank()
                ? splitCsv(corsEnv)
                : List.of();

        if (!errors.isEmpty()) {
            throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "Konfigurationsfehler: " + errors.toString().trim());
        }

        return new MeshConfig(
                baseUrl,
                username,
                password,
                splitCsv(projects),
                schemasEnv != null ? splitCsv(schemasEnv) : List.of(),
                languagesEnv != null ? splitCsv(languagesEnv) : DEFAULT_LANGUAGES,
                !"false".equalsIgnoreCase(readOnlyEnv),
                graphqlMaxDepth,
                graphqlTimeout,
                DEFAULT_REST_TIMEOUT,
                logLevel,
                transport,
                httpHost,
                httpPort,
                token,
                disabled,
                corsOrigins
        );
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns the trimmed value of the named environment variable, or
     * {@code null} after appending an error message to {@code errors}.
     */
    private static String requireEnv(String key, StringBuilder errors) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            errors.append(key).append(" ist erforderlich. ");
            return null;
        }
        return val.strip();
    }

    /**
     * Splits a comma-separated string into a trimmed, non-empty list.
     *
     * @param csv comma-separated values, e.g. {@code "demo, shop "}
     * @return immutable list of trimmed tokens
     */
    private static List<String> splitCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
