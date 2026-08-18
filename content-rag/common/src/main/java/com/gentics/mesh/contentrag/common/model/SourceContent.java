package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalisierter, sprachreiner Inhalt eines (Node, Branch, Sprache) vor dem Chunking
 * (Ergebnis der Normalisierung, Konzept 5.3). HTML wurde bereits nach Markdown konvertiert (V8).
 *
 * @param groupId         Document-Group-Identitaet
 * @param projectUuid     Mandant
 * @param project         Projektname
 * @param schema          Schemaname
 * @param title           Titel des Nodes
 * @param path            Pfad/Slug
 * @param sourceUrl       Quelle-URL
 * @param markdownByField normalisierter Markdown-Text je Feldpfad (erhaelt Struktur fuers Chunking)
 * @param meshVersion     Mesh-Version
 * @param publishStatus   Publikationsstatus
 * @param validFrom       Gueltig-ab (nullable)
 * @param validTo         Gueltig-bis (nullable)
 * @param createdDate     Erstelldatum
 * @param editedDate      Aenderungsdatum
 * @param readRoleUuids   Rollen-UUIDs mit Leserecht (K3)
 * @param referencedNodes aufgeloeste Referenzen
 * @param editorialTags   redaktionelle Metadaten
 */
public record SourceContent(
        DocumentGroupId groupId,
        String projectUuid,
        String project,
        String schema,
        String title,
        String path,
        String sourceUrl,
        Map<String, String> markdownByField,
        String meshVersion,
        PublishStatus publishStatus,
        Instant validFrom,
        Instant validTo,
        Instant createdDate,
        Instant editedDate,
        List<String> readRoleUuids,
        List<String> referencedNodes,
        List<String> editorialTags
) {
    public SourceContent {
        markdownByField = markdownByField == null ? Map.of() : Map.copyOf(markdownByField);
        readRoleUuids = readRoleUuids == null ? List.of() : List.copyOf(readRoleUuids);
        referencedNodes = referencedNodes == null ? List.of() : List.copyOf(referencedNodes);
        editorialTags = editorialTags == null ? List.of() : List.copyOf(editorialTags);
    }
}
