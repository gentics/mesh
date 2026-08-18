package com.gentics.mesh.contentrag.common.spi;

import java.util.List;

/**
 * Abstraktion ueber den Embedding-Anbieter (Konzept 5.5). Implementierungen:
 * lokal (HuggingFace TEI / ONNX, e5-small / bge-small, CPU-faehig) oder extern.
 * Embeddings sind vergleichsweise guenstig und laufen meist lokal auf CPU.
 */
public interface EmbeddingProvider {

    /** Kennung des Modells (z. B. {@code multilingual-e5-small}); wird je Chunk persistiert (V4). */
    String modelId();

    /** Vektordimension des Modells. */
    int dimension();

    /**
     * Embedded einen Batch von Texten. Reihenfolge des Ergebnisses entspricht der Eingabe.
     * Implementierungen sollten Batching und Retry mit Backoff umsetzen.
     */
    List<float[]> embed(List<String> texts);

    /** Bequemlichkeitsmethode fuer einen einzelnen Text. */
    default float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }

    /**
     * Embedded zu indexierende Dokument-/Passage-Texte. Getrennt von {@link #embedQuery(String)},
     * weil manche Modelle (z. B. e5) asymmetrische Prefixe fuer Passagen vs. Anfragen erwarten.
     * Default: identisch zu {@link #embed(List)}.
     */
    default List<float[]> embedDocuments(List<String> texts) {
        return embed(texts);
    }

    /**
     * Embedded eine Suchanfrage. Default: identisch zu {@link #embed(String)}.
     */
    default float[] embedQuery(String text) {
        return embed(text);
    }
}
