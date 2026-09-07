package com.gentics.mesh.contentrag.meshsync;

/**
 * Betriebsparameter des Mesh-seitigen RAG-Syncs (Konfigurationszweig {@code search.rag}).
 *
 * @param enabled         Schaltet den RAG-Sync insgesamt. {@code false} muss <em>beide</em>
 *                        Handler stilllegen – Event-Pfad und {@code syncIndices()} –, sonst baut
 *                        ein Voll-Sync Indizes auf, die anschliessend niemand pflegt.
 * @param defaultMode     Verhalten fuer Schemas ohne eigene RAG-Angabe.
 * @param includeBinaries Nimmt {@code binary}- und {@code s3binary}-Felder mit auf, sofern das
 *                        Schema sie freigibt. Getrennt schaltbar, weil extrahierter Dokumenttext
 *                        inhaltlich hochwertig, aber potenziell sehr lang ist.
 */
public record RagSyncOptions(boolean enabled, RagSelectionMode defaultMode, boolean includeBinaries) {

    /** Vorgabe: aus, und ohne Schema-Angabe wird nichts indexiert. */
    public static final RagSyncOptions DEFAULTS =
            new RagSyncOptions(false, RagSelectionMode.NONE, false);

    /** Setzt {@code null} als {@code defaultMode} auf {@link RagSelectionMode#NONE}. */
    public RagSyncOptions {
        defaultMode = defaultMode == null ? RagSelectionMode.NONE : defaultMode;
    }
}
