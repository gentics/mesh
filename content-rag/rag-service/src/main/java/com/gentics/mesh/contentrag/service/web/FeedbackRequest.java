package com.gentics.mesh.contentrag.service.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-Body fuer {@code POST /api/rag/feedback} (Konzept 9).
 *
 * @param auditEntryId Bezug zur ausgelieferten Antwort (AuditEntry)
 * @param projectUuid  Mandant
 * @param helpful      Daumen hoch/runter
 * @param comment      optionaler Freitext
 */
public record FeedbackRequest(
        @NotBlank String auditEntryId,
        String projectUuid,
        boolean helpful,
        String comment
) {
}
