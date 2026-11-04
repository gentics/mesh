package io.gentics.mesh.mcp.mesh;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * GraphQL client with built-in security guardrails for the Gentics Mesh API.
 *
 * <p>All checks in this class run <em>synchronously and without network access</em>
 * before the HTTP request is dispatched. This is a deliberate "fail-fast"
 * design that gives operators clear error messages without incurring any
 * Mesh round-trip cost.
 *
 * <h2>Guardrails</h2>
 * <table border="1" summary="GraphQL guardrails">
 *   <tr><th>Check</th><th>Mechanism</th><th>Error code</th></tr>
 *   <tr>
 *     <td>Mutation detection</td>
 *     <td>Regex against the leading token of the query string</td>
 *     <td>{@link ErrorCode#FORBIDDEN}</td>
 *   </tr>
 *   <tr>
 *     <td>Depth limiting</td>
 *     <td>Brace-counting heuristic (see {@link #measureDepth(String)})</td>
 *     <td>{@link ErrorCode#VALIDATION_ERROR}</td>
 *   </tr>
 *   <tr>
 *     <td>Request timeout</td>
 *     <td>Vert.x HTTP client timeout via {@code MESH_GRAPHQL_TIMEOUT_MS}</td>
 *     <td>{@link ErrorCode#TIMEOUT_ERROR}</td>
 *   </tr>
 * </table>
 *
 * <h2>Transport</h2>
 * The actual HTTP call is delegated to the official {@link MeshRestClient},
 * which handles authentication headers and token refresh automatically.
 * The {@link GraphQLResponse} DTO is serialised back to a {@link JsonNode}
 * so that callers receive the raw Mesh GraphQL JSON envelope.
 *
 * <h2>Depth measurement</h2>
 * {@link #measureDepth(String)} counts the maximum brace-nesting level of the
 * query string. While this is a heuristic (it does not parse GraphQL AST), it
 * is fast, dependency-free and sufficient to prevent runaway queries in
 * practice. Configure the limit via {@code MESH_GRAPHQL_MAX_DEPTH}.
 *
 * <h2>Endpoint</h2>
 * {@code POST {MESH_BASE_URL}/api/v2/{project}/graphql}
 *
 * @see MeshRestClient
 * @see io.gentics.mesh.mcp.config.MeshConfig#meshGraphqlMaxDepth()
 * @see io.gentics.mesh.mcp.config.MeshConfig#meshGraphqlTimeoutMs()
 * @since 0.2.0
 */
public class MeshGraphqlClient {

    /**
     * Matches queries that start with the keyword {@code mutation} (case-insensitive,
     * with optional leading whitespace).
     */
    private static final Pattern MUTATION_PATTERN =
            Pattern.compile("^\\s*mutation\\b", Pattern.CASE_INSENSITIVE);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshConfig     config;
    private final MeshRestClient client;

    /**
     * @param config server configuration (provides depth limit and timeout)
     * @param client official Mesh REST client used to execute the query
     */
    public MeshGraphqlClient(MeshConfig config, MeshRestClient client) {
        this.config = config;
        this.client = client;
    }

    /**
     * Executes a GraphQL query against the given Gentics Mesh project.
     *
     * <p>Guardrails are checked in this order:
     * <ol>
     *   <li>Mutation detection – throws immediately without touching the network.</li>
     *   <li>Depth check – throws immediately without touching the network.</li>
     *   <li>HTTP POST to Mesh via {@link MeshRestClient#graphql(String, GraphQLRequest)}.</li>
     * </ol>
     *
     * @param project   project name (must be allowlisted by the caller)
     * @param gqlQuery  the GraphQL query string
     * @param variables optional variable map; may be {@code null} or empty
     * @param auth      per-call Mesh identity; {@link MeshAuth#NONE} for anonymous
     * @return the raw JSON response from Mesh (includes {@code data} and optionally {@code errors})
     * @throws AppError(FORBIDDEN)        if the query starts with {@code mutation}
     * @throws AppError(VALIDATION_ERROR) if {@link #measureDepth(String)} exceeds
     *                                    {@link MeshConfig#meshGraphqlMaxDepth()}
     * @throws AppError(TIMEOUT_ERROR)    if the request exceeds {@link MeshConfig#meshGraphqlTimeoutMs()}
     * @throws AppError(UPSTREAM_ERROR)   if Mesh returns a non-2xx status
     */
    public JsonNode query(String project, String gqlQuery, Map<String, Object> variables,
                          MeshAuth auth) {
        // 1. Mutation guard
        if (isMutation(gqlQuery)) {
            throw AppError.forbidden(
                    "GraphQL-Mutations sind nicht erlaubt (MESH_READ_ONLY-Guardrail). " +
                    "Nur query-Operationen werden akzeptiert.");
        }

        // 2. Length guard (catches pathologically long queries before depth analysis)
        int maxLen = config.mcpGraphqlMaxQueryLength();
        if (gqlQuery != null && gqlQuery.length() > maxLen) {
            throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "GraphQL-Query-Länge " + gqlQuery.length() + " überschreitet das " +
                    "konfigurierte Maximum von " + maxLen + " Zeichen " +
                    "(MCP_GRAPHQL_MAX_QUERY_LENGTH). Query kürzen oder Limit erhöhen.");
        }

        // 3. Depth guard
        int depth = measureDepth(gqlQuery);
        if (depth > config.meshGraphqlMaxDepth()) {
            throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "GraphQL-Query-Tiefe " + depth + " überschreitet das konfigurierte " +
                    "Maximum von " + config.meshGraphqlMaxDepth() + " " +
                    "(MESH_GRAPHQL_MAX_DEPTH). Query vereinfachen oder Limit erhöhen.");
        }

        // 3. Build request DTO
        // Note: GraphQLRequest.setVariables() expects io.vertx.core.json.JsonObject,
        // not a plain java.util.Map – convert here.
        GraphQLRequest request = new GraphQLRequest();
        request.setQuery(gqlQuery);
        if (variables != null && !variables.isEmpty()) {
            request.setVariables(new JsonObject(variables));
        }

        // 4. Execute via official client (blocking), carrying the caller's identity
        var meshRequest = client.graphql(project, request);
        if (auth != null && auth.hasToken()) {
            meshRequest.setHeader("Authorization", "Bearer " + auth.bearerToken());
        }
        GraphQLResponse response = meshRequest.blockingGet();

        // 5. Serialise DTO back to JsonNode so callers receive the raw envelope.
        //    Via Mesh's own JSON mapping: data is a Vert.x JsonObject, which plain
        //    Jackson would serialise as a bean ({"map":…,"empty":…}).
        try {
            return MAPPER.readTree(response.toJson(false));
        } catch (JsonProcessingException e) {
            throw new AppError(ErrorCode.INTERNAL_ERROR,
                    "GraphQL-Antwort konnte nicht gelesen werden: " + e.getMessage(), e);
        }
    }

    // ── Static helpers (package-visible for unit tests) ───────────────────

    /**
     * Returns {@code true} if the query string starts with the keyword
     * {@code mutation} (case-insensitive, ignoring leading whitespace).
     *
     * @param query raw GraphQL query string
     * @return {@code true} for mutation operations
     */
    static boolean isMutation(String query) {
        return MUTATION_PATTERN.matcher(query.strip()).find();
    }

    /**
     * Measures the maximum brace-nesting depth of a GraphQL query string by
     * counting {@code {}} characters.
     *
     * <p>This is a heuristic: it treats every {@code {…}} as one nesting level
     * without parsing the GraphQL AST. This is sufficient for guarding against
     * runaway recursive queries while being extremely fast and dependency-free.
     *
     * @param query raw GraphQL query string
     * @return maximum nesting depth (0 for an empty or brace-free string)
     */
    static int measureDepth(String query) {
        int max = 0, current = 0;
        for (char c : query.toCharArray()) {
            if      (c == '{') { current++; max = Math.max(max, current); }
            else if (c == '}') { current--; }
        }
        return max;
    }
}
