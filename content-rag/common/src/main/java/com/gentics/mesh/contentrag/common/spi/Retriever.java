package com.gentics.mesh.contentrag.common.spi;

import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;

import java.util.List;

/**
 * Hybrid-Retrieval (Konzept 5.7): BM25 + kNN via RRF (Elasticsearch Retriever-API),
 * kombiniert mit Metadaten- und Rollen-Filter (K3 – exakt wie die Mesh-Volltextsuche).
 * Nur fuer den Nutzer sichtbare Chunks gelangen ins Ergebnis und damit vor das LLM.
 */
public interface Retriever {

    /**
     * @param query enthaelt Frage, Sprache, Filter und die Rollen-UUIDs des Anfragenden
     * @return nach Score absteigend sortierte, berechtigungsgefilterte Treffer (max. topK)
     */
    List<RetrievedChunk> retrieve(RagQuery query);
}
