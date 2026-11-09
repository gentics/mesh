package com.gentics.mesh.contentrag.service.web;

import java.time.Instant;

/**
 * Antwort fuer {@code GET /api/rag/status}.
 *
 * <p>Bewusst nur, was im Index sichtbar ist: den Sync-Fortschritt selbst kennt Mesh und meldet
 * ihn ueber seine eigenen Endpunkte.
 *
 * @param status              {@code ok}, {@code unvollstaendig}, {@code leer} oder
 *                            {@code nicht verfuegbar}
 * @param documentGroups      Anzahl unterschiedlicher Document-Groups (Naeherung ueber
 *                            Cardinality-Aggregation)
 * @param chunks              Anzahl indexierter Chunks
 * @param chunksWithoutVector Chunks ohne Embedding – nur ueber BM25 auffindbar
 * @param lastCheck           Zeitpunkt der Erhebung (nullable)
 */
public record IndexStatus(
        String status,
        long documentGroups,
        long chunks,
        long chunksWithoutVector,
        Instant lastCheck
) {
}
