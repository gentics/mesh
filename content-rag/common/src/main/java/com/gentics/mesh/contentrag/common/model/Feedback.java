package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;

/**
 * Nutzerfeedback zu einer Antwort (Konzept 5.9/5.10, API {@code POST /api/rag/feedback}).
 *
 * @param id           eindeutige ID
 * @param timestamp    Zeitpunkt
 * @param auditEntryId Bezug zur ausgelieferten Antwort (AuditEntry)
 * @param projectUuid  Mandant
 * @param helpful      Daumen hoch/runter
 * @param comment      optionaler Freitext
 */
public record Feedback(
        String id,
        Instant timestamp,
        String auditEntryId,
        String projectUuid,
        boolean helpful,
        String comment
) {
}
