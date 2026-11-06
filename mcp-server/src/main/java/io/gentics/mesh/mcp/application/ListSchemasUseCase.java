package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.security.PolicyService;

/**
 * Application use-case: list all schemas of a Gentics Mesh project.
 *
 * <p>This is the implementation behind the {@code mesh_list_schemas} MCP tool.
 * It enforces the project allowlist policy, fetches schema metadata from
 * {@link MeshClient#listSchemas(String)}, and serialises the result to JSON.
 *
 * <h2>Policy checks</h2>
 * {@link io.gentics.mesh.mcp.security.PolicyService#assertProjectAllowed(String,
 * io.gentics.mesh.mcp.config.MeshConfig)} is called before any network request.
 * Requests for projects outside the allowlist are rejected with
 * {@link io.gentics.mesh.mcp.domain.ErrorCode#FORBIDDEN}.
 *
 * <h2>Output format</h2>
 * <pre>{@code
 * {
 *   "project": "demo",
 *   "schemas": [
 *     {
 *       "uuid": "…",
 *       "name": "article",
 *       "fields": [
 *         { "name": "title", "type": "string", "required": true },
 *         { "name": "body",  "type": "html",   "required": false }
 *       ]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @see MeshClient#listSchemas(String)
 * @see io.gentics.mesh.mcp.security.PolicyService
 * @since 0.1.0
 */
public class ListSchemasUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshClient client;
    private final MeshConfig config;

    /**
     * @param client Mesh API facade
     * @param config server configuration used for policy checks
     */
    public ListSchemasUseCase(MeshClient client, MeshConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Executes the use-case: validates the project, fetches its schemas, and
     * serialises them to a JSON string.
     *
     * @param project allowlisted project name
     * @param auth     per-call Mesh identity (token from the caller, or NONE)
     * @return JSON string containing the {@code project} name and a {@code schemas} array
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN) if the project is not in the allowlist
     * @throws io.gentics.mesh.mcp.domain.AppError if the Mesh API call or JSON serialisation fails
     */
    public String execute(String project, MeshAuth auth) {
        PolicyService.assertProjectAllowed(project, config);

        var schemas = client.listSchemas(project, auth);

        ObjectNode root        = MAPPER.createObjectNode();
        ArrayNode  schemasArr  = MAPPER.createArrayNode();

        root.put("project", project);

        for (var s : schemas) {
            ObjectNode sNode  = MAPPER.createObjectNode();
            sNode.put("uuid", s.uuid());
            sNode.put("name", s.name());

            ArrayNode fieldsArr = MAPPER.createArrayNode();
            for (var f : s.fields()) {
                ObjectNode fNode = MAPPER.createObjectNode();
                fNode.put("name",     f.name());
                fNode.put("type",     f.type());
                fNode.put("required", f.required());
                fieldsArr.add(fNode);
            }
            sNode.set("fields", fieldsArr);
            schemasArr.add(sNode);
        }

        root.set("schemas", schemasArr);
        return ToolResult.json(root);
    }
}
