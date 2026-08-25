package com.gentics.mesh.contentrag.common.model;

import java.util.List;

/**
 * Ergebnis der Antwortgenerierung.
 *
 * @param answer    generierte Antwort; bei fehlender Quellenlage die definierte Nicht-Antwort (V5)
 * @param grounded  ob die Antwort auf belegten Chunks beruht (false -&gt; Nicht-Antwort ausgeliefert)
 * @param sources   verwendete Quellen
 * @param debug     optionale Debug-Infos (Chunk-Scores), nur wenn angefragt; sonst leer
 */
public record RagAnswer(
        String answer,
        boolean grounded,
        List<SourceReference> sources,
        List<RetrievedChunk> debug
) {
    public RagAnswer {
        sources = sources == null ? List.of() : List.copyOf(sources);
        debug = debug == null ? List.of() : List.copyOf(debug);
    }

    /** Standard-Nicht-Antwort, wenn keine ausreichende Quellenlage vorliegt. */
    public static RagAnswer noSufficientSources(String message) {
        return new RagAnswer(message, false, List.of(), List.of());
    }
}
