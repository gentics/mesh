package com.gentics.mesh.contentrag.meshsync;

/**
 * Verhalten fuer Schemas, die keine eigene RAG-Angabe in ihren
 * {@code elasticsearch}-Settings tragen.
 */
public enum RagSelectionMode {

    /**
     * Nichts ohne ausdrueckliche Freigabe. Vorgabe, damit das Einschalten des Features in einer
     * bestehenden Installation keinen Inhalt in den RAG-Index traegt, den niemand freigegeben hat.
     */
    NONE,

    /**
     * Alle textfuehrenden Felder, sofern sie nicht einzeln ausgeschlossen sind. Sinnvoll fuer
     * Installationen, in denen ohnehin der gesamte Inhalt RAG-faehig sein soll.
     */
    ALL
}
