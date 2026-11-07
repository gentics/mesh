package io.gentics.mesh.mcp.mesh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gentics.mesh.mcp.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MeshMapper}.
 *
 * <p>All mapper methods are {@code static}; no mocking required.
 * Jackson {@link ObjectMapper} is used to build JSON input nodes.
 */
class MeshMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ══════════════════════════════════════════════════════════════════════
    // mapProject()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void mapProject_extractsNameAndUuid() throws Exception {
        ObjectNode raw = MAPPER.createObjectNode();
        raw.put("name", "demo");
        raw.put("uuid", "abc-123");

        Project p = MeshMapper.mapProject(raw);

        assertEquals("demo",    p.name());
        assertEquals("abc-123", p.uuid());
    }

    @Test
    void mapProject_missingFields_defaultToEmpty() throws Exception {
        ObjectNode raw = MAPPER.createObjectNode();
        Project p = MeshMapper.mapProject(raw);
        assertEquals("", p.name());
        assertEquals("", p.uuid());
    }

    // ══════════════════════════════════════════════════════════════════════
    // mapSchema()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void mapSchema_extractsUuidNameAndFields() throws Exception {
        String json = """
            {
              "uuid": "schema-uuid",
              "name": "article",
              "fields": [
                { "name": "title", "type": "string", "required": true  },
                { "name": "body",  "type": "html",   "required": false }
              ]
            }
            """;
        MeshSchema schema = MeshMapper.mapSchema(MAPPER.readTree(json));

        assertEquals("schema-uuid", schema.uuid());
        assertEquals("article",     schema.name());
        assertEquals(2, schema.fields().size());

        SchemaField title = schema.fields().get(0);
        assertEquals("title",  title.name());
        assertEquals("string", title.type());
        assertTrue(title.required(), "title field must be marked required");

        SchemaField body = schema.fields().get(1);
        assertEquals("body", body.name());
        assertEquals("html", body.type());
        assertFalse(body.required(), "body field must not be marked required");
    }

    @Test
    void mapSchema_missingFieldsArray_returnsEmptyList() throws Exception {
        ObjectNode raw = MAPPER.createObjectNode();
        raw.put("uuid", "u");
        raw.put("name", "n");
        MeshSchema schema = MeshMapper.mapSchema(raw);
        assertTrue(schema.fields().isEmpty(),
                "fields list must be empty when the JSON has no 'fields' array");
    }

    // ══════════════════════════════════════════════════════════════════════
    // mapNode()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void mapNode_extractsCoreFields() throws Exception {
        String json = """
            {
              "uuid": "node-uuid",
              "language": "de",
              "schema": { "name": "article" },
              "displayName": "Hello World",
              "path": "/news/hello",
              "status": "published",
              "version": "1.0",
              "fields": { "title": "Hello", "body": "World" }
            }
            """;
        MeshNode node = MeshMapper.mapNode(MAPPER.readTree(json), "demo");

        assertEquals("node-uuid",  node.uuid());
        assertEquals("demo",       node.project());
        assertEquals("de",         node.language());
        assertEquals("article",    node.schema());
        assertEquals("Hello World",node.displayName());
        assertEquals("/news/hello", node.path());
        assertTrue(node.published(), "node with status=published must have published=true");
        assertEquals("1.0",        node.version());
    }

    @Test
    void mapNode_redactsSensitiveFields() throws Exception {
        String json = """
            {
              "uuid": "x", "language": "de",
              "schema": { "name": "a" }, "displayName": "d", "version": "1.0",
              "fields": {
                "title": "safe",
                "password": "super-secret",
                "token": "bearer-abc"
              }
            }
            """;
        MeshNode node = MeshMapper.mapNode(MAPPER.readTree(json), "demo");
        assertEquals("safe",        node.fields().get("title"));
        assertEquals("[REDACTED]",  node.fields().get("password"));
        assertEquals("[REDACTED]",  node.fields().get("token"));
    }

    @Test
    void mapNode_publishedViaStatus() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        raw.put("status", "published");
        assertTrue(MeshMapper.mapNode(raw, "demo").published(),
                "status=published must yield published=true");
    }

    @Test
    void mapNode_publishedViaAvailableLanguages() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        raw.put("status", "draft");
        var avail = raw.putObject("availableLanguages").putObject("de");
        avail.put("published", true);
        assertTrue(MeshMapper.mapNode(raw, "demo").published(),
                "availableLanguages[lang].published=true must yield published=true even when status=draft");
    }

    @Test
    void mapNode_notPublished_returnsFalse() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        raw.put("status", "draft");
        assertFalse(MeshMapper.mapNode(raw, "demo").published(),
                "status=draft with no availableLanguages flag must yield published=false");
    }

    @Test
    void mapNode_extractsNodeReferences() throws Exception {
        String json = """
            {
              "uuid": "n", "language": "de",
              "schema": { "name": "a" }, "displayName": "d", "version": "1.0",
              "fields": {
                "author": { "uuid": "author-uuid", "schema": { "name": "person" } },
                "title":  "just a string"
              }
            }
            """;
        MeshNode node = MeshMapper.mapNode(MAPPER.readTree(json), "demo");
        assertEquals(1, node.references().size());
        NodeReference ref = node.references().get(0);
        assertEquals("author",      ref.field());
        assertEquals("author-uuid", ref.uuid());
    }

    @Test
    void mapNode_noReferences_returnsEmptyList() throws Exception {
        ObjectNode raw = buildMinimalNode("en");
        MeshNode node = MeshMapper.mapNode(raw, "demo");
        assertTrue(node.references().isEmpty(),
                "references must be empty when fields contain no node-reference objects");
    }

    @Test
    void mapNode_pathFromBreadcrumb_whenNoTopLevelPath() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        var breadcrumb = raw.putArray("breadcrumb");
        var last = MAPPER.createObjectNode();
        last.put("path", "/breadcrumb-path");
        breadcrumb.add(last);

        MeshNode node = MeshMapper.mapNode(raw, "demo");
        assertEquals("/breadcrumb-path", node.path());
    }

    // ══════════════════════════════════════════════════════════════════════
    // mapSearchResult()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void mapSearchResult_extractsBasicFields() throws Exception {
        String json = """
            {
              "uuid": "sr-uuid",
              "displayName": "My Article",
              "schema": { "name": "article" },
              "language": "de",
              "path": "/articles/my-article",
              "availableLanguages": { "de": {}, "en": {} },
              "fields": { "title": "My Article", "teaser": "Short intro text" }
            }
            """;
        SearchResult sr = MeshMapper.mapSearchResult(
                MAPPER.readTree(json), List.of("title", "teaser", "body"));

        assertEquals("sr-uuid",              sr.uuid());
        assertEquals("My Article",           sr.title());
        assertEquals("article",              sr.schema());
        assertEquals("de",                   sr.language());
        assertEquals("/articles/my-article", sr.path());
        assertTrue(sr.published(), "published must be true when availableLanguages has entries");
    }

    @Test
    void mapSearchResult_snippetFromPreferredField() throws Exception {
        String json = """
            {
              "uuid": "x", "displayName": "D",
              "schema": {"name": "a"}, "language": "de",
              "fields": {
                "body":   "This is the body text",
                "teaser": "This is the teaser",
                "title":  "This is the title"
              }
            }
            """;
        SearchResult sr = MeshMapper.mapSearchResult(
                MAPPER.readTree(json), List.of("teaser", "body"));

        // "teaser" is first in preference, so it should be used
        assertEquals("This is the teaser", sr.snippet());
    }

    @Test
    void mapSearchResult_snippetFallsBackToFirstStringField() throws Exception {
        String json = """
            {
              "uuid": "x", "displayName": "D",
              "schema": {"name": "a"}, "language": "de",
              "fields": { "content": "fallback content" }
            }
            """;
        SearchResult sr = MeshMapper.mapSearchResult(
                MAPPER.readTree(json), List.of("title", "teaser"));

        assertEquals("fallback content", sr.snippet());
    }

    @Test
    void mapSearchResult_snippetStripsHtml() throws Exception {
        String json = """
            {
              "uuid": "x", "displayName": "D",
              "schema": {"name": "a"}, "language": "de",
              "fields": { "body": "<p>Plain text</p>" }
            }
            """;
        SearchResult sr = MeshMapper.mapSearchResult(
                MAPPER.readTree(json), List.of("body"));

        assertEquals("Plain text", sr.snippet());
    }

    @Test
    void mapSearchResult_snippetTruncatedTo300() throws Exception {
        String longText = "x".repeat(400);
        String json = String.format("""
            {
              "uuid": "x", "displayName": "D",
              "schema": {"name": "a"}, "language": "de",
              "fields": { "body": "%s" }
            }
            """, longText);
        SearchResult sr = MeshMapper.mapSearchResult(
                MAPPER.readTree(json), List.of("body"));

        // 300 chars + ellipsis character
        assertEquals(301, sr.snippet().length(),
                "Snippet must be truncated to 300 chars + 1 ellipsis = 301 total");
        assertTrue(sr.snippet().endsWith("…"),
                "Truncated snippet must end with the ellipsis character '…'");
    }

    // ══════════════════════════════════════════════════════════════════════
    // mapWebrootNode()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void mapWebrootNode_isAlwaysPublished() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        raw.put("status", "draft");
        MeshNode node = MeshMapper.mapWebrootNode(raw, "/my/path", "demo");
        assertTrue(node.published(), "Webroot nodes are always published");
    }

    @Test
    void mapWebrootNode_usesProvidedPath() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        MeshNode node = MeshMapper.mapWebrootNode(raw, "/explicit/path", "demo");
        assertEquals("/explicit/path", node.path());
    }

    @Test
    void mapWebrootNode_hasEmptyReferenceList() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        MeshNode node = MeshMapper.mapWebrootNode(raw, "/path", "demo");
        assertTrue(node.references().isEmpty(),
                "mapWebrootNode() must always produce an empty references list");
    }

    @Test
    void mapWebrootNode_redactsSensitiveFields() throws Exception {
        ObjectNode raw = buildMinimalNode("de");
        ((ObjectNode) raw.get("fields")).put("secret", "hidden");
        MeshNode node = MeshMapper.mapWebrootNode(raw, "/p", "demo");
        assertEquals("[REDACTED]", node.fields().get("secret"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Builds a minimal valid node JSON with an empty fields object. */
    private static ObjectNode buildMinimalNode(String language) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("uuid",        "test-uuid");
        node.put("language",    language);
        node.put("displayName", "Test Node");
        node.put("version",     "1.0");
        node.putObject("schema").put("name", "article");
        node.putObject("fields");
        return node;
    }
}
