package com.gentics.mesh.contentrag.service.web;

import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.service.security.MeshPrincipal;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request-Body fuer {@code POST /api/rag/query}.
 *
 * <p>Es gibt hier bewusst <strong>kein</strong> Feld fuer Rollen: die stammen ausschliesslich
 * aus dem Mesh-Token des Anfragenden. Ein Rollenfeld im Body waere eine Selbstauskunft und
 * damit die Berechtigungspruefung nur noch eine Formalie.
 *
 * @param question   Nutzerfrage
 * @param language   bevorzugte Antwortsprache (nullable)
 * @param projects   optionale Einschraenkung auf Projekte (projectUuid); schraenkt nur weiter
 *                   ein, erweitert nie — die Grenze zieht der Rollenfilter
 * @param schemas    optionale Einschraenkung auf Schemas
 * @param pathPrefix optionaler Pfad-Praefix-Filter
 * @param topK       gewuenschte Trefferzahl (nullable -&gt; Service-Default)
 * @param stream     ob die Antwort gestreamt werden soll
 * @param debug      ob Chunk-Scores/Debug-Infos mitgeliefert werden sollen
 */
public record QueryRequest(
        @NotBlank String question,
        String language,
        List<String> projects,
        List<String> schemas,
        String pathPrefix,
        Integer topK,
        boolean stream,
        boolean debug
) {
    /**
     * @param principal der ueber das Mesh-Token aufgeloeste Anfragende
     * @return das DTO als internes {@link RagQuery}-Modell, mit den Rollen des Principals
     */
    public RagQuery toQuery(MeshPrincipal principal) {
        return new RagQuery(question, language, principal.roleUuids(), projects, schemas,
                pathPrefix, topK, stream, debug);
    }
}
