package io.gentics.mesh.mcp.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * High-level facade over the official Gentics Mesh REST client.
 *
 * <p>{@code MeshClient} is the single entry point used by all application
 * use-cases. It delegates HTTP transport and authentication to the official
 * {@link MeshRestClient} (which is Vert.x/RxJava-based), and maps typed
 * Mesh DTO responses to the application's own domain model via
 * {@link MeshMapper}.
 *
 * <h2>Approach</h2>
 * <p>The official client returns RxJava 2 {@code Single<T>} instances. This
 * class converts them to synchronous results via {@code .blockingGet()} because
 * the MCP stdio server processes requests sequentially and has no need for
 * async I/O from the application's perspective.
 *
 * <h2>DTO → JsonNode bridge</h2>
 * <p>The Mesh DTO responses are serialised back to {@link JsonNode} using
 * Jackson and then passed through the existing {@link MeshMapper} methods.
 * This keeps the mapping layer stable while the transport layer is upgraded
 * to the official client.
 *
 * <h2>Thread safety</h2>
 * {@code MeshClient} instances are stateless beyond the immutable
 * {@link MeshRestClient} reference. They are safe to share across threads.
 *
 * @see MeshRestClient
 * @see MeshMapper
 * @since 0.2.0
 */
public class MeshClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshConfig       config;
    private final MeshRestClient   client;
    private final MeshGraphqlClient graphql;

    /**
     * Creates a fully wired {@code MeshClient} using the official
     * {@link MeshRestClient} and immediately logs in to obtain a JWT token.
     *
     * <p>{@link MeshRestClient#setLogin(String, String)} only stores the credentials –
     * it does <em>not</em> call the login endpoint. {@link MeshRestClient#login()}
     * must be invoked explicitly to acquire the bearer token; otherwise all
     * subsequent API calls would be made as the anonymous (unauthenticated) user.
     *
     * @param config validated server configuration
     * @throws RuntimeException if the initial login to Mesh fails
     */
    public MeshClient(MeshConfig config) {
        this.config  = config;
        this.client  = buildRestClient(config);
        // setLogin() only stores credentials; login() must be called explicitly to obtain
        // the JWT token. Without this call every request goes out unauthenticated.
        this.client.setLogin(config.meshUsername(), config.meshPassword());
        try {
            this.client.login().blockingGet();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Mesh-Login fehlgeschlagen – Credentials und MESH_BASE_URL prüfen: "
                    + e.getMessage(), e);
        }
        // GraphQL guardrails layer still delegates execution to this client
        this.graphql = new MeshGraphqlClient(config, client);
    }

    /**
     * Package-private constructor for unit testing; allows injecting a mock
     * {@link MeshRestClient}.
     *
     * @param config  validated server configuration
     * @param client  mock or real Mesh REST client
     * @param graphql mock or real GraphQL client
     */
    MeshClient(MeshConfig config, MeshRestClient client, MeshGraphqlClient graphql) {
        this.config  = config;
        this.client  = client;
        this.graphql = graphql;
    }

    // ── Projects ──────────────────────────────────────────────────────────

    /**
     * Returns all projects that exist in Mesh AND are present in
     * {@link MeshConfig#meshAllowedProjects()}.
     *
     * @return filtered, immutable list of projects
     * @throws io.gentics.mesh.mcp.domain.AppError on network or auth failure
     */
    public List<Project> listProjects() {
        ProjectListResponse response = client
                .findProjects(new PagingParametersImpl().setPerPage(200L))
                .blockingGet();

        var result = new ArrayList<Project>();
        if (response.getData() != null) {
            for (var p : response.getData()) {
                if (config.meshAllowedProjects().contains(p.getName())) {
                    result.add(MeshMapper.mapProject(toJsonNode(p)));
                }
            }
        }
        return List.copyOf(result);
    }

    // ── Schemas ───────────────────────────────────────────────────────────

    /**
     * Returns all schemas of the given project, optionally filtered by
     * {@link MeshConfig#meshAllowedSchemas()}.
     *
     * @param project allowlisted project name
     * @return list of schemas (filtered if schema allowlist is configured)
     * @throws io.gentics.mesh.mcp.domain.AppError on network or auth failure
     */
    public List<MeshSchema> listSchemas(String project) {
        // Use var to avoid importing the internal SchemaListResponse class;
        // the compiler infers the correct type from findSchemas()'s return type.
        var response = client
                .findSchemas(project, new PagingParametersImpl().setPerPage(200L))
                .blockingGet();

        var result = new ArrayList<MeshSchema>();
        if (response.getData() != null) {
            for (var s : response.getData()) {
                if (config.meshAllowedSchemas().isEmpty() ||
                        config.meshAllowedSchemas().contains(s.getName())) {
                    result.add(MeshMapper.mapSchema(toJsonNode(s)));
                }
            }
        }
        return List.copyOf(result);
    }

    // ── Node listing ─────────────────────────────────────────────────────

    /**
     * Returns up to 100 nodes of a project as a flat list.
     *
     * @param project  allowlisted project name
     * @param language ISO 639-1 language code
     * @return list of mapped nodes (sensitive fields redacted)
     */
    public List<MeshNode> listNodes(String project, String language) {
        NodeListResponse response = client
                .findNodes(project,
                        new NodeParametersImpl().setLanguages(language),
                        new PagingParametersImpl().setPerPage(100L))
                .blockingGet();

        var result = new ArrayList<MeshNode>();
        if (response.getData() != null) {
            for (var node : response.getData()) {
                result.add(MeshMapper.mapNode(toJsonNode(node), project));
            }
        }
        return List.copyOf(result);
    }

    // ── Single node ───────────────────────────────────────────────────────

    /**
     * Loads a single node by UUID and language.
     *
     * @param project  allowlisted project name
     * @param uuid     node UUID
     * @param language ISO 639-1 language code
     * @return normalised node with redacted sensitive fields
     * @throws io.gentics.mesh.mcp.domain.AppError(NOT_FOUND) if the UUID does not exist
     */
    public MeshNode getNode(String project, String uuid, String language) {
        NodeResponse response = client
                .findNodeByUuid(project, uuid,
                        new NodeParametersImpl().setLanguages(language))
                .blockingGet();
        return MeshMapper.mapNode(toJsonNode(response), project);
    }

    // ── Search ────────────────────────────────────────────────────────────

    /**
     * Performs a full-text search via the Elasticsearch backend of Mesh.
     *
     * <p><strong>Requires</strong> Elasticsearch to be configured and running
     * in the Mesh stack. Returns an empty list (not an error) if Mesh returns
     * no results.
     *
     * @param project       allowlisted project name
     * @param query         Elasticsearch query string or {@code query_string} expression
     * @param schema        optional schema name filter; {@code null} = no filter
     * @param language      ISO 639-1 language code
     * @param snippetFields ordered list of field names to use for snippet extraction
     * @param limit         maximum number of results (1–20)
     * @param publishedOnly when {@code true}, only published nodes are returned
     * @return list of search results with snippets
     */
    public List<SearchResult> searchNodes(
            String project, String query, String schema, String language,
            List<String> snippetFields, int limit, boolean publishedOnly) {

        Map<String, Object> esBody = buildEsQuery(query, schema, language, publishedOnly, limit);
        String esBodyJson;
        try {
            esBodyJson = MAPPER.writeValueAsString(esBody);
        } catch (Exception e) {
            esBodyJson = "{\"query\":{\"query_string\":{\"query\":\"" + query + "\"}}}";
        }

        NodeListResponse response = client
                .searchNodes(project, esBodyJson,
                        new PagingParametersImpl().setPerPage((long) limit))
                .blockingGet();

        var result = new ArrayList<SearchResult>();
        if (response.getData() != null) {
            for (var node : response.getData()) {
                result.add(MeshMapper.mapSearchResult(toJsonNode(node), snippetFields));
            }
        }
        return List.copyOf(result);
    }

    // ── Webroot ───────────────────────────────────────────────────────────

    /**
     * Resolves a webroot path to its corresponding node.
     *
     * <p>Leading slashes are normalised automatically.
     *
     * @param project  allowlisted project name
     * @param path     webroot path (e.g. {@code "/automobiles/delorean-dmc-12"})
     * @param language ISO 639-1 language code
     * @return normalised node at the given path
     * @throws io.gentics.mesh.mcp.domain.AppError(NOT_FOUND) if no node exists at the path
     */
    public MeshNode getWebroot(String project, String path, String language) {
        String normPath = path.startsWith("/") ? path : "/" + path;
        // webroot() accepts the path split into individual segments as varargs
        String[] segments = normPath.substring(1).split("/");
        // webroot() returns MeshWebrootResponse which wraps either a node or a binary;
        // getNodeResponse() yields the underlying NodeResponse.
        MeshWebrootResponse webrootResponse = client
                .webroot(project, segments,
                        new NodeParametersImpl().setLanguages(language))
                .blockingGet();
        NodeResponse response = webrootResponse.getNodeResponse();
        return MeshMapper.mapWebrootNode(toJsonNode(response), normPath, project);
    }

    // ── GraphQL ───────────────────────────────────────────────────────────

    /**
     * Executes a GraphQL query via {@link MeshGraphqlClient} (guardrails applied).
     *
     * @param project   allowlisted project name
     * @param query     read-only GraphQL query string
     * @param variables optional variables map
     * @return raw Mesh GraphQL response JSON
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN)        on mutation attempt
     * @throws io.gentics.mesh.mcp.domain.AppError(VALIDATION_ERROR) on depth violation
     */
    public JsonNode runGraphql(String project, String query, Map<String, Object> variables) {
        return graphql.query(project, query, variables);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Serialises any Mesh DTO to a {@link JsonNode} using Jackson.
     *
     * <p>Mesh DTOs are Jackson-annotated and their serialised form matches the
     * raw Mesh REST API JSON structure. This bridge lets us reuse the existing
     * {@link MeshMapper} methods without modification.
     *
     * @param dto any Jackson-serialisable Mesh DTO
     * @return the DTO serialised as a {@link JsonNode}
     */
    private static JsonNode toJsonNode(Object dto) {
        return MAPPER.valueToTree(dto);
    }

    /**
     * Constructs a simple Elasticsearch {@code bool/must} query body.
     *
     * @param query        user-provided query string
     * @param schema       optional schema name filter
     * @param language     language code filter
     * @param publishedOnly add a published=true term filter when {@code true}
     * @param limit        result size
     * @return Elasticsearch request body as a {@code Map}
     */
    private Map<String, Object> buildEsQuery(
            String query, String schema, String language,
            boolean publishedOnly, int limit) {

        var must = new ArrayList<Map<String, Object>>();
        must.add(Map.of("query_string", Map.of("query", query)));

        if (language != null && !language.isBlank()) {
            must.add(Map.of("term", Map.of("language", language)));
        }
        if (schema != null && !schema.isBlank()) {
            must.add(Map.of("term", Map.of("schema.name.raw", schema)));
        }
        if (publishedOnly) {
            must.add(Map.of("term", Map.of("published", true)));
        }

        return Map.of(
                "query", Map.of("bool", Map.of("must", must)),
                "size",  limit
        );
    }

    /**
     * Creates and configures the official {@link MeshRestClient} from
     * the application {@link MeshConfig}.
     *
     * <p>Parses {@link MeshConfig#meshBaseUrl()} to extract host, port and
     * TLS flag. The Vert.x instance used internally by the client is managed
     * by the client itself.
     *
     * @param config validated server configuration
     * @return a ready-to-use {@link MeshRestClient}
     */
    private static MeshRestClient buildRestClient(MeshConfig config) {
        try {
            URI uri  = URI.create(config.meshBaseUrl());
            String host = uri.getHost();
            int    port = uri.getPort() != -1 ? uri.getPort()
                    : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
            boolean ssl = "https".equalsIgnoreCase(uri.getScheme());

            MeshRestClientConfig clientConfig = MeshRestClientConfig.newConfig()
                    .setHost(host)
                    .setPort(port)
                    .setSsl(ssl)
                    .build();

            return MeshRestClient.create(clientConfig);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Konnte MeshRestClient nicht erstellen aus URL: " + config.meshBaseUrl(), e);
        }
    }
}
