package com.gentics.mesh.contentrag.service.web;

import com.gentics.mesh.contentrag.common.model.RagQuery;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request-Body fuer {@code POST /api/rag/query} (Konzept 9).
 *
 * @param question   Nutzerfrage
 * @param language   bevorzugte Antwortsprache (nullable)
 * @param roleUuids  Rollen-UUIDs des Anfragenden fuer den Permission-Filter (K3)
 * @param projects   optionale Einschraenkung auf Projekte (projectUuid)
 * @param schemas    optionale Einschraenkung auf Schemas
 * @param pathPrefix optionaler Pfad-Praefix-Filter
 * @param topK       gewuenschte Trefferzahl (nullable -&gt; Service-Default)
 * @param stream     ob die Antwort gestreamt werden soll
 * @param debug      ob Chunk-Scores/Debug-Infos mitgeliefert werden sollen
 */
public record QueryRequest(
        @NotBlank String question,
        String language,
        List<String> roleUuids,
        List<String> projects,
        List<String> schemas,
        String pathPrefix,
        Integer topK,
        boolean stream,
        boolean debug
) {
    /**
     * @return das DTO als internes {@link RagQuery}-Modell
     */
    public RagQuery toQuery() {
        return new RagQuery(question, language, roleUuids, projects, schemas, pathPrefix, topK, stream, debug);
    }
}
