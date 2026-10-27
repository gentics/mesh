package com.gentics.mesh.contentrag.common.model;

/**
 * Quellenangabe zu einer generierten Antwort (Konzept 5.8).
 *
 * @param title     Titel des Quell-Nodes/Chunks
 * @param sourceUrl Quelle-URL
 * @param nodeUuid  Node-Referenz
 * @param chunkId   konkreter belegender Chunk
 * @param score     normalisierter Relevanz-Score des Chunks
 */
public record SourceReference(
        String title,
        String sourceUrl,
        String nodeUuid,
        String chunkId,
        double score
) {
}
