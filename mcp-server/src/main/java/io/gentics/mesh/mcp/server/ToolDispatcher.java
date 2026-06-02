package io.gentics.mesh.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gentics.mesh.mcp.application.GetNodeUseCase;
import io.gentics.mesh.mcp.application.GetWebrootUseCase;
import io.gentics.mesh.mcp.application.ListProjectsUseCase;
import io.gentics.mesh.mcp.application.ListSchemasUseCase;
import io.gentics.mesh.mcp.application.RunGraphqlUseCase;
import io.gentics.mesh.mcp.application.SearchNodesUseCase;
import io.gentics.mesh.mcp.config.MeshConfig;
import io.gentics.mesh.mcp.domain.AppError;
import io.gentics.mesh.mcp.domain.ErrorCode;
import io.gentics.mesh.mcp.mesh.MeshClient;

import java.util.List;
import java.util.Map;

/**
 * Transport-agnostic registry of all MCP tools exposed by the server.
 *
 * <p>This class owns the tool definitions (name + description + JSON Schema)
 * and the dispatch logic that turns a {@code (toolName, args)} pair into the
 * resulting JSON string. Both the stdio transport ({@link McpServerRunner})
 * and the HTTP transport ({@link HttpMcpServer}) delegate to it, so the
 * tool surface stays identical across transports.
 *
 * <p>Validation, policy checks and Mesh I/O happen inside the use-case classes
 * called from {@link #dispatch(String, Map)}. This class is intentionally
 * thin: it only routes arguments and surfaces failures as {@link AppError}.
 *
 * @since 0.2.0
 */
