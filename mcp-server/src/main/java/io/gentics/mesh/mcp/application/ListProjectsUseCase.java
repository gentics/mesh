package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshClient;

/**
 * Application use-case: list all accessible Gentics Mesh projects.
 *
 * <p>This is the implementation behind the {@code mesh_list_projects} MCP tool.
 * It delegates to {@link MeshClient#listProjects()}, which already applies the
 * project allowlist filter, and serialises the result to a JSON string that the
 * MCP framework returns verbatim to the LLM client.
 *
 * <h2>No policy checks required</h2>
 * {@code listProjects} is the one tool that requires no project parameter and
 * therefore no allowlist assertion. The filtering is applied inside
 * {@link MeshClient#listProjects()} itself, which only returns projects that are
 * present in {@link io.gentics.mesh.mcp.config.MeshConfig#meshAllowedProjects()}.
 *
 * <h2>Output format</h2>
 * <pre>{@code
 * {
 *   "projects": [
 *     { "name": "demo", "uuid": "abc123…" },
 *     …
 *   ]
 * }
 * }</pre>
 *
 * @see MeshClient#listProjects()
 * @see io.gentics.mesh.mcp.server.McpServerRunner
 * @since 0.1.0
 */
public class ListProjectsUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshClient client;
    private final MeshConfig config;

    /**
     * @param client Mesh API facade
     * @param config server configuration (provided for consistency; not directly used here)
     */
    public ListProjectsUseCase(MeshClient client, MeshConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Executes the use-case: fetches and filters all projects, then serialises
     * them to a JSON string.
     *
     * @return JSON string containing a {@code projects} array
     * @throws io.gentics.mesh.mcp.domain.AppError if the Mesh API call or JSON serialisation fails
     */
    public String execute() {
        var projects  = client.listProjects();
        ObjectNode root  = MAPPER.createObjectNode();
        ArrayNode  array = MAPPER.createArrayNode();

        for (var p : projects) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("name", p.name());
            node.put("uuid", p.uuid());
            array.add(node);
        }

        root.set("projects", array);
        return ToolResult.json(root);
    }
}
