package com.gentics.mesh.contentrag.index;

import com.gentics.mesh.contentrag.common.model.Chunk;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abbildung {@link Chunk} auf das Elasticsearch-Quelldokument sowie die Hilfsabfragen, die die
 * schreibende Seite fuer Group-Delete-and-Replace braucht.
 *
 * <p>Rueckgabe als {@code Map}/{@code List}, damit dieses Modul ohne JSON-Bibliothek auskommt:
 * die Mesh-Seite serialisiert mit ihrem Jackson, der RAG-Service mit seinem. Ein gemeinsames
 * Modul, das eine Fremdbibliothek in den CMS-Classpath zieht, waere genau die Kopplung, die
 * wir vermeiden wollen.
 */
public final class RagChunkDocuments {

    private RagChunkDocuments() {
    }

    /**
     * Elasticsearch-Dokument-ID eines Chunks.
     *
     * @param chunk Chunk
     * @return deterministische ID; gleiche Struktur ergibt dieselbe ID
     */
    public static String documentId(Chunk chunk) {
        return chunk.chunkId();
    }

    /**
     * Baut das Quelldokument. Das Vektorfeld bleibt leer – es wird von der Ingest-Pipeline
     * ergaenzt (siehe {@link RagInferenceSetup}).
     *
     * @param chunk Chunk
     * @return Quelldokument in Feldreihenfolge des Mappings
     */
    public static Map<String, Object> toSource(Chunk chunk) {
        Map<String, Object> doc = new LinkedHashMap<>();
        put(doc, RagChunkFields.CHUNK_ID, chunk.chunkId());
        put(doc, RagChunkFields.DOCUMENT_GROUP, chunk.documentGroup());
        put(doc, RagChunkFields.NODE_UUID, chunk.nodeUuid());
        put(doc, RagChunkFields.PROJECT_UUID, chunk.projectUuid());
        put(doc, RagChunkFields.PROJECT, chunk.project());
        put(doc, RagChunkFields.BRANCH_UUID, chunk.branchUuid());
        put(doc, RagChunkFields.BRANCH, chunk.branch());
        put(doc, RagChunkFields.LANGUAGE, chunk.language());
        put(doc, RagChunkFields.SCHEMA, chunk.schema());
        put(doc, RagChunkFields.PATH, chunk.path());
        put(doc, RagChunkFields.TITLE, chunk.title());
        put(doc, RagChunkFields.CHUNK_TEXT, chunk.chunkText());
        doc.put(RagChunkFields.CHUNK_INDEX, chunk.chunkIndex());
        put(doc, RagChunkFields.FIELD_PATH, chunk.fieldPath());
        put(doc, RagChunkFields.MESH_VERSION, chunk.meshVersion());
        put(doc, RagChunkFields.CONTENT_HASH, chunk.contentHash());
        if (chunk.publishStatus() != null) {
            doc.put(RagChunkFields.PUBLISH_STATUS, chunk.publishStatus().name().toLowerCase());
        }
        putInstant(doc, RagChunkFields.VALID_FROM, chunk.validFrom());
        putInstant(doc, RagChunkFields.VALID_TO, chunk.validTo());
        putInstant(doc, RagChunkFields.CREATED_DATE, chunk.createdDate());
        putInstant(doc, RagChunkFields.EDITED_DATE, chunk.editedDate());
        // Als einzige Liste immer geschrieben, auch leer - so wie Mesh es im Node-Dokument
        // tut. Sonst sehen "keine Rolle hat Leserecht" und "das Feld wurde nie geschrieben"
        // im Dokument gleich aus, und die Unterscheidung kostet eine Fehlersuche.
        doc.put(RagChunkFields.ROLE_UUIDS, List.copyOf(chunk.roleUuids()));
        put(doc, RagChunkFields.SOURCE_URL, chunk.sourceUrl());
        putList(doc, RagChunkFields.REFERENCED_NODES, chunk.referencedNodes());
        putList(doc, RagChunkFields.EDITORIAL_TAGS, chunk.editorialTags());
        return doc;
    }

    /**
     * Query, die alle Chunks einer Document-Group trifft – fuer den Loeschfall (Node geloescht,
     * depubliziert oder per No-RAG-Konvention ausgeschlossen).
     *
     * @param documentGroupKey {@code nodeUuid:branch:language}
     * @return Term-Query auf {@link RagChunkFields#DOCUMENT_GROUP}
     */
    public static Map<String, Object> documentGroupQuery(String documentGroupKey) {
        return Map.of("term", Map.of(RagChunkFields.DOCUMENT_GROUP, documentGroupKey));
    }

    /**
     * Query, die <em>ueberzaehlige</em> Chunks einer Document-Group trifft: alle mit einem Index
     * ab {@code chunkCount}.
     *
     * <p>Damit kommt der Schreiber beim Aktualisieren ohne vorheriges Delete-by-Query aus. Er
     * indexiert die aktuellen Chunks per Bulk-Upsert – die deterministischen IDs ueberschreiben
     * die alten Dokumente – und raeumt nur den Rest weg, falls der Node kuerzer geworden ist.
     * Das spart pro Aktualisierung eine Suche samt Loeschung, was bei einem Voll-Sync ueber
     * Millionen Nodes den Unterschied macht.
     *
     * @param documentGroupKey {@code nodeUuid:branch:language}
     * @param chunkCount       Anzahl der jetzt gueltigen Chunks
     * @return Bool-Query auf Document-Group und Chunk-Index
     */
    public static Map<String, Object> staleChunksQuery(String documentGroupKey, int chunkCount) {
        Map<String, Object> range = Map.of("range",
                Map.of(RagChunkFields.CHUNK_INDEX, Map.of("gte", chunkCount)));
        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", List.of(documentGroupQuery(documentGroupKey), range));
        return Map.of("bool", bool);
    }

    /**
     * Query, die alle Chunks eines Nodes trifft – unabhaengig von der Sprache.
     *
     * <p>Fuer den Loeschfall, in dem die Sprachversionen nicht mehr ermittelbar sind (geloeschter
     * Node). Der Branch muss die Query nicht einschraenken: die RAG-Indizes sind bereits nach
     * Projekt und Branch getrennt.
     *
     * @param nodeUuid UUID des Nodes
     * @return Term-Query auf {@link RagChunkFields#NODE_UUID}
     */
    public static Map<String, Object> nodeQuery(String nodeUuid) {
        return Map.of("term", Map.of(RagChunkFields.NODE_UUID, nodeUuid));
    }

    /**
     * Query, die alle Chunks einer Sprachversion eines Nodes trifft – etwa beim Depublizieren
     * einer einzelnen Sprache.
     *
     * @param nodeUuid UUID des Nodes
     * @param language Sprachcode
     * @return Bool-Query auf Node und Sprache
     */
    public static Map<String, Object> nodeLanguageQuery(String nodeUuid, String language) {
        Map<String, Object> languageTerm = Map.of("term", Map.of(RagChunkFields.LANGUAGE, language));
        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("filter", List.of(nodeQuery(nodeUuid), languageTerm));
        return Map.of("bool", bool);
    }

    private static void put(Map<String, Object> doc, String field, String value) {
        if (value != null && !value.isEmpty()) {
            doc.put(field, value);
        }
    }

    private static void putInstant(Map<String, Object> doc, String field, Instant value) {
        if (value != null) {
            doc.put(field, value.toString());
        }
    }

    private static void putList(Map<String, Object> doc, String field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            doc.put(field, List.copyOf(values));
        }
    }
}
