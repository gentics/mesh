package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;

/**
 * Schlanker State pro Document-Group fuer Embedding-Idempotenz und No-Op-Erkennung (Konzept K6).
 * Ersetzt nicht Mesh' Full-Sync, optimiert nur den teuren Embedding-Schritt.
 *
 * @param groupId        Document-Group
 * @param projectUuid    Mandant (fuer projektweite Operationen)
 * @param contentHash    zuletzt verarbeiteter contentHash
 * @param meshVersion    zuletzt verarbeitete Mesh-Version
 * @param embeddingModel Modell, mit dem zuletzt embedded wurde
 * @param status         Verarbeitungsstatus
 * @param lastError      Fehlertext bei FAILED, sonst null
 * @param updatedAt      Zeitpunkt der letzten Aktualisierung
 */
public record ChunkState(
        DocumentGroupId groupId,
        String projectUuid,
        String contentHash,
        String meshVersion,
        String embeddingModel,
        ChunkProcessingStatus status,
        String lastError,
        Instant updatedAt
) {
    /**
     * Ob fuer den gegebenen Inhalt/Modell ein Re-Embedding noetig ist.
     * No-Op nur, wenn contentHash UND embeddingModel unveraendert sind.
     */
    public boolean needsReprocessing(String newContentHash, String currentEmbeddingModel) {
        return !status.equals(ChunkProcessingStatus.INDEXED)
                || contentHash == null
                || !contentHash.equals(newContentHash)
                || embeddingModel == null
                || !embeddingModel.equals(currentEmbeddingModel);
    }
}
