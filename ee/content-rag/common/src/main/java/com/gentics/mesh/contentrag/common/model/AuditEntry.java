package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;
import java.util.List;

/**
 * Audit-Eintrag je RAG-Auslieferung (Konzept 5.10/10): Anfrage, geprüfte Berechtigungen,
 * verwendete Quellen und optional die Antwort. DSGVO: Antwort-Logging opt-in, mit Aufbewahrungsfrist (O7).
 *
 * @param id              eindeutige Eintrags-ID
 * @param timestamp       Zeitpunkt
 * @param userRef         pseudonymisierte Nutzerreferenz (Datensparsamkeit)
 * @param projectUuid     betroffener Mandant
 * @param question        Anfragetext
 * @param sourceChunkIds  IDs der verwendeten Quellen-Chunks
 * @param checkedRoleUuids zur Filterung herangezogene Rollen-UUIDs
 * @param answerLogged    ob die Antwort gespeichert wurde (opt-in)
 * @param answer          Antworttext, nur wenn answerLogged
 */
public record AuditEntry(
        String id,
        Instant timestamp,
        String userRef,
        String projectUuid,
        String question,
        List<String> sourceChunkIds,
        List<String> checkedRoleUuids,
        boolean answerLogged,
        String answer
) {
    public AuditEntry {
        sourceChunkIds = sourceChunkIds == null ? List.of() : List.copyOf(sourceChunkIds);
        checkedRoleUuids = checkedRoleUuids == null ? List.of() : List.copyOf(checkedRoleUuids);
    }
}
