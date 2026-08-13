package io.gentics.mesh.mcp.config;

import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;

import java.net.URI;
import java.util.ArrayList;
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
 *   <tr><td>MESH_API_KEY</td><td>stdio only</td><td>–</td><td>Mesh API key (long-lived JWT); alternative to username/password. Rejected in HTTP mode</td></tr>
 *   <tr><td>MESH_USERNAME</td><td>stdio only</td><td>–</td><td>Service-account username; required with MESH_PASSWORD unless MESH_API_KEY is set. Rejected in HTTP mode</td></tr>
 *   <tr><td>MESH_PASSWORD</td><td>stdio only</td><td>–</td><td>Service-account password (never logged). Rejected in HTTP mode</td></tr>
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
 *   <tr><td>MCP_RATE_LIMIT_PER_SECOND</td><td>no</td><td>50</td><td>Global token-bucket refill (weighted permits/s)</td></tr>
 *   <tr><td>MCP_RATE_LIMIT_PER_TOKEN_PER_SECOND</td><td>no</td><td>20</td><td>Per-Bearer-token refill (weighted permits/s)</td></tr>
 *   <tr><td>MCP_MESH_MAX_CONCURRENT</td><td>no</td><td>10</td><td>Bulkhead permits — max simultaneous in-flight Mesh calls</td></tr>
 *   <tr><td>MCP_CIRCUIT_BREAKER_ENABLED</td><td>no</td><td>true</td><td>When {@code false}, never opens the breaker (rate-limit / bulkhead still apply)</td></tr>
 *   <tr><td>MCP_CIRCUIT_BREAKER_FAILURE_RATE_PERCENT</td><td>no</td><td>50</td><td>Open breaker when failure rate over window ≥ this %</td></tr>
 *   <tr><td>MCP_CIRCUIT_BREAKER_WINDOW_SIZE</td><td>no</td><td>20</td><td>Sliding-window size (number of calls) for failure-rate calc</td></tr>
 *   <tr><td>MCP_CIRCUIT_BREAKER_OPEN_DURATION_MS</td><td>no</td><td>30000</td><td>How long the breaker stays open before half-open probe</td></tr>
 *   <tr><td>MCP_GRAPHQL_MAX_QUERY_LENGTH</td><td>no</td><td>4000</td><td>Reject GraphQL queries longer than this many characters</td></tr>
 * </table>
 *
 * @param meshBaseUrl           Base URL of the Gentics Mesh instance (required)
 * @param meshApiKey            Mesh API key (long-lived JWT) for the stdio transport;
 *                              always {@code null} in HTTP mode
 * @param meshUsername          Service-account username for the stdio transport;
 *                              always {@code null} in HTTP mode
 * @param meshPassword          Service-account password – never logged; stdio only,
 *                              always {@code null} in HTTP mode
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
 * @param mcpRateLimitPerSecond            Global weighted permits/s (token bucket)
 * @param mcpRateLimitPerTokenPerSecond    Per-Bearer-token weighted permits/s
 * @param mcpMeshMaxConcurrent             Bulkhead permits — max simultaneous Mesh calls
 * @param mcpCircuitBreakerEnabled         Whether the circuit breaker is active
 * @param mcpCircuitBreakerFailureRatePct  Failure rate (%) at which the breaker opens
 * @param mcpCircuitBreakerWindowSize      Sliding-window size for failure-rate calc
 * @param mcpCircuitBreakerOpenDurationMs  How long the breaker stays open
 * @param mcpGraphqlMaxQueryLength         Max characters allowed in a GraphQL query body
 *
 * @see io.gentics.mesh.mcp.security.PolicyService
 * @since 0.1.0
 */
