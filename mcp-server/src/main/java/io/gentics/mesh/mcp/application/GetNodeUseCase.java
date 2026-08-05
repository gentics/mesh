package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.model.MeshNode;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.security.PolicyService;

/**
 * Application use-case: load a single Gentics Mesh node by UUID.
 *
 * <p>This is the implementation behind the {@code mesh_get_node} MCP tool.
 * It enforces project and language policy, loads the node from the Mesh REST API
 * via {@link MeshClient#getNode(String, String, String)}, and serialises the
 * normalised {@link MeshNode} domain record to JSON.
 *
 * <h2>Policy checks</h2>
 * <ul>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertProjectAllowed} –
 *       rejects unknown projects.</li>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertLanguageAllowed} –
 *       rejects languages not in the allowlist (if configured).</li>
 * </ul>
 *
 * <h2>Output format</h2>
 * See {@link #toJson(MeshNode)} for the complete JSON structure. All sensitive
 * fields in the {@code fields} map are already redacted by
 * {@link io.gentics.mesh.mcp.security.RedactionService} at the mapping layer.
 *
 * @see MeshClient#getNode(String, String, String)
 * @see MeshNode
 * @see io.gentics.mesh.mcp.security.PolicyService
 * @since 0.1.0
 */
public class GetNodeUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshClient client;
    private final MeshConfig config;

    /**
     * @param client Mesh API facade
     * @param config server configuration used for policy checks
     */
    public GetNodeUseCase(MeshClient client, MeshConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Executes the use-case: validates project and language, loads the node by
     * UUID, and returns a JSON representation.
     *
     * @param project  allowlisted project name
     * @param uuid     Mesh node UUID
     * @param language ISO 639-1 language code (e.g. {@code "de"}, {@code "en"})
     * @return JSON string representing the {@link MeshNode}
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN)  if the project or language is not allowed
     * @throws io.gentics.mesh.mcp.domain.AppError(NOT_FOUND)  if no node with the given UUID exists
     * @throws io.gentics.mesh.mcp.domain.AppError(INTERNAL_ERROR) if serialisation fails
     */
    public String execute(String project, String uuid, String language) {
        PolicyService.assertProjectAllowed(project, config);
        PolicyService.assertLanguageAllowed(language, config);

        MeshNode node = client.getNode(project, uuid, language);
        return ToolResult.json(toJson(node));
    }

    /**
     * Converts a {@link MeshNode} domain record to a Jackson {@link ObjectNode}.
     *
     * <p>This method is {@code static} and package-visible so it can be reused by
     * {@link GetWebrootUseCase} without duplicating the serialisation logic.
     *
     * <p>Output structure:
     * <pre>{@code
     * {
     *   "uuid":        "…",
     *   "project":     "demo",
     *   "language":    "de",
     *   "schema":      "article",
     *   "displayName": "Hello World",
     *   "path":        "/news/hello-world",
     *   "published":   true,
     *   "version":     "1.0",
     *   "fields":      { … redacted … },
     *   "references":  [{ "field": "author", "uuid": "…" }]
     * }
     * }</pre>
     *
     * @param node the domain record to serialise
     * @return a Jackson {@link ObjectNode} ready for JSON rendering
     */
    static ObjectNode toJson(MeshNode node) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("uuid",        node.uuid());
        root.put("project",     node.project());
        root.put("language",    node.language());
        root.put("schema",      node.schema());
        root.put("displayName", node.displayName());
        root.put("path",        node.path());
        root.put("published",   node.published());
        root.put("version",     node.version());
        root.putPOJO("fields",  node.fields());

        ArrayNode refs = mapper.createArrayNode();
        for (var ref : node.references()) {
            ObjectNode r = mapper.createObjectNode();
            r.put("field", ref.field());
            r.put("uuid",  ref.uuid());
            refs.add(r);
        }
        root.set("references", refs);
        return root;
    }
}
