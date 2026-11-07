package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.PublishStatus;
import com.gentics.mesh.contentrag.index.RagChunkFields;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lesende Gegenrichtung zu {@code RagChunkDocuments}: Elasticsearch-{@code _source} zurueck in
 * einen {@link Chunk}. Das Vektorfeld wird nicht zurueckgelesen – fuer Anzeige, Quellenangabe
 * und LLM-Kontext wird es nicht gebraucht, und es waere der mit Abstand groesste Teil der
 * uebertragenen Antwort.
 */
final class ChunkSources {

    private ChunkSources() {
    }

    /**
     * @param source {@code _source} eines Treffers
     * @return Chunk mit den im Index vorhandenen Feldern
     */
    static Chunk fromSource(JsonNode source) {
        return new Chunk(
                text(source, RagChunkFields.CHUNK_ID),
                text(source, RagChunkFields.DOCUMENT_GROUP),
                text(source, RagChunkFields.NODE_UUID),
                text(source, RagChunkFields.PROJECT_UUID),
                text(source, RagChunkFields.PROJECT),
                text(source, RagChunkFields.BRANCH_UUID),
                text(source, RagChunkFields.BRANCH),
                text(source, RagChunkFields.LANGUAGE),
                text(source, RagChunkFields.SCHEMA),
                text(source, RagChunkFields.PATH),
                text(source, RagChunkFields.TITLE),
                text(source, RagChunkFields.CHUNK_TEXT),
                source.path(RagChunkFields.CHUNK_INDEX).asInt(0),
                text(source, RagChunkFields.FIELD_PATH),
                text(source, RagChunkFields.MESH_VERSION),
                text(source, RagChunkFields.CONTENT_HASH),
                status(text(source, RagChunkFields.PUBLISH_STATUS)),
                instant(text(source, RagChunkFields.VALID_FROM)),
                instant(text(source, RagChunkFields.VALID_TO)),
                instant(text(source, RagChunkFields.CREATED_DATE)),
                instant(text(source, RagChunkFields.EDITED_DATE)),
                list(source, RagChunkFields.ROLE_UUIDS),
                text(source, RagChunkFields.SOURCE_URL),
                list(source, RagChunkFields.REFERENCED_NODES),
                list(source, RagChunkFields.EDITORIAL_TAGS));
    }

    private static String text(JsonNode source, String field) {
        JsonNode node = source.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static List<String> list(JsonNode source, String field) {
        List<String> values = new ArrayList<>();
        JsonNode node = source.get(field);
        if (node != null && node.isArray()) {
            node.forEach(entry -> values.add(entry.asText()));
        }
        return values;
    }

    private static Instant instant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static PublishStatus status(String value) {
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