public record MeshConfig(
        String meshBaseUrl,
        String meshApiKey,
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
        List<String> mcpCorsOrigins,
        int mcpRateLimitPerSecond,
        int mcpRateLimitPerTokenPerSecond,
        int mcpMeshMaxConcurrent,
        boolean mcpCircuitBreakerEnabled,
        int mcpCircuitBreakerFailureRatePct,
        int mcpCircuitBreakerWindowSize,
        long mcpCircuitBreakerOpenDurationMs,
        int mcpGraphqlMaxQueryLength
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
    private static final int          DEFAULT_RATE_LIMIT_GLOBAL  = 50;
    private static final int          DEFAULT_RATE_LIMIT_TOKEN   = 20;
    private static final int          DEFAULT_MAX_CONCURRENT     = 10;
    private static final boolean      DEFAULT_BREAKER_ENABLED    = true;
    private static final int          DEFAULT_BREAKER_FAIL_PCT   = 50;
    private static final int          DEFAULT_BREAKER_WINDOW     = 20;
    private static final long         DEFAULT_BREAKER_OPEN_MS    = 30_000L;
    private static final int          DEFAULT_GRAPHQL_MAX_LENGTH = 4_000;

    /**
     * Load and validate configuration from environment variables.
     *
     * <p>Collects all validation errors before throwing so that operators
     * can fix every problem in one deployment cycle.
     *
     * @param transportOverride transport selected by a CLI flag ({@code "http"} /
     *                          {@code "stdio"}), or {@code null} to use
     *                          {@code MCP_TRANSPORT}. Resolved before validation
     *                          because the transport decides which Mesh
     *                          credentials are legal.
     * @return a fully validated, immutable {@link MeshConfig}
     * @throws AppError with code {@link ErrorCode#VALIDATION_ERROR} if any
     *                  required variable is missing or any value is out of range
     */
    public static MeshConfig load(String transportOverride) {
        var errors = new StringBuilder();

        String baseUrl  = requireEnv("MESH_BASE_URL",         errors);
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

        String transport;
        if (transportOverride != null && !transportOverride.isBlank()) {
            transport = transportOverride.strip().toLowerCase();      // --http / --stdio
        } else if (transportEnv != null && !transportEnv.isBlank()) {
            transport = transportEnv.strip().toLowerCase();
        } else {
            transport = DEFAULT_TRANSPORT;
        }
        if (!VALID_TRANSPORTS.contains(transport)) {
            errors.append("MCP_TRANSPORT muss 'stdio' oder 'http' sein. ");
        }

        // ── Mesh authentication ────────────────────────────────────────────
        // stdio: the server holds one identity for all calls, so it needs
        //        credentials – an API key or username+password.
        // http:  the Mesh identity comes from the request (or is anonymous),
        //        so server-side credentials would be silently ineffective.
        //        Configuring them is an error rather than a no-op.
        String apiKey   = optionalEnv("MESH_API_KEY");
        String username = optionalEnv("MESH_USERNAME");
        String password = optionalEnv("MESH_PASSWORD");
        if ("stdio".equals(transport)) {
            if (apiKey == null && (username == null || password == null)) {
                errors.append("Mesh-Authentifizierung fehlt: im stdio-Modus entweder "
                        + "MESH_API_KEY setzen oder MESH_USERNAME und MESH_PASSWORD. ");
            }
        } else {
            var configured = new ArrayList<String>();
            if (apiKey   != null) configured.add("MESH_API_KEY");
            if (username != null) configured.add("MESH_USERNAME");
            if (password != null) configured.add("MESH_PASSWORD");
            if (!configured.isEmpty()) {
                errors.append(String.join(", ", configured))
                      .append(" ist im HTTP-Modus nicht erlaubt: Mesh-Requests laufen "
                            + "anonym bzw. mit dem Bearer-Token des Clients. Variablen entfernen. ");
            }
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

        // ── Protection layer configuration ─────────────────────────────────
        int rateLimitGlobal = parsePositiveInt("MCP_RATE_LIMIT_PER_SECOND",
                System.getenv("MCP_RATE_LIMIT_PER_SECOND"), DEFAULT_RATE_LIMIT_GLOBAL, errors);
        int rateLimitToken  = parsePositiveInt("MCP_RATE_LIMIT_PER_TOKEN_PER_SECOND",
                System.getenv("MCP_RATE_LIMIT_PER_TOKEN_PER_SECOND"), DEFAULT_RATE_LIMIT_TOKEN, errors);
        int maxConcurrent   = parsePositiveInt("MCP_MESH_MAX_CONCURRENT",
                System.getenv("MCP_MESH_MAX_CONCURRENT"), DEFAULT_MAX_CONCURRENT, errors);

        String breakerEnabledEnv = System.getenv("MCP_CIRCUIT_BREAKER_ENABLED");
        boolean breakerEnabled = breakerEnabledEnv == null || !"false".equalsIgnoreCase(breakerEnabledEnv);

        int breakerFailPct = parseRangedInt("MCP_CIRCUIT_BREAKER_FAILURE_RATE_PERCENT",
                System.getenv("MCP_CIRCUIT_BREAKER_FAILURE_RATE_PERCENT"),
                DEFAULT_BREAKER_FAIL_PCT, 1, 100, errors);
        int breakerWindow = parsePositiveInt("MCP_CIRCUIT_BREAKER_WINDOW_SIZE",
                System.getenv("MCP_CIRCUIT_BREAKER_WINDOW_SIZE"), DEFAULT_BREAKER_WINDOW, errors);
        long breakerOpenMs = parsePositiveLong("MCP_CIRCUIT_BREAKER_OPEN_DURATION_MS",
                System.getenv("MCP_CIRCUIT_BREAKER_OPEN_DURATION_MS"), DEFAULT_BREAKER_OPEN_MS, errors);
        int graphqlMaxLength = parsePositiveInt("MCP_GRAPHQL_MAX_QUERY_LENGTH",
                System.getenv("MCP_GRAPHQL_MAX_QUERY_LENGTH"), DEFAULT_GRAPHQL_MAX_LENGTH, errors);

        if (!errors.isEmpty()) {
            throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "Konfigurationsfehler: " + errors.toString().trim());
        }

        return new MeshConfig(
                baseUrl,
                apiKey,
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
                corsOrigins,
                rateLimitGlobal,
                rateLimitToken,
                maxConcurrent,
                breakerEnabled,
                breakerFailPct,
                breakerWindow,
                breakerOpenMs,
                graphqlMaxLength
        );
    }

    private static int parsePositiveInt(String key, String raw, int def, StringBuilder errors) {
        if (raw == null || raw.isBlank()) return def;
        try {
            int v = Integer.parseInt(raw.strip());
            if (v < 1) { errors.append(key).append(" muss >= 1 sein. "); return def; }
            return v;
        } catch (NumberFormatException e) {
            errors.append(key).append(" muss eine Ganzzahl sein. ");
            return def;
        }
    }

    private static long parsePositiveLong(String key, String raw, long def, StringBuilder errors) {
        if (raw == null || raw.isBlank()) return def;
        try {
            long v = Long.parseLong(raw.strip());
            if (v < 1) { errors.append(key).append(" muss >= 1 sein. "); return def; }
            return v;
        } catch (NumberFormatException e) {
            errors.append(key).append(" muss eine Ganzzahl sein. ");
            return def;
        }
    }

    private static int parseRangedInt(String key, String raw, int def, int min, int max, StringBuilder errors) {
        if (raw == null || raw.isBlank()) return def;
        try {
            int v = Integer.parseInt(raw.strip());
            if (v < min || v > max) {
                errors.append(key).append(" muss zwischen ").append(min)
                        .append(" und ").append(max).append(" liegen. ");
                return def;
            }
            return v;
        } catch (NumberFormatException e) {
            errors.append(key).append(" muss eine Ganzzahl sein. ");
            return def;
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns the trimmed value of the named environment variable, or
     * {@code null} after appending an error message to {@code errors}.
     */
    /**
     * Returns the trimmed value of the named environment variable, or
     * {@code null} when it is unset or blank. No error is recorded.
     */
    private static String optionalEnv(String key) {
        String val = System.getenv(key);
        return (val == null || val.isBlank()) ? null : val.strip();
    }

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
