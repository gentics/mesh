package io.gentics.mesh.mcp.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.model.*;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

    private static final Logger log = LoggerFactory.getLogger(MeshClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Mesh's built-in fallback user – what {@code me()} reports when auth did not take. */
    private static final String ANONYMOUS_USERNAME = "anonymous";

    /** Transport whose Mesh identity comes from configuration instead of per request. */
    private static final String STDIO_TRANSPORT = "stdio";

    private final MeshConfig       config;
    private final MeshRestClient   client;
    private final MeshGraphqlClient graphql;

    /**
     * Creates a fully wired {@code MeshClient} using the official
     * {@link MeshRestClient} and authenticates it against Mesh.
     *
     * <h2>Two authentication modes</h2>
     * <ul>
     *   <li><strong>API key</strong> ({@code MESH_API_KEY}) – a long-lived JWT
     *       issued by Mesh. {@link MeshRestClient#setAPIKey(String)} attaches it
     *       as a Bearer header to every request; there is no login endpoint and
     *       no session to refresh.</li>
     *   <li><strong>Username/password</strong> – {@link MeshRestClient#setLogin(String, String)}
     *       only <em>stores</em> the credentials; {@link MeshRestClient#login()}
     *       must be invoked explicitly to acquire the JWT, otherwise every
     *       request goes out as the anonymous user.</li>
     * </ul>
     * When both are configured the API key wins.
     *
     * <p>Either way the result is verified via {@link #verifyAuthentication(String)}
     * before the constructor returns, so a misconfigured server fails at startup
     * rather than serving anonymous-user results on the first tool call.
     *
     * @param config validated server configuration
     * @throws RuntimeException if authentication against Mesh fails
     */
    public MeshClient(MeshConfig config) {
        this.config  = config;
        this.client  = buildRestClient(config);
        if (STDIO_TRANSPORT.equals(config.mcpTransport())) {
            authenticateFromConfig();
        } else {
            verifyReachable();
        }
        // GraphQL guardrails layer still delegates execution to this client
        this.graphql = new MeshGraphqlClient(config, client);
    }

    /**
     * stdio: gives the shared client the single identity it will use for every
     * call, from {@code MESH_API_KEY} or {@code MESH_USERNAME}/{@code MESH_PASSWORD}.
     */
    private void authenticateFromConfig() {
        if (config.meshApiKey() != null) {
            if (config.meshUsername() != null) {
                log.warn("MESH_API_KEY und MESH_USERNAME sind beide gesetzt – "
                        + "der API-Key hat Vorrang, die Credentials werden ignoriert.");
            }
            client.setAPIKey(config.meshApiKey());
            verifyAuthentication("Mesh-API-Key wurde abgelehnt – MESH_API_KEY und MESH_BASE_URL prüfen");
        } else {
            client.setLogin(config.meshUsername(), config.meshPassword());
            try {
                awaitSingle(client.login(), config.meshRestTimeoutMs());
            } catch (Exception e) {
                throw new RuntimeException(
                        "Mesh-Login fehlgeschlagen – Credentials und MESH_BASE_URL prüfen: "
                        + e.getMessage(), e);
            }
            verifyAuthentication("Mesh-Login fehlgeschlagen – Credentials und MESH_BASE_URL prüfen");
        }
    }

    /**
     * HTTP: there is nothing to authenticate at startup – the Mesh identity
     * arrives per request, or the call is anonymous. This only proves that
     * {@code MESH_BASE_URL} points at something that answers, so a typo or an
     * unreachable host is caught at startup rather than on the first tool call.
     *
     * <p>{@code me()} works unauthenticated (Mesh replies as the anonymous user),
     * which makes it a usable ping without credentials.
     */
    private void verifyReachable() {
        try {
            awaitSingle(client.me().toSingle(), config.meshRestTimeoutMs());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Mesh ist nicht erreichbar – MESH_BASE_URL prüfen: " + e.getMessage(), e);
        }
        log.info("Mesh erreichbar unter {} – Requests laufen anonym bzw. mit dem "
                + "Bearer-Token des Clients", config.meshBaseUrl());
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

    /**
     * Asks Mesh who we are and fails startup unless it answers with a real user.
     *
     * <p>{@code GET /api/v2/auth/me} never returns an HTTP error for bad
     * credentials – Mesh simply answers as the built-in {@code anonymous} user.
     * A 200 response therefore proves nothing on its own; the username is what
     * distinguishes "authenticated" from "silently anonymous". This matters most
     * for the API-key path, which has no login endpoint that could fail loudly.
     *
     * @param failureMessage operator-facing prefix for the thrown exception
     * @throws RuntimeException if the request fails, times out, or Mesh answers
     *                          as the anonymous user
     */
    private void verifyAuthentication(String failureMessage) {
        UserResponse user;
        try {
            user = awaitSingle(client.me().toSingle(), config.meshRestTimeoutMs());
        } catch (Exception e) {
            throw new RuntimeException(failureMessage + ": " + e.getMessage(), e);
        }
        if (user == null || ANONYMOUS_USERNAME.equals(user.getUsername())) {
            throw new RuntimeException(failureMessage
                    + ": Mesh antwortet als anonymer Benutzer – die Zugangsdaten "
                    + "wurden nicht akzeptiert.");
        }
        log.info("Mesh-Authentifizierung verifiziert – angemeldet als '{}'", user.getUsername());
    }

    /**
     * Subscribes to the given {@link Single} and waits for its result.
     *
     * <p>Used instead of {@code blockingGet()} so the wait is bounded: a Mesh
     * that accepts the connection but never answers would otherwise hang startup
     * forever with no diagnostic.
     *
     * @param single    the request to run
     * @param timeoutMs how long to wait for the result
     * @return the emitted value
     * @throws IllegalStateException on timeout or when the Single signalled an error
     * @throws InterruptedException  if the waiting thread is interrupted
     */
    private static <T> T awaitSingle(Single<T> single, long timeoutMs) throws InterruptedException {
        var done    = new CountDownLatch(1);
        var value   = new AtomicReference<T>();
        var failure = new AtomicReference<Throwable>();

        single.subscribe(
                v -> { value.set(v);   done.countDown(); },
                e -> { failure.set(e); done.countDown(); });

        if (!done.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException("Keine Antwort von Mesh innerhalb von " + timeoutMs + " ms");
        }
        Throwable error = failure.get();
        if (error != null) {
            throw new IllegalStateException(error.getMessage(), error);
        }
        return value.get();
    }

    /**
     * Attaches the caller's Mesh identity to a single outgoing request.
     *
     * <p>The token is set on the {@link MeshRequest}, never on the shared
     * {@link MeshRestClient}: concurrent tool calls each carry their own
     * identity and cannot inherit one another's. {@link MeshAuth#NONE} leaves
     * the request untouched – Mesh then applies whatever the client itself
     * carries (stdio) or treats it as anonymous (HTTP).
     *
     * @param request the request about to be executed
     * @param auth    per-call identity; {@code null} is treated as {@link MeshAuth#NONE}
     * @return the same request, for chaining
     */
    private static <T> MeshRequest<T> withAuth(MeshRequest<T> request, MeshAuth auth) {
        if (auth != null && auth.hasToken()) {
            request.setHeader("Authorization", "Bearer " + auth.bearerToken());
        }
        return request;
    }

    // ── Projects ──────────────────────────────────────────────────────────

    /**
     * Returns all projects that exist in Mesh AND are present in
     * {@link MeshConfig#meshAllowedProjects()}.
     *
     * @param auth per-call Mesh identity
     * @return filtered, immutable list of projects
     * @throws io.gentics.mesh.mcp.domain.AppError on network or auth failure
     */
    public List<Project> listProjects(MeshAuth auth) {
        ProjectListResponse response = withAuth(client
                .findProjects(new PagingParametersImpl().setPerPage(200L)), auth)
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
     * @param auth    per-call Mesh identity
     * @return list of schemas (filtered if schema allowlist is configured)
     * @throws io.gentics.mesh.mcp.domain.AppError on network or auth failure
     */
    public List<MeshSchema> listSchemas(String project, MeshAuth auth) {
        // Use var to avoid importing the internal SchemaListResponse class;
        // the compiler infers the correct type from findSchemas()'s return type.
        var response = withAuth(client
                .findSchemas(project, new PagingParametersImpl().setPerPage(200L)), auth)
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
     * @param auth     per-call Mesh identity
     * @return list of mapped nodes (sensitive fields redacted)
     */
    public List<MeshNode> listNodes(String project, String language, MeshAuth auth) {
        NodeListResponse response = withAuth(client
                .findNodes(project,
                        new NodeParametersImpl().setLanguages(language),
                        new PagingParametersImpl().setPerPage(100L)), auth)
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
     * @param auth     per-call Mesh identity
     * @return normalised node with redacted sensitive fields
     * @throws io.gentics.mesh.mcp.domain.AppError(NOT_FOUND) if the UUID does not exist
     */
    public MeshNode getNode(String project, String uuid, String language, MeshAuth auth) {
        NodeResponse response = withAuth(client
                .findNodeByUuid(project, uuid,
                        new NodeParametersImpl().setLanguages(language)), auth)
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
     * @param auth          per-call Mesh identity
     * @return list of search results with snippets
     */
    public List<SearchResult> searchNodes(
            String project, String query, String schema, String language,
            List<String> snippetFields, int limit, boolean publishedOnly, MeshAuth auth) {

        Map<String, Object> esBody = buildEsQuery(query, schema, language, publishedOnly, limit);
        String esBodyJson;
        try {
            esBodyJson = MAPPER.writeValueAsString(esBody);
        } catch (Exception e) {
            esBodyJson = "{\"query\":{\"query_string\":{\"query\":\"" + query + "\"}}}";
        }

        NodeListResponse response = withAuth(client
                .searchNodes(project, esBodyJson,
                        new PagingParametersImpl().setPerPage((long) limit)), auth)
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
     * @param auth     per-call Mesh identity
     * @return normalised node at the given path
     * @throws io.gentics.mesh.mcp.domain.AppError(NOT_FOUND) if no node exists at the path
     */
    public MeshNode getWebroot(String project, String path, String language, MeshAuth auth) {
        String normPath = path.startsWith("/") ? path : "/" + path;
        // webroot() accepts the path split into individual segments as varargs
        String[] segments = normPath.substring(1).split("/");
        // webroot() returns MeshWebrootResponse which wraps either a node or a binary;
        // getNodeResponse() yields the underlying NodeResponse.
        MeshWebrootResponse webrootResponse = withAuth(client
                .webroot(project, segments,
                        new NodeParametersImpl().setLanguages(language)), auth)
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
     * @param auth      per-call Mesh identity
     * @return raw Mesh GraphQL response JSON
     * @throws io.gentics.mesh.mcp.domain.AppError(FORBIDDEN)        on mutation attempt
     * @throws io.gentics.mesh.mcp.domain.AppError(VALIDATION_ERROR) on depth violation
     */
    public JsonNode runGraphql(String project, String query, Map<String, Object> variables,
                               MeshAuth auth) {
        return graphql.query(project, query, variables, auth);
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
