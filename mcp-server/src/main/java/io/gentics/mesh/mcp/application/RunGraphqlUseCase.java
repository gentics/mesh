package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.security.PolicyService;

import java.util.Map;

/**
 * Application use-case: execute a read-only GraphQL query against a Gentics Mesh project.
 *
 * <p>This is the implementation behind the {@code mesh_run_graphql} MCP tool.
 * It enforces the project allowlist policy and then delegates to
 * {@link MeshClient#runGraphql(String, String, java.util.Map)}, which in turn
 * applies mutation blocking and depth limiting via
 * {@link io.gentics.mesh.mcp.mesh.MeshGraphqlClient}.
 *
 * <h2>Policy checks</h2>
 * <ul>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertProjectAllowed} –
 *       rejects unknown projects.</li>
 *   <li>Mutation guard in {@link io.gentics.mesh.mcp.mesh.MeshGraphqlClient} –
 *       rejects queries starting with {@code mutation}.</li>
 *   <li>Depth limit in {@link io.gentics.mesh.mcp.mesh.MeshGraphqlClient} –
 *       rejects deeply nested queries exceeding {@code MESH_GRAPHQL_MAX_DEPTH}.</li>
 * </ul>
 *
 * <h2>Output format</h2>
 * <pre>{@code
 * {
 *   "data": { … raw GraphQL data … },
 *   "meta": { "truncated": false, "timedOut": false }
 * }
 * }</pre>
 *
 * <p>The {@code meta} object is reserved for future pagination/truncation support.
 * Both flags are currently always {@code false}.
 *
 * @see MeshClient#runGraphql(String, String, java.util.Map)
 * @see io.gentics.mesh.mcp.mesh.MeshGraphqlClient
 * @since 0.1.0
 */
public class RunGraphqlUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshClient client;
    private final MeshConfig config;

    /**
     * @param client Mesh API facade
     * @param config server configuration used for policy checks
     */
    public RunGraphqlUseCase(MeshClient client, MeshConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Executes the use-case: validates the project, runs the GraphQL query, and
     * returns the response wrapped in a {@code data}/{@code meta} envelope.
     *
     * @param project   allowlisted project name
     * @param query     read-only GraphQL query string (mutations are rejected)
     * @param variables optional GraphQL variable bindings; may be empty but not {@code null}
     * @return JSON string with {@code data} and {@code meta} fields
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN)         if the project is not allowed or a mutation was attempted
     * @throws io.gentics.mesh.mcp.domain.AppError(VALIDATION_ERROR)  if the query exceeds the depth limit
     * @throws io.gentics.mesh.mcp.domain.AppError(TIMEOUT_ERROR)     if the GraphQL request times out
     * @throws io.gentics.mesh.mcp.domain.AppError(INTERNAL_ERROR) if serialisation fails
     */
    public String execute(String project, String query,
                          Map<String, Object> variables) {
        PolicyService.assertProjectAllowed(project, config);

        var raw = client.runGraphql(project, query, variables);

        ObjectNode result = MAPPER.createObjectNode();
        result.set("data", raw.path("data"));

        ObjectNode meta = MAPPER.createObjectNode();
        meta.put("truncated", false);
        meta.put("timedOut",  false);
        result.set("meta", meta);

        return ToolResult.json(result);
    }
}
