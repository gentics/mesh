package com.gentics.mesh.contentrag.common.spi;

import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;

import java.util.List;

/**
 * Schreibt Chunks nach Elasticsearch (Konzept 5.6). Idempotent ueber
 * Group-Delete-and-Replace (K5): bei Update wird per Delete-by-Query die gesamte
 * Document-Group entfernt und neu geschrieben.
 */
public interface Indexer {

    /**
     * Ersetzt atomar (aus Sicht der Gruppe) alle Chunks einer Document-Group:
     * Delete-by-Query auf {@code documentGroup} + Bulk-Index der neuen Chunks.
     */
    void replaceGroup(DocumentGroupId groupId, List<Chunk> chunks);

    /** Entfernt alle Chunks einer Document-Group (Node geloescht/depubliziert). */
    void deleteGroup(DocumentGroupId groupId);

    /** Voller Rebuild eines Projekts (manueller Reindex, Konzept 5.9). */
    void deleteProject(String projectUuid);
}
