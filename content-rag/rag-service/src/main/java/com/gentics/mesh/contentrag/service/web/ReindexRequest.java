package com.gentics.mesh.contentrag.service.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-Body fuer {@code POST /api/rag/reindex} (Konzept 9). nodeUuid optional:
 * fehlt er, wird das gesamte Projekt (via Mesh-Full-Sync-Reuse) neu indexiert.
 *
 * @param projectUuid Mandant
 * @param nodeUuid    optionaler Node fuer node-bezogenen Reindex
 * @param branch      Branch (Default {@code main}, wenn leer)
 * @param language    Sprache (Default {@code en}, wenn leer)
 */
public record ReindexRequest(
        @NotBlank String projectUuid,
        String nodeUuid,
        String branch,
        String language
) {
}
