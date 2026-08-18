package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;
import java.util.List;

/**
 * Ein indexierbarer Inhalts-Chunk. Entspricht einem Elasticsearch-Dokument
 * (Mapping-Entwurf, Konzept Abschnitt 6).
 *
 * @param chunkId            deterministische ID = hash(nodeUuid+branch+language+fieldPath+chunkIndex) (K5)
 * @param documentGroup      Gruppe fuer Group-Delete (nodeUuid:branch:language)
 * @param nodeUuid           Node-Referenz
 * @param projectUuid        Mandant / Routing
 * @param project            Projektname (Anzeige)
 * @param branch             Branch-Filter
 * @param language           Sprachfilter
 * @param schema             Schemafilter
 * @param path               Pfad/Slug zur Quelle-URL-Bildung
 * @param title              Anzeige & BM25
 * @param chunkText          BM25 + LLM-Kontext
 * @param chunkIndex         Position innerhalb der Document-Group
 * @param fieldPath          Herkunftsfeld im Node
 * @param embedding          Vektor; {@code null} solange noch nicht embedded
 * @param embeddingModel     Modellkennung fuer Versionierung (V4)
 * @param embeddingDim       Vektordimension
 * @param meshVersion        Mesh-Version des Quell-Nodes (Idempotenz)
 * @param contentHash        Hash des normalisierten Inhalts (No-Op-Erkennung, K5/K6)
 * @param publishStatus      published/draft/archived
 * @param validFrom          Gueltig-ab (Query-Zeit-Filter, V1), nullable
 * @param validTo            Gueltig-bis (Query-Zeit-Filter, V1), nullable
 * @param createdDate        Erstelldatum
 * @param editedDate         Aenderungsdatum (Aktualitaets-Boost)
 * @param readRoleUuids      Rollen-UUIDs mit Leserecht (Permission-Pre-Filter, K3)
 * @param sourceUrl          Quellenangabe
 * @param referencedNodes    aufgeloeste Referenzen
 * @param editorialTags      redaktionelle Metadaten
 */
public record Chunk(
        String chunkId,
        String documentGroup,
        String nodeUuid,
        String projectUuid,
        String project,
        String branch,
        String language,
        String schema,
        String path,
        String title,
        String chunkText,
        int chunkIndex,
        String fieldPath,
        float[] embedding,
        String embeddingModel,
        Integer embeddingDim,
        String meshVersion,
        String contentHash,
        PublishStatus publishStatus,
        Instant validFrom,
        Instant validTo,
        Instant createdDate,
        Instant editedDate,
        List<String> readRoleUuids,
        String sourceUrl,
        List<String> referencedNodes,
        List<String> editorialTags
) {
    /** Ob bereits ein Embedding-Vektor gesetzt ist. */
    public boolean hasEmbedding() {
        return embedding != null && embedding.length > 0;
    }
}
