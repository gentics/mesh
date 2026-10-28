package com.gentics.mesh.contentrag.index;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapping und Settings der RAG-Chunk-Indizes.
 *
 * <p>Ausgeliefert wird das Ganze als <strong>Composable Index Template</strong>. Damit legt die
 * schreibende Mesh-Seite ihre Indizes so an, wie sie es fuer die Node-Indizes ohnehin tut –
 * ohne Mapping-Wissen im Sync-Pfad: das Template greift automatisch, sobald ein Indexname auf
 * das Pattern passt, und bringt auch die Ingest-Pipeline ueber
 * {@code index.default_pipeline} mit.
 */
public final class RagIndexTemplate {

    private RagIndexTemplate() {
    }

    /**
     * Body fuer {@code PUT _index_template/<name>}.
     *
     * @param prefix      Installations-Praefix
     * @param pipelineId  ID der Ingest-Pipeline, die den Vektor erzeugt
     * @param dimension   Vektordimension des Embedding-Modells
     * @param shards      Anzahl Primaer-Shards je Index
     * @param replicas    Anzahl Replicas je Index
     * @return Template-Definition
     */
    public static Map<String, Object> indexTemplate(String prefix, String pipelineId, int dimension,
                                                    int shards, int replicas) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("settings", settings(pipelineId, shards, replicas));
        template.put("mappings", mappings(dimension));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("index_patterns", List.of(RagIndexNaming.templatePattern(prefix)));
        body.put("template", template);
        body.put("priority", 100);
        body.put("_meta", Map.of("source", "gentics-mesh-content-rag"));
        return body;
    }

    /**
     * Index-Settings.
     *
     * @param pipelineId ID der Ingest-Pipeline
     * @param shards     Anzahl Primaer-Shards
     * @param replicas   Anzahl Replicas
     * @return Settings-Block
     */
    public static Map<String, Object> settings(String pipelineId, int shards, int replicas) {
        Map<String, Object> index = new LinkedHashMap<>();
        index.put("number_of_shards", shards);
        index.put("number_of_replicas", replicas);
        index.put("default_pipeline", pipelineId);
        return Map.of("index", index);
    }

    /**
     * Mapping der Chunk-Dokumente.
     *
     * <p>Das Vektorfeld nutzt {@code int8_hnsw}: die skalare Quantisierung senkt den Speicher-
     * bedarf des Vektorbestands auf etwa ein Viertel, was bei grossen Installationen ueber
     * Erfolg oder Misserfolg entscheidet, und kostet praktisch keine Trefferqualitaet.
     *
     * @param dimension Vektordimension des Embedding-Modells
     * @return Mapping-Block
     */
    public static Map<String, Object> mappings(int dimension) {
        Map<String, Object> props = new LinkedHashMap<>();

        props.put(RagChunkFields.CHUNK_ID, keyword());
        props.put(RagChunkFields.DOCUMENT_GROUP, keyword());
        props.put(RagChunkFields.NODE_UUID, keyword());
        props.put(RagChunkFields.PROJECT_UUID, keyword());
        props.put(RagChunkFields.PROJECT, keyword());
        props.put(RagChunkFields.BRANCH_UUID, keyword());
        props.put(RagChunkFields.BRANCH, keyword());
        props.put(RagChunkFields.LANGUAGE, keyword());
        props.put(RagChunkFields.SCHEMA, keyword());
        props.put(RagChunkFields.PATH, keyword());
        props.put(RagChunkFields.TITLE, textWithRaw());
        props.put(RagChunkFields.CHUNK_TEXT, type("text"));
        props.put(RagChunkFields.CHUNK_INDEX, type("integer"));
        props.put(RagChunkFields.FIELD_PATH, keyword());
        props.put(RagChunkFields.EMBEDDING, denseVector(dimension));
        props.put(RagChunkFields.MESH_VERSION, keyword());
        props.put(RagChunkFields.CONTENT_HASH, keyword());
        props.put(RagChunkFields.PUBLISH_STATUS, keyword());
        props.put(RagChunkFields.VALID_FROM, type("date"));
        props.put(RagChunkFields.VALID_TO, type("date"));
        props.put(RagChunkFields.CREATED_DATE, type("date"));
        props.put(RagChunkFields.EDITED_DATE, type("date"));
        props.put(RagChunkFields.ROLE_UUIDS, keyword());
        props.put(RagChunkFields.SOURCE_URL, keyword());
        props.put(RagChunkFields.REFERENCED_NODES, keyword());
        props.put(RagChunkFields.EDITORIAL_TAGS, keyword());
        props.put("embeddingError", keyword());

        return Map.of("properties", props);
    }

    private static Map<String, Object> denseVector(int dimension) {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "dense_vector");
        field.put("dims", dimension);
        field.put("index", true);
        field.put("similarity", "cosine");
        field.put("index_options", Map.of("type", "int8_hnsw"));
        return field;
    }

    private static Map<String, Object> textWithRaw() {
        Map<String, Object> field = new LinkedHashMap<>();
        field.put("type", "text");
        field.put("fields", Map.of("raw", keyword()));
        return field;
    }

    private static Map<String, Object> keyword() {
        return type("keyword");
    }

    private static Map<String, Object> type(String type) {
        return Map.of("type", type);
    }
}
