package com.gentics.mesh.contentrag.common.model;

/**
 * Verarbeitungsstatus einer Document-Group im schlanken Chunk-State (Konzept K6).
 */
public enum ChunkProcessingStatus {
    /** In Bearbeitung / noch nicht abgeschlossen. */
    PENDING,
    /** Erfolgreich indexiert. */
    INDEXED,
    /** Verarbeitung fehlgeschlagen (siehe lastError). */
    FAILED,
    /** Uebersprungen, weil contentHash unveraendert (No-Op, kein Re-Embedding). */
    SKIPPED_NOOP
}
