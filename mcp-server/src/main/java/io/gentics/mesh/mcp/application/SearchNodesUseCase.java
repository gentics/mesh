package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.security.PolicyService;

import java.util.List;

/**
 * Application use-case: full-text search for nodes in a Gentics Mesh project.
 *
 * <p>This is the implementation behind the {@code mesh_search_nodes} MCP tool.
 * It enforces project and language policies, validates the {@code limit} parameter,
 * delegates to {@link MeshClient#searchNodes} (which constructs and submits an
 * Elasticsearch query via Mesh), and serialises the result list to JSON.
 *
 * <h2>Prerequisites</h2>
 * The Gentics Mesh instance must have Elasticsearch configured and running.
 * If Elasticsearch is unavailable, the Mesh API will return an error that is
 * propagated as an {@link io.gentics.mesh.mcp.domain.AppError}.
 *
 * <h2>Policy checks</h2>
 * <ul>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertProjectAllowed}</li>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertLanguageAllowed}</li>
 *   <li>Local validation: {@code limit} must be in the range [1, 20].</li>
 * </ul>
 *
 * <h2>Output format</h2>
 * <pre>{@code
 * {
 *   "results": [
 *     {
 *       "uuid":      "…",
 *       "title":     "Hello World",
 *       "schema":    "article",
 *       "language":  "de",
 *       "path":      "/news/hello-world",
 *       "snippet":   "Lorem ipsum…",
 *       "published": true
 *     }
 *   ]
 * }
 * }</pre>
 *
 * @see MeshClient#searchNodes
 * @see io.gentics.mesh.mcp.domain.model.SearchResult
 * @since 0.1.0
 */
public class SearchNodesUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshClient client;
    private final MeshConfig config;

    /**
     * @param client Mesh API facade
     * @param config server configuration used for policy checks
     */
    public SearchNodesUseCase(MeshClient client, MeshConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Executes the use-case: validates inputs, runs the Elasticsearch query via
     * Mesh, and serialises the search results to a JSON string.
     *
     * @param project       allowlisted project name
     * @param query         Elasticsearch query string expression
     * @param schema        optional schema name filter; {@code null} means no filter
     * @param language      ISO 639-1 language code
     * @param fields        ordered list of field names to prefer for snippet extraction
     * @param limit         maximum result count; must be in [1, 20]
     * @param publishedOnly when {@code true}, only published nodes are returned
     * @param auth     per-call Mesh identity (token from the caller, or NONE)
     * @return JSON string containing a {@code results} array
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN)         if the project or language is not allowed
     * @throws io.gentics.mesh.mcp.domain.AppError(VALIDATION_ERROR)  if {@code limit} is out of range
     * @throws io.gentics.mesh.mcp.domain.AppError if the Mesh API call or JSON serialisation fails
     */
    public String execute(String project, String query,
                          String schema, String language,
                          List<String> fields, int limit,
                          boolean publishedOnly, MeshAuth auth) {

        PolicyService.assertProjectAllowed(project, config);
        PolicyService.assertLanguageAllowed(language, config);

        if (limit < 1 || limit > 20) {
            throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "limit muss zwischen 1 und 20 liegen, war: " + limit);
        }

        var results = client.searchNodes(project, query, schema, language,
                fields, limit, publishedOnly, auth);

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode  arr  = MAPPER.createArrayNode();

        for (var r : results) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("uuid",      r.uuid());
            n.put("title",     r.title());
            n.put("schema",    r.schema());
            n.put("language",  r.language());
            n.put("path",      r.path());
            n.put("snippet",   r.snippet());
            n.put("published", r.published());
            arr.add(n);
        }

        root.set("results", arr);
        return ToolResult.json(root);
    }
}
