package com.gentics.mesh.contentrag.service.web;

import com.gentics.mesh.contentrag.bridge.FullSyncTrigger;
import com.gentics.mesh.contentrag.bridge.PushEvent;
import com.gentics.mesh.contentrag.bridge.PushEventType;
import com.gentics.mesh.contentrag.bridge.RagIngestionGateway;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.Feedback;
import com.gentics.mesh.contentrag.common.model.RagAnswer;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;
import com.gentics.mesh.contentrag.common.spi.Retriever;
import com.gentics.mesh.contentrag.common.store.FeedbackStore;
import com.gentics.mesh.contentrag.service.query.QueryService;
import com.gentics.mesh.contentrag.service.status.IndexStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * REST-API des RAG-Service (Konzept 9). Auth/Mandanten-/Rollenkontrolle ueber bestehende
 * CMP-/Mesh-Mechanismen wird in einer spaeteren Ausbaustufe vorgeschaltet (O5).
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final QueryService queryService;
    private final Retriever retriever;
    private final IndexStatusService statusService;
    private final FeedbackStore feedbackStore;
    private final RagIngestionGateway ingestionGateway;
    private final FullSyncTrigger fullSyncTrigger;

    /**
     * @param queryService     Query-Orchestrierung
     * @param retriever        Hybrid-Retriever (fuer Debug-Chunks)
     * @param statusService    Index-/Sync-Status
     * @param feedbackStore    Feedback-Persistenz
     * @param ingestionGateway Push-Gateway (node-bezogener Reindex)
     * @param fullSyncTrigger  Full-Sync-Anstoss (projektweiter Reindex)
     */
    public RagController(QueryService queryService, Retriever retriever,
                         IndexStatusService statusService, FeedbackStore feedbackStore,
                         RagIngestionGateway ingestionGateway, FullSyncTrigger fullSyncTrigger) {
        this.queryService = queryService;
        this.retriever = retriever;
        this.statusService = statusService;
        this.feedbackStore = feedbackStore;
        this.ingestionGateway = ingestionGateway;
        this.fullSyncTrigger = fullSyncTrigger;
    }

    /** Frage + Filter -> Antwort + Quellen (optional Debug-Chunk-Scores). */
    @PostMapping("/query")
    public RagAnswer query(@Valid @RequestBody QueryRequest request) {
        return queryService.answer(request.toQuery());
    }

    /** Voller oder node-bezogener Reindex. Projektweit ueber Mesh-Full-Sync-Reuse (K6). */
    @PostMapping("/reindex")
    public ResponseEntity<Void> reindex(@Valid @RequestBody ReindexRequest request) {
        if (request.nodeUuid() != null && !request.nodeUuid().isBlank()) {
            DocumentGroupId group = new DocumentGroupId(
                    request.nodeUuid(),
                    request.branch() == null ? "main" : request.branch(),
                    request.language() == null ? "en" : request.language());
            ingestionGateway.handle(new PushEvent(
                    PushEventType.UPSERT, group, request.projectUuid(), null, null, List.of()));
        } else {
            fullSyncTrigger.triggerFullSync(request.projectUuid());
        }
        return ResponseEntity.accepted().build();
    }

    /** Index-/Sync-Status, Zaehler, Fehler. */
    @GetMapping("/status")
    public IndexStatus status(@RequestParam(required = false) String projectUuid) {
        return statusService.current(projectUuid);
    }

    /** Quellen-/Chunk-Inspektion (Debugging): rohe Retrieval-Treffer fuer eine Frage. */
    @GetMapping("/debug/chunks")
    public List<RetrievedChunk> debugChunks(@RequestParam String question,
                                            @RequestParam(required = false) String language,
                                            @RequestParam(required = false) Integer topK) {
        RagQuery query = new RagQuery(question, language, List.of(), List.of(), List.of(),
                null, topK, false, true);
        return retriever.retrieve(query);
    }

    /** Nutzerfeedback je Antwort. */
    @PostMapping("/feedback")
    public ResponseEntity<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        feedbackStore.save(new Feedback(
                UUID.randomUUID().toString(), Instant.now(),
                request.auditEntryId(), request.projectUuid(), request.helpful(), request.comment()));
        return ResponseEntity.accepted().build();
    }
}