public final class ToolDispatcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Static metadata for a single MCP tool. */
    public record ToolSpec(String name, String description, JsonNode schema, String schemaJson) {}

    private final ListProjectsUseCase listProjects;
    private final ListSchemasUseCase  listSchemas;
    private final GetNodeUseCase      getNode;
    private final SearchNodesUseCase  searchNodes;
    private final GetWebrootUseCase   getWebroot;
    private final RunGraphqlUseCase   runGraphql;

    private final List<ToolSpec> tools;

    public ToolDispatcher(MeshConfig config, MeshClient client) {
        this.listProjects = new ListProjectsUseCase(client, config);
        this.listSchemas  = new ListSchemasUseCase(client, config);
        this.getNode      = new GetNodeUseCase(client, config);
        this.searchNodes  = new SearchNodesUseCase(client, config);
        this.getWebroot   = new GetWebrootUseCase(client, config);
        this.runGraphql   = new RunGraphqlUseCase(client, config);
        this.tools        = buildToolSpecs();
    }

    /** Returns all tool specs in stable registration order. */
    public List<ToolSpec> tools() {
        return tools;
    }

    /**
     * Invokes the named tool with the given arguments.
     *
     * @param name MCP tool name (e.g. {@code "mesh_get_node"})
     * @param args raw argument map as parsed from JSON-RPC params
     * @return JSON string with the tool's result payload
     * @throws AppError when validation fails, the tool is unknown,
     *                  or the upstream Mesh request fails
     */
    public String dispatch(String name, Map<String, Object> args) {
        if (args == null) args = Map.of();
        return switch (name) {
            case "mesh_list_projects" -> listProjects.execute();
            case "mesh_list_schemas"  -> listSchemas.execute(require(args, "project"));
            case "mesh_get_node"      -> getNode.execute(
                    require(args, "project"),
                    require(args, "uuid"),
                    str(args, "language", "de"));
            case "mesh_search_nodes"  -> dispatchSearch(args);
            case "mesh_get_webroot"   -> getWebroot.execute(
                    require(args, "project"),
                    require(args, "path"),
                    str(args, "language", "de"));
            case "mesh_run_graphql"   -> dispatchGraphql(args);
            default -> throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "Unbekanntes Tool: '" + name + "'.");
        };
    }

    // ── private dispatchers with structured argument extraction ───────────

    @SuppressWarnings("unchecked")
    private String dispatchSearch(Map<String, Object> args) {
        List<String> fields = args.containsKey("fields")
                ? (List<String>) args.get("fields")
                : List.of("title", "teaser", "body");
        return searchNodes.execute(
                require(args, "project"),
                require(args, "query"),
                (String) args.get("schema"),
                str(args, "language", "de"),
                fields,
                num(args, "limit", 5),
                bool(args, "publishedOnly", true));
    }

    @SuppressWarnings("unchecked")
    private String dispatchGraphql(Map<String, Object> args) {
        Map<String, Object> vars = args.containsKey("variables")
                ? (Map<String, Object>) args.get("variables")
                : Map.of();
        return runGraphql.execute(
                require(args, "project"),
                require(args, "query"),
                vars);
    }

    // ── tool specs ─────────────────────────────────────────────────────────

    private static List<ToolSpec> buildToolSpecs() {
        return List.of(
            spec("mesh_list_projects",
                "Listet alle erlaubten und erreichbaren Gentics-Mesh-Projekte.",
                "{\"type\":\"object\",\"properties\":{}}"),
            spec("mesh_list_schemas",
                "Listet Schemas eines erlaubten Projekts mit allen Felddefinitionen.",
                """
                {"type":"object","required":["project"],
                 "properties":{"project":{"type":"string","description":"Projektname"}}}
                """),
            spec("mesh_get_node",
                "Lädt einen Node per UUID und gibt eine normalisierte Struktur zurück.",
                """
                {"type":"object","required":["project","uuid"],
                 "properties":{
                   "project":{"type":"string"},
                   "uuid":{"type":"string"},
                   "language":{"type":"string","default":"de"},
                   "resolveReferencesDepth":{"type":"integer","minimum":0,"maximum":2,"default":0}
                 }}
                """),
            spec("mesh_search_nodes",
                "Volltextsuche über Nodes eines Projekts (benötigt Elasticsearch).",
                """
                {"type":"object","required":["project","query"],
                 "properties":{
                   "project":{"type":"string"},
                   "query":{"type":"string"},
                   "schema":{"type":"string"},
                   "language":{"type":"string","default":"de"},
                   "fields":{"type":"array","items":{"type":"string"}},
                   "limit":{"type":"integer","minimum":1,"maximum":20,"default":5},
                   "publishedOnly":{"type":"boolean","default":true}
                 }}
                """),
            spec("mesh_get_webroot",
                "Löst einen Webroot-Pfad auf und gibt den zugehörigen Node zurück.",
                """
                {"type":"object","required":["project","path"],
                 "properties":{
                   "project":{"type":"string"},
                   "path":{"type":"string","description":"z.B. /automobiles/delorean"},
                   "language":{"type":"string","default":"de"}
                 }}
                """),
            spec("mesh_run_graphql",
                "Führt eine read-only GraphQL-Query aus. Mutations werden blockiert.",
                """
                {"type":"object","required":["project","query"],
                 "properties":{
                   "project":{"type":"string"},
                   "query":{"type":"string"},
                   "variables":{"type":"object"}
                 }}
                """)
        );
    }

    private static ToolSpec spec(String name, String description, String schemaJson) {
        try {
            JsonNode node = MAPPER.readTree(schemaJson.strip());
            return new ToolSpec(name, description, node, schemaJson.strip());
        } catch (Exception e) {
            throw new RuntimeException("Invalid schema for tool '" + name + "': " + e.getMessage(), e);
        }
    }

    // ── argument helpers (mirror the previous private helpers in McpServerRunner) ──

    private static String require(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null || val.toString().isBlank()) {
            throw new AppError(ErrorCode.VALIDATION_ERROR,
                    "Pflichtfeld '" + key + "' fehlt im Tool-Input.");
        }
        return val.toString();
    }

    private static String str(Map<String, Object> args, String key, String def) {
        Object v = args.get(key);
        return v != null ? v.toString() : def;
    }

    private static int num(Map<String, Object> args, String key, int def) {
        Object v = args.get(key);
        return v instanceof Number n ? n.intValue() : def;
    }

    private static boolean bool(Map<String, Object> args, String key, boolean def) {
        Object v = args.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s)  return Boolean.parseBoolean(s);
        return def;
    }
}
