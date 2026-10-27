package com.gentics.mesh.contentrag.meshsync;

import java.util.List;
import java.util.Map;

/**
 * Eine Schemaversion, so weit die Feldauswahl sie kennen muss.
 *
 * @param name        Schemaname
 * @param versionUuid {@code schemaVersionUuid}; zugleich der Cache-Schluessel in
 *                    {@link RagFieldSelector}, weil sich die Auswahl nur mit der Schemaversion
 *                    aendern kann
 * @param settings    freies {@code elasticsearch}-JSON des Schemas als Map; nur der Schluessel
 *                    {@code rag} wird ausgewertet
 * @param fields      Felder in Schemareihenfolge – diese Reihenfolge bestimmt spaeter die
 *                    Reihenfolge der Chunks und damit deren {@code chunkIndex}
 */
public record RagSchemaDescriptor(String name, String versionUuid, Map<String, Object> settings,
                                  List<RagFieldDescriptor> fields) {

    /** Ersetzt {@code null}-Settings und {@code null}-Feldliste durch leere Sammlungen. */
    public RagSchemaDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Schemaname fehlt");
        }
        settings = settings == null ? Map.of() : Map.copyOf(settings);
        fields = fields == null ? List.of() : List.copyOf(fields);
    }
}
