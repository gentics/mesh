package com.gentics.mesh.contentrag.service.query;

import com.gentics.mesh.contentrag.common.model.AuditEntry;
import com.gentics.mesh.contentrag.common.model.RagAnswer;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;
import com.gentics.mesh.contentrag.common.spi.LlmProvider;
import com.gentics.mesh.contentrag.common.spi.Retriever;
import com.gentics.mesh.contentrag.common.store.AuditStore;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestriert die Query-Pipeline (Konzept 7/5.7/5.8):
 * Hybrid-Retrieval (mit Rollen-Filter) -&gt; Relevanz-Schwellwert -&gt; LLM (nur belegte Inhalte) -&gt; Audit.
 */
@Service
public class QueryService {

    private final Retriever retriever;
    private final LlmProvider llmProvider;
    private final AuditStore auditStore;
    private final RagProperties properties;

    /**
     * @param retriever   Hybrid-Retriever
     * @param llmProvider generatives LLM
     * @param auditStore  Audit-Persistenz
     * @param properties  RAG-Konfiguration (Relevanz-Schwellwert)
     */
    public QueryService(Retriever retriever, LlmProvider llmProvider,
                        AuditStore auditStore, RagProperties properties) {
        this.retriever = retriever;
        this.llmProvider = llmProvider;
        this.auditStore = auditStore;
        this.properties = properties;
    }

    /**
     * Beantwortet eine Anfrage: Retrieval, Schwellwert-Pruefung, Antwortgenerierung und Audit.
     *
     * @param query Anfrage inkl. Rollen-/Metadatenfilter
     * @return quellenbelegte Antwort oder die definierte Nicht-Antwort bei fehlender Basis
     */
    public RagAnswer answer(RagQuery query) {
        List<RetrievedChunk> retrieved = retriever.retrieve(query);

        double minScore = properties.getRetrieval().getMinScore();
        List<RetrievedChunk> grounded = retrieved.stream()
                .filter(rc -> rc.score() >= minScore)
                .toList();

        RagAnswer answer;
        if (grounded.isEmpty()) {
            // "Keine Antwort ohne Quellenbasis" (V5) – sprachspezifischer Text in spaeterer Ausbaustufe.
            answer = RagAnswer.noSufficientSources(
                    "Zu dieser Frage liegen keine ausreichend belegten Inhalte vor.");
        } else {
            answer = llmProvider.generate(query, grounded);
        }

        audit(query, answer);
        return answer;
    }

    private void audit(RagQuery query, RagAnswer answer) {
        AuditEntry entry = new AuditEntry(
                UUID.randomUUID().toString(),
                Instant.now(),
                null, // pseudonymisierte Nutzerreferenz: in spaeterer Ausbaustufe aus Auth-Kontext
                query.projectFilter().isEmpty() ? null : query.projectFilter().get(0),
                query.question(),
                answer.sources().stream().map(s -> s.chunkId()).toList(),
                query.userRoleUuids(),
                false, // Antwort-Logging opt-in (O7)
                null);
        auditStore.append(entry);
    }
}
