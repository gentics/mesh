package com.gentics.mesh.contentrag.service.web;

import java.time.Instant;

/**
 * Antwort fuer {@code GET /api/rag/status} (Konzept 9/5.10).
 *
 * @param status        textueller Gesamtstatus
 * @param indexedGroups Anzahl indexierter Document-Groups
 * @param indexedChunks Anzahl indexierter Chunks
 * @param failedGroups  Anzahl fehlgeschlagener Document-Groups
 * @param lastSync      Zeitpunkt des letzten Syncs (nullable)
 */
public record IndexStatus(
        String status,
        long indexedGroups,
        long indexedChunks,
        long failedGroups,
        Instant lastSync
) {
}
