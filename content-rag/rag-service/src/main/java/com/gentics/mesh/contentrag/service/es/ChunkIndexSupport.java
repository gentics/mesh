package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.PublishStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Gemeinsame Helfer fuer Index-Benennung, Mapping und die Konvertierung Chunk &lt;-&gt; ES-Dokument
 * (Konzept 5.6/6). Das Mapping bildet das dokumentierte Mesh-Permission-Modell nach:
 * {@code readRoleUuids} als keyword-Array, analog zu Mesh' {@code _roleUuids} (K3).
 */
public final class ChunkIndexSupport {

    private ChunkIndexSupport() {
    }

    /**
     * Bildet den Chunk-Indexnamen fuer ein Projekt (Topologie Index-pro-Projekt, V3).
     *
     * @param prefix      konfigurierter Index-Praefix
     * @param projectUuid Mandant
     * @return kleingeschriebener, gueltiger Elasticsearch-Indexname
     */
    public static String indexName(String prefix, String projectUuid) {
        return (prefix + "-chunk-" + projectUuid).toLowerCase();
    }

    /**
     * Liefert den Wildcard-Ausdruck ueber alle Chunk-Indizes (projektuebergreifend).
     *
     * @param prefix konfigurierter Index-Praefix
     * @return Wildcard-Index-Ausdruck
     */
    public static String wildcard(String prefix) {
        return (prefix + "-chunk-*").toLowerCase();
    }

    /** Erzeugt das Index-Mapping inkl. dense_vector mit der gegebenen Dimension. */
    public static ObjectNode buildMapping(ObjectMapper m, int dimension) {
        ObjectNode root = m.createObjectNode();
        ObjectNode mappings = root.putObject("mappings");
        ObjectNode props = mappings.putObject("properties");

        keyword(m, props, "chunkId");
        keyword(m, props, "documentGroup");
        keyword(m, props, "nodeUuid");
        keyword(m, props, "projectUuid");
        keyword(m, props, "project");
        keyword(m, props, "branch");
        keyword(m, props, "language");
        keyword(m, props, "schema");
        keyword(m, props, "path");
        textWithRaw(m, props, "title");
        props.putObject("chunkText").put("type", "text");
        props.putObject("chunkIndex").put("type", "integer");
        keyword(m, props, "fieldPath");

        ObjectNode emb = props.putObject("embedding");
        emb.put("type", "dense_vector");
        emb.put("dims", dimension);
        emb.put("index", true);
        emb.put("similarity", "cosine");

        keyword(m, props, "embeddingModel");
        props.putObject("embeddingDim").put("type", "integer");
        keyword(m, props, "meshVersion");
        keyword(m, props, "contentHash");
        keyword(m, props, "publishStatus");
        date(m, props, "validFrom");
        date(m, props, "validTo");
        date(m, props, "createdDate");
        date(m, props, "editedDate");
        keyword(m, props, "readRoleUuids");
        keyword(m, props, "sourceUrl");
        keyword(m, props, "referencedNodes");
        keyword(m, props, "editorialTags");
        return root;
    }

    /** Chunk -&gt; ES-Dokument (ObjectNode) inklusive Feld {@code _id}. */
    public static ObjectNode toDocument(ObjectMapper m, Chunk c) {
        ObjectNode d = m.createObjectNode();
        d.put("_id", c.chunkId());
        d.put("chunkId", c.chunkId());
        d.put("documentGroup", c.documentGroup());
        d.put("nodeUuid", c.nodeUuid());
        putIf(d, "projectUuid", c.projectUuid());
        putIf(d, "project", c.project());
        putIf(d, "branch", c.branch());
        putIf(d, "language", c.language());
        putIf(d, "schema", c.schema());
        putIf(d, "path", c.path());
        putIf(d, "title", c.title());
        putIf(d, "chunkText", c.chunkText());
        d.put("chunkIndex", c.chunkIndex());
        putIf(d, "fieldPath", c.fieldPath());

        if (c.hasEmbedding()) {
            ArrayNode vec = d.putArray("embedding");
            for (float f : c.embedding()) {
                vec.add(f);
            }
        }
        putIf(d, "embeddingModel", c.embeddingModel());
        if (c.embeddingDim() != null) {
            d.put("embeddingDim", c.embeddingDim());
        }
        putIf(d, "meshVersion", c.meshVersion());
        putIf(d, "contentHash", c.contentHash());
        if (c.publishStatus() != null) {
            d.put("publishStatus", c.publishStatus().name().toLowerCase());
        }
        putInstant(d, "validFrom", c.validFrom());
        putInstant(d, "validTo", c.validTo());
        putInstant(d, "createdDate", c.createdDate());
        putInstant(d, "editedDate", c.editedDate());
        putArray(d, "readRoleUuids", c.readRoleUuids());
        putIf(d, "sourceUrl", c.sourceUrl());
        putArray(d, "referencedNodes", c.referencedNodes());
        putArray(d, "editorialTags", c.editorialTags());
        return d;
    }

    /** ES-_source -&gt; Chunk. Das Embedding wird nicht zurueckgelesen (fuer Retrieval nicht noetig). */
    public static Chunk fromSource(JsonNode s) {
        return new Chunk(
                text(s, "chunkId"),
                text(s, "documentGroup"),
                text(s, "nodeUuid"),
                text(s, "projectUuid"),
                text(s, "project"),
                text(s, "branch"),
                text(s, "language"),
                text(s, "schema"),
                text(s, "path"),
                text(s, "title"),
                text(s, "chunkText"),
                s.path("chunkIndex").asInt(0),
                text(s, "fieldPath"),
                null,
                text(s, "embeddingModel"),
                s.has("embeddingDim") ? s.get("embeddingDim").asInt() : null,
                text(s, "meshVersion"),
                text(s, "contentHash"),
                parseStatus(text(s, "publishStatus")),
                parseInstant(text(s, "validFrom")),
                parseInstant(text(s, "validTo")),
                parseInstant(text(s, "createdDate")),
                parseInstant(text(s, "editedDate")),
                list(s, "readRoleUuids"),
                text(s, "sourceUrl"),
                list(s, "referencedNodes"),
                list(s, "editorialTags"));
    }

    // --- Helfer ---

    private static void keyword(ObjectMapper m, ObjectNode props, String name) {
        props.putObject(name).put("type", "keyword");
    }

    private static void date(ObjectMapper m, ObjectNode props, String name) {
        props.putObject(name).put("type", "date");
    }

    private static void textWithRaw(ObjectMapper m, ObjectNode props, String name) {
        ObjectNode f = props.putObject(name);
        f.put("type", "text");
        f.putObject("fields").putObject("raw").put("type", "keyword");
    }

    private static void putIf(ObjectNode d, String field, String value) {
        if (value != null) {
            d.put(field, value);
        }
    }

    private static void putInstant(ObjectNode d, String field, Instant value) {
        if (value != null) {
            d.put(field, value.toString());
        }
    }

    private static void putArray(ObjectNode d, String field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            ArrayNode arr = d.putArray(field);
            values.forEach(arr::add);
        }
    }

    private static String text(JsonNode s, String field) {
        JsonNode n = s.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static List<String> list(JsonNode s, String field) {
        List<String> out = new ArrayList<>();
        JsonNode n = s.get(field);
        if (n != null && n.isArray()) {
            n.forEach(e -> out.add(e.asText()));
        }
        return out;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static PublishStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return PublishStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
