package com.gentics.mesh.contentrag.common.model;

import java.util.List;

/**
 * Anfrage an die RAG-Query-Pipeline.
 *
 * @param question        Nutzerfrage
 * @param language        bevorzugte Antwortsprache (nullable -&gt; Service-Default)
 * @param userRoleUuids   Rollen-UUIDs des Anfragenden fuer den Permission-Filter (K3); leer = anonym/public
 * @param projectFilter   optionale Einschraenkung auf Projekte (projectUuid)
 * @param schemaFilter    optionale Einschraenkung auf Schemas
 * @param pathPrefix      optionaler Pfad-Praefix-Filter
 * @param topK            gewuenschte Anzahl Treffer fuer das Retrieval (nullable -&gt; Service-Default)
 * @param stream          ob die Antwort gestreamt werden soll
 * @param debug           ob Chunk-Scores/Debug-Infos mitgeliefert werden sollen
 */
public record RagQuery(
        String question,
        String language,
        List<String> userRoleUuids,
        List<String> projectFilter,
        List<String> schemaFilter,
        String pathPrefix,
        Integer topK,
        boolean stream,
        boolean debug
) {
    public RagQuery {
        userRoleUuids = userRoleUuids == null ? List.of() : List.copyOf(userRoleUuids);
        projectFilter = projectFilter == null ? List.of() : List.copyOf(projectFilter);
        schemaFilter = schemaFilter == null ? List.of() : List.copyOf(schemaFilter);
    }
}
