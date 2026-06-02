package io.gentics.mesh.mcp.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import io.gentics.mesh.mcp.domain.model.*;
import io.gentics.mesh.mcp.security.RedactionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates raw Gentics Mesh JSON API responses into typed domain objects.
 *
 * <p>{@code MeshMapper} is a pure, stateless utility class (no instances,
 * no mutable state) that centralises all JSON-to-domain mapping logic.
 * Every method in this class accepts a raw {@link JsonNode} from the Mesh
 * REST API and returns a corresponding domain record.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Extracting typed scalar values from Jackson {@link JsonNode}s using safe
 *       {@code path()} / {@code asText()} / {@code asBoolean()} accessors that
 *       return sensible defaults rather than throwing on missing fields.</li>
 *   <li>Converting the dynamic Mesh {@code fields} JSON object to a
 *       {@code Map<String, Object>} via {@link #jsonToMap(JsonNode)}.</li>
 *   <li>Calling {@link RedactionService#redact(java.util.Map)} on every field map
 *       before it is embedded in a domain object, ensuring sensitive data is
 *       never propagated to the MCP tool response.</li>
 *   <li>Detecting inline node references inside field values
 *       (objects that carry both a {@code uuid} and a {@code schema} key)
 *       and collecting them as {@link NodeReference} records.</li>
 *   <li>Extracting text snippets for search results, with HTML stripping and
 *       truncation via {@link RedactionService#htmlToPlainText(String)} and
 *       {@link RedactionService#truncate(String, int)}.</li>
 * </ul>
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>All methods are {@code public static} – callers do not need an instance.</li>
 *   <li>Missing JSON fields are handled gracefully (empty string / false / empty list),
 *       so that partial API responses do not cause {@code NullPointerException}s.</li>
 *   <li>The {@code published} flag is determined from two possible locations in the
 *       Mesh response: the top-level {@code status} field and the per-language
 *       {@code availableLanguages[lang].published} flag.</li>
 * </ul>
 *
 * @see RedactionService
 * @see MeshNode
 * @see SearchResult
 * @since 0.1.0
 */
public final class MeshMapper {

    /** Utility class – do not instantiate. */
    private MeshMapper() {}

    // ── Project ───────────────────────────────────────────────────────────

    /**
     * Maps a raw project JSON node from the Mesh {@code /api/v2/projects} endpoint
     * to a {@link Project} domain record.
     *
     * @param raw one element of the {@code data} array in the projects list response
     * @return a {@link Project} with {@code name} and {@code uuid} extracted;
     *         empty strings are used as defaults for missing fields
     */
    public static Project mapProject(JsonNode raw) {
        return new Project(
                raw.path("name").asText(""),
                raw.path("uuid").asText("")
        );
    }

    // ── Schema ────────────────────────────────────────────────────────────

    /**
     * Maps a raw schema JSON node from the Mesh {@code /api/v2/{project}/schemas}
     * endpoint to a {@link MeshSchema} domain record.
     *
     * <p>Iterates the {@code fields} array within the schema definition and maps
     * each element to a {@link SchemaField} carrying {@code name}, {@code type},
     * and {@code required} attributes. Missing or non-array {@code fields} nodes
     * result in an empty list.
     *
     * @param raw one element of the {@code data} array in the schemas list response
     * @return a {@link MeshSchema} with its field definitions
     */
    public static MeshSchema mapSchema(JsonNode raw) {
        List<SchemaField> fields = new ArrayList<>();
        JsonNode fieldArray = raw.path("fields");
        if (fieldArray.isArray()) {
            for (JsonNode f : fieldArray) {
                fields.add(new SchemaField(
                        f.path("name").asText(""),
                        f.path("type").asText("string"),
                        f.path("required").asBoolean(false)
                ));
            }
        }
        return new MeshSchema(
                raw.path("uuid").asText(""),
                raw.path("name").asText(""),
                fields
        );
    }

    // ── Node ──────────────────────────────────────────────────────────────

    /**
     * Maps a raw node JSON response from {@code /api/v2/{project}/nodes/{uuid}}
     * to a fully populated {@link MeshNode} domain record.
     *
     * <p>The method:
     * <ol>
     *   <li>Converts the {@code fields} JSON object to a {@code Map<String, Object>}.</li>
     *   <li>Passes the raw field map through {@link RedactionService#redact(java.util.Map)}.</li>
     *   <li>Extracts inline {@link NodeReference}s by scanning for field values that
     *       are objects with both {@code uuid} and {@code schema} keys.</li>
     *   <li>Resolves the {@code published} flag from either {@code status == "published"}
     *       or {@code availableLanguages[lang].published == true}.</li>
     * </ol>
     *
     * @param raw     full node JSON as returned by the Mesh nodes endpoint
     * @param project project name (not present in the node response body itself)
     * @return a normalised {@link MeshNode} with sensitive fields redacted
     */
    public static MeshNode mapNode(JsonNode raw, String project) {
        String language = raw.path("language").asText("de");
        Map<String, Object> rawFields = jsonToMap(raw.path("fields"));
        Map<String, Object> redacted  = RedactionService.redact(rawFields);
        List<NodeReference> refs      = extractReferences(raw.path("fields"), rawFields);

        return new MeshNode(
                raw.path("uuid").asText(""),
                project,
                language,
                raw.path("schema").path("name").asText(""),
                raw.path("displayName").asText(""),
                pathFromNode(raw),
                "published".equalsIgnoreCase(raw.path("status").asText("")) ||
                        raw.path("availableLanguages").path(language).path("published").asBoolean(false),
                raw.path("version").asText(""),
                redacted,
                refs
        );
    }

    // ── Search result ─────────────────────────────────────────────────────

    /**
     * Maps a raw node hit from the Mesh Elasticsearch search response to a
     * lightweight {@link SearchResult} record suitable for LLM consumption.
     *
     * <p>A text snippet is extracted from the node's {@code fields} object by
     * checking the {@code snippetFields} list in order, falling back to the first
     * non-blank string field if none of the preferred keys are present. The snippet
     * is HTML-stripped and truncated to 300 characters.
     *
     * @param raw           one element from the search response {@code data} array
     * @param snippetFields ordered list of preferred field names for the snippet;
     *                      typically {@code ["title", "teaser", "body"]}
     * @return a {@link SearchResult} with title, path, schema, snippet, and publish status
     */
    public static SearchResult mapSearchResult(JsonNode raw, List<String> snippetFields) {
        Map<String, Object> fields = jsonToMap(raw.path("fields"));
        String snippet = extractSnippet(fields, snippetFields);

        return new SearchResult(
                raw.path("uuid").asText(""),
                raw.path("displayName").asText(""),
                raw.path("schema").path("name").asText(""),
                raw.path("language").asText("de"),
                pathFromNode(raw),
                snippet,
                raw.path("availableLanguages").fields().hasNext()
        );
    }

    // ── Webroot ───────────────────────────────────────────────────────────

    /**
     * Maps a webroot response to a {@link MeshNode} domain record.
     *
     * <p>The webroot API endpoint returns the same JSON structure as the nodes
     * endpoint. The main difference is that the caller already knows the path
     * (from the request URL), so it is passed in explicitly rather than being
     * extracted from the response body. Nodes resolved via webroot are considered
     * published by definition (the webroot API only resolves published nodes).
     *
     * @param raw     full JSON response from {@code /api/v2/{project}/webroot/{path}}
     * @param path    the normalised webroot path that was requested (e.g. {@code "/news/article"})
     * @param project allowlisted project name
     * @return a normalised {@link MeshNode} with {@code published = true} and
     *         an empty {@link NodeReference} list
     */
    public static MeshNode mapWebrootNode(JsonNode raw, String path, String project) {
        Map<String, Object> redacted = RedactionService.redact(jsonToMap(raw.path("fields")));
        return new MeshNode(
                raw.path("uuid").asText(""),
                project,
                raw.path("language").asText("de"),
                raw.path("schema").path("name").asText(""),
                raw.path("displayName").asText(""),
                path,
                true,
                raw.path("version").asText(""),
                redacted,
                List.of()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Converts a JSON object node to a {@code Map<String, Object>} using recursive
     * type inference for nested objects and arrays.
     *
     * <p>Returns an empty, immutable map if the input is {@code null}, missing,
     * or not a JSON object.
     *
     * @param node the JSON object node to convert
     * @return a mutable {@link java.util.LinkedHashMap} preserving insertion order,
     *         or an empty immutable map for non-object inputs
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> jsonToMap(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isObject()) {
            return Map.of();
        }
        var result = new LinkedHashMap<String, Object>();
        node.fields().forEachRemaining(e -> result.put(e.getKey(), jsonToValue(e.getValue())));
        return result;
    }

    /**
     * Converts a single {@link JsonNode} to a Java value using the following mapping:
     * <ul>
     *   <li>{@code null} → {@code null}</li>
     *   <li>boolean → {@link Boolean}</li>
     *   <li>integral number → {@link Long}</li>
     *   <li>floating-point number → {@link Double}</li>
     *   <li>object → recursive {@code Map<String, Object>}</li>
     *   <li>array → {@link java.util.ArrayList} of recursively converted values</li>
     *   <li>everything else → {@link String} via {@code asText()}</li>
     * </ul>
     *
     * @param node the JSON node to convert
     * @return the corresponding Java value, or {@code null} for JSON null
     */
    private static Object jsonToValue(JsonNode node) {
        if (node.isNull())    return null;
        if (node.isBoolean()) return node.asBoolean();
        if (node.isNumber())  return node.isIntegralNumber() ? node.asLong() : node.asDouble();
        if (node.isObject())  return jsonToMap(node);
        if (node.isArray()) {
            var list = new ArrayList<>();
            node.forEach(n -> list.add(jsonToValue(n)));
            return list;
        }
        return node.asText();
    }

    /**
     * Scans the raw {@code fields} JSON object for inline node references and
     * returns them as a list of {@link NodeReference} records.
     *
     * <p>A field value is considered a node reference when it is a JSON object
     * that contains <em>both</em> a {@code uuid} key and a {@code schema} key.
     * This heuristic matches the Mesh node-reference field type without requiring
     * schema-type metadata at mapping time.
     *
     * @param fieldsNode the raw {@code fields} JSON object from the node response
     * @param fields     pre-converted field map (unused; reserved for future use)
     * @return list of detected {@link NodeReference}s; empty list if none found
     */
    private static List<NodeReference> extractReferences(JsonNode fieldsNode,
                                                          Map<String, Object> fields) {
        var refs = new ArrayList<NodeReference>();
        if (fieldsNode == null || !fieldsNode.isObject()) return refs;

        fieldsNode.fields().forEachRemaining(e -> {
            JsonNode val = e.getValue();
            // node reference: has "uuid" and "schema" sibling
            if (val.isObject() && val.has("uuid") && val.has("schema")) {
                refs.add(new NodeReference(e.getKey(), val.path("uuid").asText()));
            }
        });
        return refs;
    }

    /**
     * Extracts the canonical URL path for a node from the Mesh response.
     *
     * <p>Tries two locations in order:
     * <ol>
     *   <li>Top-level {@code path} field (present when the node was resolved via webroot).</li>
     *   <li>Last element of the {@code breadcrumb} array → {@code path} (present in
     *       standard node responses when breadcrumb is requested).</li>
     * </ol>
     * Returns an empty string if neither location yields a value.
     *
     * @param node the raw JSON node response
     * @return the path string, or {@code ""} if not determinable
     */
    private static String pathFromNode(JsonNode node) {
        JsonNode pathNode = node.path("path");
        if (!pathNode.isMissingNode()) return pathNode.asText("");
        JsonNode breadcrumb = node.path("breadcrumb");
        if (breadcrumb.isArray() && !breadcrumb.isEmpty()) {
            return breadcrumb.path(breadcrumb.size() - 1).path("path").asText("");
        }
        return "";
    }

    /**
     * Extracts a plain-text snippet from a node's field map for use in search results.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Iterate {@code preferredKeys} in order; return the first non-blank
     *       {@link String} value found, after HTML stripping and truncation.</li>
     *   <li>If none of the preferred keys matches, fall back to the first
     *       non-blank {@link String} value in the map, regardless of key name.</li>
     *   <li>If the map contains no suitable string value, return {@code ""}.</li>
     * </ol>
     *
     * @param fields        redacted field map of the node
     * @param preferredKeys ordered list of field names to prefer for the snippet
     * @return a plain-text snippet of at most 300 characters, or {@code ""}
     */
    private static String extractSnippet(Map<String, Object> fields, List<String> preferredKeys) {
        for (String key : preferredKeys) {
            Object val = fields.get(key);
            if (val instanceof String s && !s.isBlank()) {
                return RedactionService.truncate(
                        RedactionService.htmlToPlainText(s), 300);
            }
        }
        // Fallback: first non-blank string field
        for (var entry : fields.entrySet()) {
            if (entry.getValue() instanceof String s && !s.isBlank()) {
                return RedactionService.truncate(
                        RedactionService.htmlToPlainText(s), 300);
            }
        }
        return "";
    }
}
