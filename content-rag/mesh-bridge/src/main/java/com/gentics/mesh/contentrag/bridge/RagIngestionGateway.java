package com.gentics.mesh.contentrag.bridge;

/**
 * Eintrittspunkt des RAG-Indexers fuer gepushte Mesh-Aenderungen.
 * Die Bridge (Mesh-seitig) ruft diese Schnittstelle auf; die Implementierung
 * liegt im rag-service. At-least-once-Push ist dank Idempotenz unkritisch (Konzept 12).
 */
public interface RagIngestionGateway {

    /**
     * Verarbeitet ein einzelnes Push-Event idempotent
     * (Selektion/No-RAG/Status-Pruefung -&gt; ggf. GraphQL-Read -&gt; contentHash-Vergleich
     * -&gt; Chunking -&gt; Embedding -&gt; Delete-by-Query der Gruppe -&gt; Bulk-Index -&gt; State-Update).
     */
    void handle(PushEvent event);
}
