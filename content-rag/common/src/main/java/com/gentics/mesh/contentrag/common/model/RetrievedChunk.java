package com.gentics.mesh.contentrag.common.model;

/**
 * Ein vom Retrieval zurueckgegebener Chunk samt normalisiertem Hybrid-Score.
 *
 * @param chunk der gefundene Chunk
 * @param score normalisierter Score (z. B. nach RRF), aufsteigend sortierbar
 */
public record RetrievedChunk(Chunk chunk, double score) {
}
