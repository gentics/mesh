package com.gentics.mesh.contentrag.meshsync;

import java.util.Locale;
import java.util.Map;

/**
 * Ein Schemafeld, so weit die Feldauswahl es kennen muss. Bewusst eine eigene, framework-freie
 * Sicht auf {@code FieldSchema}: die Auswahlregeln sind damit ohne laufendes Mesh testbar, und
 * dieses Modul haengt nicht an der Mesh-Schema-API.
 *
 * @param name     Feldname im Schema
 * @param type     Feldtyp in Kleinschreibung ({@code string}, {@code html}, {@code list},
 *                 {@code micronode}, {@code binary}, {@code s3binary}, {@code number} …),
 *                 entspricht {@code FieldTypes#toString()}
 * @param noIndex  Wert von {@code FieldSchema#isNoIndex()}: das bestehende Opt-out der
 *                 Volltextsuche, das hier als implizites No-RAG gilt
 * @param settings freies {@code elasticsearch}-JSON des Feldes als Map; nur der Schluessel
 *                 {@code rag} wird ausgewertet
 */
public record RagFieldDescriptor(String name, String type, boolean noIndex, Map<String, Object> settings) {

    /** Normalisiert den Typ auf Kleinschreibung und ersetzt {@code null}-Settings durch eine leere Map. */
    public RagFieldDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Feldname fehlt");
        }
        type = type == null ? "" : type.toLowerCase(Locale.ROOT);
        settings = settings == null ? Map.of() : Map.copyOf(settings);
    }

    /**
     * Bequemer Konstruktor fuer Felder ohne eigene {@code elasticsearch}-Settings.
     *
     * @param name Feldname
     * @param type Feldtyp
     */
    public RagFieldDescriptor(String name, String type) {
        this(name, type, false, Map.of());
    }
}
