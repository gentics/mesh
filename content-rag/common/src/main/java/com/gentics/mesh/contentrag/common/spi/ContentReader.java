package com.gentics.mesh.contentrag.common.spi;

import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.SourceContent;

import java.util.Optional;

/**
 * Liest und normalisiert Mesh-Inhalte (Konzept 5.1/5.3). Wo die Push-Nutzlast nicht alle
 * benoetigten Felder/Referenzen enthaelt, liest die Implementierung ergaenzend ueber GraphQL
 * (gezielte Feldauswahl, Sprache/Branch, Referenzaufloesung mit Tiefenbegrenzung, V7).
 */
public interface ContentReader {

    /**
     * Liefert den normalisierten Inhalt einer Document-Group.
     *
     * @return leeres Optional, wenn der Node nicht (mehr) existiert, nicht freigegeben ist
     *         oder per No-RAG-Konvention (K4) ausgeschlossen wurde
     */
    Optional<SourceContent> read(DocumentGroupId groupId);
}
