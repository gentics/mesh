package io.gentics.mesh.mcp.showcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.gentics.mesh.mcp.application.*;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.model.MeshNode;
import io.gentics.mesh.mcp.mesh.MeshAuth;
import io.gentics.mesh.mcp.mesh.MeshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server that powers the Gentics Mesh MCP Showcase UI.
 *
 * <p>Starts a local web server on port 3000 and serves the {@code showcase/index.html}
 * frontend together with a small REST API that the browser-side JavaScript calls.
 * The API internally reuses the same {@link MeshClient} and use-case layer as the
 * MCP server – no duplicate business logic.
 *
 * <h2>Endpoints</h2>
 * <table border="1" summary="Showcase API endpoints">
 *   <tr><th>Method + Path</th><th>Description</th></tr>
 *   <tr><td>GET /</td><td>Serves showcase/index.html from the JAR classpath</td></tr>
 *   <tr><td>GET /api/projects</td><td>Allowed project list</td></tr>
 *   <tr><td>GET /api/schemas?project=</td><td>Schemas of a project</td></tr>
 *   <tr><td>GET /api/nodes?project=&amp;schema=&amp;lang=</td><td>Node listing (max 100)</td></tr>
 *   <tr><td>GET /api/node/{uuid}?project=&amp;lang=</td><td>Single node by UUID</td></tr>
 *   <tr><td>GET /api/webroot?project=&amp;path=&amp;lang=</td><td>Node by webroot path</td></tr>
 * </table>
 *
 * <h2>Start</h2>
 * <pre>{@code
 * java -jar mesh-mcp-server-0.1.0.jar --showcase
 * }</pre>
 * Then open {@code http://localhost:3000} in your browser.
 *
 * @see io.gentics.mesh.mcp.Main
 * @since 0.1.0
 */
public class ShowcaseServer {

    private static final Logger log = LoggerFactory.getLogger(ShowcaseServer.class);
    private static final int    PORT   = 3000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MeshConfig config;
    private final MeshClient client;

    /**
     * @param config validated server configuration (env-based)
     * @param client fully wired Mesh API client
     */
    public ShowcaseServer(MeshConfig config, MeshClient client) {
        this.config = config;
        this.client = client;
    }

    /**
     * Starts the HTTP server and blocks until the JVM exits.
     *
     * @throws IOException if the server socket cannot be bound
     */
    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::dispatch);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.err.println();
        System.err.println("  🚀  Gentics Mesh MCP Showcase läuft auf http://localhost:" + PORT);
        System.err.println("       Mesh-Backend : " + config.meshBaseUrl());
        System.err.println("       Projekte     : " + String.join(", ", config.meshAllowedProjects()));
        System.err.println();
    }

    // ── Dispatcher ────────────────────────────────────────────────────────

    private void dispatch(HttpExchange ex) {
        try {
            // CORS – always set before any response
            ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }

            String          path   = ex.getRequestURI().getPath();
            Map<String,String> q   = queryParams(ex.getRequestURI());

            if ("/".equals(path) || "/index.html".equals(path)) {
                serveHtml(ex);
            } else if ("/api/projects".equals(path)) {
                handleProjects(ex);
            } else if ("/api/schemas".equals(path)) {
                handleSchemas(ex, q);
            } else if ("/api/nodes".equals(path)) {
                handleNodes(ex, q);
            } else if (path.startsWith("/api/node/")) {
                handleNode(ex, path.substring("/api/node/".length()), q);
            } else if ("/api/webroot".equals(path)) {
                handleWebroot(ex, q);
            } else {
                sendJson(ex, 404, "{\"error\":\"Not found\"}");
            }
        } catch (Exception e) {
            log.error("Showcase request error", e);
            try {
                String body = MAPPER.writeValueAsString(Map.of("error", e.getMessage()));
                sendJson(ex, 500, body);
            } catch (Exception ignored) { /* best-effort */ }
        }
    }

    // ── HTML ──────────────────────────────────────────────────────────────

    private void serveHtml(HttpExchange ex) throws IOException {
        InputStream is = getClass().getResourceAsStream("/showcase/index.html");
        if (is == null) {
            sendJson(ex, 500, "{\"error\":\"showcase/index.html not found in JAR\"}");
            return;
        }
        byte[] html = is.readAllBytes();
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, html.length);
        try (var out = ex.getResponseBody()) { out.write(html); }
    }

    // ── API handlers ──────────────────────────────────────────────────────

    /** GET /api/projects  →  { "projects": [ { "name": "...", "uuid": "..." } ] } */
    private void handleProjects(HttpExchange ex) throws Exception {
        String json = new ListProjectsUseCase(client, config).execute(MeshAuth.NONE);
        sendJson(ex, 200, json);
    }

    /** GET /api/schemas?project=demo  →  { "project": "demo", "schemas": [...] } */
    private void handleSchemas(HttpExchange ex, Map<String,String> q) throws Exception {
        String project = q.getOrDefault("project", firstAllowedProject());
        String json    = new ListSchemasUseCase(client, config).execute(project, MeshAuth.NONE);
        sendJson(ex, 200, json);
    }

    /**
     * GET /api/nodes?project=demo&schema=article&lang=de
     * →  { "nodes": [ { "uuid","displayName","schema","path","language","published","fields" } ] }
     */
    private void handleNodes(HttpExchange ex, Map<String,String> q) throws Exception {
        String project  = q.getOrDefault("project",  firstAllowedProject());
        String schema   = q.get("schema");
        String language = q.getOrDefault("lang", defaultLanguage());

        List<MeshNode> nodes = client.listNodes(project, language, MeshAuth.NONE);

        // Optional schema filter
        if (schema != null && !schema.isBlank()) {
            nodes = nodes.stream()
                         .filter(n -> schema.equals(n.schema()))
                         .toList();
        }

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode  arr  = MAPPER.createArrayNode();
        for (MeshNode n : nodes) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("uuid",        n.uuid());
            node.put("displayName", n.displayName());
            node.put("schema",      n.schema());
            node.put("path",        n.path());
            node.put("language",    n.language());
            node.put("published",   n.published());
            node.putPOJO("fields",  n.fields());
            arr.add(node);
        }
        root.set("nodes", arr);
        sendJson(ex, 200, MAPPER.writeValueAsString(root));
    }

    /** GET /api/node/{uuid}?project=demo&lang=de  →  full node JSON */
    private void handleNode(HttpExchange ex, String uuid,
                            Map<String,String> q) throws Exception {
        String project  = q.getOrDefault("project",  firstAllowedProject());
        String language = q.getOrDefault("lang", defaultLanguage());
        String json     = new GetNodeUseCase(client, config).execute(project, uuid, language, MeshAuth.NONE);
        sendJson(ex, 200, json);
    }

    /** GET /api/webroot?project=demo&path=/vehicles/delorean&lang=de  →  full node JSON */
    private void handleWebroot(HttpExchange ex, Map<String,String> q) throws Exception {
        String project  = q.getOrDefault("project",  firstAllowedProject());
        String path     = q.getOrDefault("path",     "/");
        String language = q.getOrDefault("lang",     defaultLanguage());
        String json     = new GetWebrootUseCase(client, config).execute(project, path, language, MeshAuth.NONE);
        sendJson(ex, 200, json);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (var out = ex.getResponseBody()) { out.write(bytes); }
    }

    /** Parses {@code key=value&…} query string into a map. */
    private static Map<String, String> queryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) return params;
        Arrays.stream(query.split("&"))
              .forEach(pair -> {
                  int idx = pair.indexOf('=');
                  if (idx > 0) params.put(
                          pair.substring(0, idx),
                          pair.substring(idx + 1));
              });
        return params;
    }

    /** Returns the first project in the allowlist as a safe default. */
    private String firstAllowedProject() {
        return config.meshAllowedProjects().get(0);
    }

    /** Returns the first configured language as the request default (e.g. "de"). */
    private String defaultLanguage() {
        List<String> langs = config.meshAllowedLanguages();
        return (langs != null && !langs.isEmpty()) ? langs.get(0) : "de";
    }
}
