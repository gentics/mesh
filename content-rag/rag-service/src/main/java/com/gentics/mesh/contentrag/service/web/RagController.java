package com.gentics.mesh.contentrag.service.web;

import com.gentics.mesh.contentrag.common.model.RagAnswer;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;
import com.gentics.mesh.contentrag.common.spi.Retriever;
import com.gentics.mesh.contentrag.service.query.QueryService;
import com.gentics.mesh.contentrag.service.status.IndexStatusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-API des RAG-Service (Konzept 9).
 *
 * <p>Rein lesend. Einen Reindex-Endpunkt gibt es bewusst nicht mehr: Befuellung und Voll-Sync
 * laufen ueber Mesh ({@code POST /api/v2/search/sync}), damit es fuer den Index genau einen
 * Schreibweg gibt. Ein zweiter Ausloeser hier waere ein zweites Sync-Paradigma – genau das,
 * was der Umbau beseitigt hat.
 *
 * <p>Einen Feedback-Endpunkt gibt es bewusst nicht: sinnvoll auswertbar waere er erst mit einer
 * Referenz auf den Audit-Eintrag der Antwort, und die gibt {@code RagAnswer} nicht heraus.
 * Solange keine Oberflaeche Feedback erhebt, waere er totes Gewicht in der API-Oberflaeche.
 * Wenn er kommt, dann zusammen mit der Audit-ID in der Antwort und persistenten Stores.
 *
 * <p>Auth und Mandantentrennung ueber die bestehenden CMP-/Mesh-Mechanismen sind noch
 * vorzuschalten (O5).
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final QueryService queryService;
    private final Retriever retriever;
    private final IndexStatusService statusService;

    /**
     * @param queryService  Query-Orchestrierung
     * @param retriever     Hybrid-Retriever (fuer die Chunk-Inspektion)
     * @param statusService Index-Status
     */
    public RagController(QueryService queryService, Retriever retriever,
                         IndexStatusService statusService) {
        this.queryService = queryService;
        this.retriever = retriever;
        this.statusService = statusService;
    }

    /** Frage und Filter zu Antwort und Quellen. */
    @PostMapping("/query")
    public RagAnswer query(@Valid @RequestBody QueryRequest request) {
        return queryService.answer(request.toQuery());
    }

    /** Zaehler ueber die RAG-Chunk-Indizes. */
    @GetMapping("/status")
    public IndexStatus status(@RequestParam(required = false) String projectUuid) {
        return statusService.current(projectUuid);
    }

    /** Quellen- und Chunk-Inspektion: rohe Retrieval-Treffer zu einer Frage. */
    @GetMapping("/debug/chunks")
    public List<RetrievedChunk> debugChunks(@RequestParam String question,
                                            @RequestParam(required = false) String language,
                                            @RequestParam(required = false) Integer topK) {
        RagQuery query = new RagQuery(question, language, List.of(), List.of(), List.of(),
                null, topK, false, true);
        return retriever.retrieve(query);
    }
}
