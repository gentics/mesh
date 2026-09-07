package com.gentics.mesh.contentrag.common.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * @param markdownByField normalisierter Text je Feldpfad (HTML bereits nach Markdown gewandelt),
 *                        in Schemareihenfolge; die Iterationsreihenfolge geht in {@code chunkIndex}
 *                        und damit in die Chunk-IDs ein und wird deshalb erhalten
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
    /**
     * Defensivkopien fuer Map- und Listenfelder.
     *
     * <p>Bewusst {@link LinkedHashMap} statt {@link Map#copyOf(Map)}: dessen Iterationsreihenfolge
     * ist nicht festgelegt und aendert sich seit Java 9 sogar zwischen zwei JVM-Starts. Der
     * Chunker zaehlt {@code chunkIndex} in Iterationsreihenfolge hoch, und die Chunk-ID haengt an
     * {@code fieldPath + chunkIndex} – eine wechselnde Reihenfolge ergaebe also fuer denselben
     * Inhalt andere IDs und damit Dubletten im Index statt eines Upserts.
     */
    public SourceContent {
        markdownByField = markdownByField == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(markdownByField));
        roleUuids = roleUuids == null ? List.of() : List.copyOf(roleUuids);
        referencedNodes = referencedNodes == null ? List.of() : List.copyOf(referencedNodes);
        editorialTags = editorialTags == null ? List.of() : List.copyOf(editorialTags);
    }
}
