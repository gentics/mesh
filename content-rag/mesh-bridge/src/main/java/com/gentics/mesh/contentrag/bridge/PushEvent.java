package com.gentics.mesh.contentrag.bridge;

import com.gentics.mesh.contentrag.common.model.DocumentGroupId;

import java.util.List;

/**
 * Push-Kontrakt: was Mesh (bzw. das Bridge-Plugin) aktiv an den RAG-Indexer schiebt.
 * Die Nutzlast ist bewusst schlank; fehlende Felder/Referenzen liest der RAG-Service
 * bei Bedarf per GraphQL nach (ContentReader, Konzept 5.1).
 *
 * @param type          Art der Aenderung
 * @param groupId       betroffene Document-Group (Node, Branch, Sprache)
 * @param projectUuid   Mandant
 * @param schema        Schemaname (fuer Selektions-/No-RAG-Pruefung)
 * @param meshVersion   Mesh-Version des Nodes (Idempotenz)
 * @param readRoleUuids aktuelle lesende Rollen-UUIDs (K3) – Quelle wie bei der Mesh-Suche
 */
public record PushEvent(
        PushEventType type,
        DocumentGroupId groupId,
        String projectUuid,
        String schema,
        String meshVersion,
        List<String> readRoleUuids
) {
    public PushEvent {
        readRoleUuids = readRoleUuids == null ? List.of() : List.copyOf(readRoleUuids);
    }
}
