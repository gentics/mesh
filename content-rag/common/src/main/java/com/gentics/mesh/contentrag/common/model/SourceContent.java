package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Normalisierter Quellinhalt einer Document-Group (Node, Branch, Sprache) als Eingabe des
 * Chunkings. Wird auf der Mesh-Seite aus dem Node-Container aufgebaut, waehrend der
 * Standard-Sync das RAG-Chunk-Dokument erzeugt – es findet kein Rueckruf nach Mesh statt.
 *
 * @param groupId         Document-Group (Node, Branch, Sprache)
 * @param projectUuid     Mandant
 * @param project         Projektname
 * @param branchUuid      Branch-UUID (Index-Zuordnung)
 * @param schema          Schemaname
 * @param title           Titel des Nodes
 * @param path            Pfad/Slug
 * @param sourceUrl       Quellen-URL
 * @param markdownByField normalisierter Text je Feldpfad (HTML bereits nach Markdown gewandelt)
 * @param meshVersion     Mesh-Version des Node-Containers
 * @param publishStatus   Publikationsstatus
 * @param validFrom       Gueltig-ab, nullable
 * @param validTo         Gueltig-bis, nullable
 * @param createdDate     Erstelldatum
 * @param editedDate      Aenderungsdatum
 * @param roleUuids       Rollen-UUIDs mit Leserecht, identisch zu Mesh' {@code _roleUuids}
 * @param referencedNodes aufgeloeste Referenzen
 * @param editorialTags   redaktionelle Metadaten
 */
public record SourceContent(
        DocumentGroupId groupId,
        String projectUuid,
        String project,
        String branchUuid,
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
        List<String> roleUuids,
        List<String> referencedNodes,
        List<String> editorialTags
) {
    /** Defensivkopien fuer Map- und Listenfelder. */
    public SourceContent {
        markdownByField = markdownByField == null ? Map.of() : Map.copyOf(markdownByField);
        roleUuids = roleUuids == null ? List.of() : List.copyOf(roleUuids);
        referencedNodes = referencedNodes == null ? List.of() : List.copyOf(referencedNodes);
        editorialTags = editorialTags == null ? List.of() : List.copyOf(editorialTags);
    }
}
