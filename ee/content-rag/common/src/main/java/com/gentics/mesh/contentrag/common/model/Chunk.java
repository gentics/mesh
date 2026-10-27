package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;
import java.util.List;

/**
 * Ein indexierbarer Inhalts-Chunk und zugleich das Quelldokument eines RAG-Chunk-Index-Eintrags
 * (Feldnamen siehe {@code RagChunkFields} im Modul {@code index-contract}).
 *
 * <p>Der Chunk traegt bewusst <strong>keinen</strong> Embedding-Vektor: die Vektorisierung
 * erfolgt in Elasticsearch beim Indexieren (Ingest-Pipeline mit Inference-Processor), damit
 * weder Mesh noch der RAG-Service ML-Last traegt und der Mesh-Sync nicht am Embedder haengt.
 *
 * @param chunkId         deterministische ID = hash(nodeUuid+branch+language+fieldPath+chunkIndex);
 *                        wird als Elasticsearch-Dokument-ID verwendet
 * @param documentGroup   Gruppe fuer Group-Delete (nodeUuid:branch:language)
 * @param nodeUuid        Node-Referenz
 * @param projectUuid     Mandant / Index-Zuordnung
 * @param project         Projektname (Anzeige)
 * @param branchUuid      Branch-UUID (Index-Zuordnung)
 * @param branch          Branch-Name (Anzeige/Filter)
 * @param language        Sprachfilter
 * @param schema          Schemafilter
 * @param path            Pfad/Slug zur Quelle-URL-Bildung
 * @param title           Anzeige und BM25
 * @param chunkText       BM25, Eingabe der Vektorisierung und LLM-Kontext
 * @param chunkIndex      Position innerhalb der Document-Group
 * @param fieldPath       Herkunftsfeld im Node
 * @param meshVersion     Mesh-Version des Quell-Nodes (Diagnose/Nachvollziehbarkeit)
 * @param contentHash     Hash des normalisierten Inhalts (Diagnose, Dubletten-Erkennung)
 * @param publishStatus   published/draft/archived
 * @param validFrom       Gueltig-ab (Query-Zeit-Filter, V1), nullable
 * @param validTo         Gueltig-bis (Query-Zeit-Filter, V1), nullable
 * @param createdDate     Erstelldatum
 * @param editedDate      Aenderungsdatum (Aktualitaets-Boost)
 * @param roleUuids       Rollen-UUIDs mit Leserecht; identische Semantik wie Mesh' {@code _roleUuids} (K3)
 * @param sourceUrl       Quellenangabe
 * @param referencedNodes aufgeloeste Referenzen
 * @param editorialTags   redaktionelle Metadaten
 */
public record Chunk(
        String chunkId,
        String documentGroup,
        String nodeUuid,
        String projectUuid,
        String project,
        String branchUuid,
        String branch,
        String language,
        String schema,
        String path,
        String title,
        String chunkText,
        int chunkIndex,
        String fieldPath,
        String meshVersion,
        String contentHash,
        PublishStatus publishStatus,
        Instant validFrom,
        Instant validTo,
        Instant createdDate,
        Instant editedDate,
        List<String> roleUuids,
        String sourceUrl,
        List<String> referencedNodes,
        List<String> editorialTags
) {
    /** Defensivkopien fuer die Listenfelder. */
    public Chunk {
        roleUuids = roleUuids == null ? List.of() : List.copyOf(roleUuids);
        referencedNodes = referencedNodes == null ? List.of() : List.copyOf(referencedNodes);
        editorialTags = editorialTags == null ? List.of() : List.copyOf(editorialTags);
    }
}
