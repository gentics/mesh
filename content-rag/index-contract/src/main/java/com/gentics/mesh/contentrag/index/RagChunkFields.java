package com.gentics.mesh.contentrag.index;

/**
 * Feldnamen des RAG-Chunk-Dokuments. Einzige Wahrheit fuer beide Seiten: die schreibende
 * Mesh-Seite baut das Dokument damit auf, der lesende RAG-Service formuliert damit seine Queries.
 *
 * <p>Das Permission-Feld heisst bewusst {@value #ROLE_UUIDS} – identisch zu dem Feld, das die
 * Mesh-Volltextsuche in ihren Node-Dokumenten fuehrt. Damit ist die Filterlogik zwischen
 * Volltextsuche und RAG-Retrieval dieselbe (K3).
 */
public final class RagChunkFields {

    private RagChunkFields() {
    }

    /** Deterministische Chunk-ID; zugleich die Elasticsearch-Dokument-ID. */
    public static final String CHUNK_ID = "chunkId";
    /** {@code nodeUuid:branch:language} – Klammer fuer Group-Delete-and-Replace. */
    public static final String DOCUMENT_GROUP = "documentGroup";
    /** UUID des Mesh-Nodes. */
    public static final String NODE_UUID = "nodeUuid";
    /** Mandant. */
    public static final String PROJECT_UUID = "projectUuid";
    /** Projektname (Anzeige). */
    public static final String PROJECT = "project";
    /** Branch-UUID. */
    public static final String BRANCH_UUID = "branchUuid";
    /** Branch-Name. */
    public static final String BRANCH = "branch";
    /** Sprachcode. */
    public static final String LANGUAGE = "language";
    /** Schemaname. */
    public static final String SCHEMA = "schema";
    /** Pfad/Slug. */
    public static final String PATH = "path";
    /** Titel des Nodes. */
    public static final String TITLE = "title";
    /** Chunk-Text: BM25-Feld und Eingabe der Vektorisierung. */
    public static final String CHUNK_TEXT = "chunkText";
    /** Position des Chunks innerhalb der Document-Group. */
    public static final String CHUNK_INDEX = "chunkIndex";
    /** Herkunftsfeld im Node. */
    public static final String FIELD_PATH = "fieldPath";
    /**
     * Vektorfeld. Wird <strong>nicht</strong> vom Schreiber befuellt, sondern von der
     * Ingest-Pipeline beim Indexieren erzeugt (siehe {@link RagInferenceSetup}).
     */
    public static final String EMBEDDING = "embedding";
    /** Mesh-Version des Quell-Nodes. */
    public static final String MESH_VERSION = "meshVersion";
    /** Hash des normalisierten Inhalts. */
    public static final String CONTENT_HASH = "contentHash";
    /** published/draft/archived. */
    public static final String PUBLISH_STATUS = "publishStatus";
    /** Gueltig-ab. */
    public static final String VALID_FROM = "validFrom";
    /** Gueltig-bis. */
    public static final String VALID_TO = "validTo";
    /** Erstelldatum. */
    public static final String CREATED_DATE = "createdDate";
    /** Aenderungsdatum. */
    public static final String EDITED_DATE = "editedDate";
    /** Rollen-UUIDs mit Leserecht – gleiche Semantik und gleicher Name wie in der Mesh-Suche. */
    public static final String ROLE_UUIDS = "_roleUuids";
    /** Quellen-URL. */
    public static final String SOURCE_URL = "sourceUrl";
    /** Aufgeloeste Referenzen. */
    public static final String REFERENCED_NODES = "referencedNodes";
    /** Redaktionelle Metadaten. */
    public static final String EDITORIAL_TAGS = "editorialTags";

    /** Rollenwert fuer oeffentlich lesbare Inhalte, analog zur Mesh-Suche. */
    public static final String PUBLIC_ROLE = "public";
}
