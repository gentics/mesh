package io.gentics.mesh.mcp.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.mesh.MeshClient;
import io.gentics.mesh.mcp.security.PolicyService;

/**
 * Application use-case: resolve a Gentics Mesh webroot path to its node.
 *
 * <p>This is the implementation behind the {@code mesh_get_webroot} MCP tool.
 * It enforces project and language policies, delegates to
 * {@link MeshClient#getWebroot(String, String, String)}, and serialises the
 * resulting {@link io.gentics.mesh.mcp.domain.model.MeshNode} to JSON using the
 * shared {@link GetNodeUseCase#toJson(io.gentics.mesh.mcp.domain.model.MeshNode)}
 * helper.
 *
 * <h2>Path normalisation</h2>
 * Leading slash normalisation is handled by {@link MeshClient#getWebroot}:
 * a path like {@code "news/article"} is silently rewritten to {@code "/news/article"}.
 *
 * <h2>Policy checks</h2>
 * <ul>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertProjectAllowed}</li>
 *   <li>{@link io.gentics.mesh.mcp.security.PolicyService#assertLanguageAllowed}</li>
 * </ul>
 *
 * <h2>Error behaviour</h2>
 * If no published node exists at the given path, Mesh returns HTTP 404, which
 * is mapped to {@link io.gentics.mesh.mcp.domain.AppError} with
 * {@link io.gentics.mesh.mcp.domain.ErrorCode#NOT_FOUND}.
 *
 * @see MeshClient#getWebroot(String, String, String)
 * @see GetNodeUseCase#toJson(io.gentics.mesh.mcp.domain.model.MeshNode)
 * @since 0.1.0
 */
public class GetWebrootUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshClient client;
    private final MeshConfig config;

    /**
     * @param client Mesh API facade
     * @param config server configuration used for policy checks
     */
    public GetWebrootUseCase(MeshClient client, MeshConfig config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Executes the use-case: validates project and language, resolves the webroot
     * path, and returns the node as a JSON string.
     *
     * @param project  allowlisted project name
     * @param path     webroot path (e.g. {@code "/automobiles/delorean-dmc-12"})
     * @param language ISO 639-1 language code
     * @return JSON string representing the resolved {@link io.gentics.mesh.mcp.domain.model.MeshNode}
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN)  if the project or language is not allowed
     * @throws io.gentics.mesh.mcp.domain.AppError(NOT_FOUND)  if no node exists at the path
     * @throws io.gentics.mesh.mcp.domain.AppError(INTERNAL_ERROR) if serialisation fails
     */
    public String execute(String project, String path, String language) {
        PolicyService.assertProjectAllowed(project, config);
        PolicyService.assertLanguageAllowed(language, config);

        var node = client.getWebroot(project, path, language);
        return ToolResult.json(GetNodeUseCase.toJson(node));
    }
}
