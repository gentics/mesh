package com.gentics.mesh.contentrag.service.ingest;

import com.gentics.mesh.contentrag.bridge.PushEvent;
import com.gentics.mesh.contentrag.bridge.PushEventType;
import com.gentics.mesh.contentrag.bridge.RagIngestionGateway;
import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.ChunkProcessingStatus;
import com.gentics.mesh.contentrag.common.model.ChunkState;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.contentrag.common.spi.Chunker;
import com.gentics.mesh.contentrag.common.spi.ContentReader;
import com.gentics.mesh.contentrag.common.spi.EmbeddingProvider;
import com.gentics.mesh.contentrag.common.spi.Indexer;
import com.gentics.mesh.contentrag.common.store.SyncStateStore;
import com.gentics.mesh.contentrag.common.util.ChunkIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ingestion-Pipeline (Konzept 7). Implementiert das Push-Gateway: idempotente Verarbeitung
 * eines Push-Events mit No-Op-Erkennung ueber den Chunk-State (K5/K6).
 */
@Service
public class IngestionService implements RagIngestionGateway {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final ContentReader contentReader;
    private final Chunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final Indexer indexer;
    private final SyncStateStore syncStateStore;

    /**
     * @param contentReader     liest und normalisiert Mesh-Inhalte
     * @param chunker           strukturbasiertes Chunking
     * @param embeddingProvider Passagen-Embedding
     * @param indexer           Elasticsearch-Indexer
     * @param syncStateStore    Chunk-State fuer No-Op-Erkennung
     */
    public IngestionService(ContentReader contentReader, Chunker chunker,
                            EmbeddingProvider embeddingProvider, Indexer indexer,
                            SyncStateStore syncStateStore) {
        this.contentReader = contentReader;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.indexer = indexer;
        this.syncStateStore = syncStateStore;
    }

    @Override
    public void handle(PushEvent event) {
        DocumentGroupId group = event.groupId();
        if (event.type() == PushEventType.DELETE) {
            indexer.deleteGroup(group);
            syncStateStore.delete(group);
            log.debug("Document-Group {} geloescht", group.asKey());
            return;
        }

        Optional<SourceContent> maybe = contentReader.read(group);
        if (maybe.isEmpty()) {
            // Node nicht (mehr) freigegeben oder per No-RAG-Konvention ausgeschlossen (K4).
            indexer.deleteGroup(group);
            syncStateStore.delete(group);
            return;
        }
        SourceContent content = maybe.get();

        String contentHash = ChunkIds.contentHash(String.join("\n", content.markdownByField().values()));
        String model = embeddingProvider.modelId();

        Optional<ChunkState> state = syncStateStore.find(group);
        boolean needsWork = state.map(s -> s.needsReprocessing(contentHash, model)).orElse(true);
        if (!needsWork) {
            log.debug("No-Op fuer {} (contentHash unveraendert)", group.asKey());
            return;
        }

        List<Chunk> chunks = chunker.chunk(content);
        List<Chunk> embedded = embedAll(chunks, model);
        indexer.replaceGroup(group, embedded);

        syncStateStore.save(new ChunkState(
                group, content.projectUuid(), contentHash, content.meshVersion(),
                model, ChunkProcessingStatus.INDEXED, null, Instant.now()));
    }

    private List<Chunk> embedAll(List<Chunk> chunks, String model) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        List<String> texts = chunks.stream().map(Chunk::chunkText).toList();
        // Passagen-Embedding (e5-Prefix wird im Provider gesetzt), getrennt vom Query-Embedding.
        List<float[]> vectors = embeddingProvider.embedDocuments(texts);
        int dim = embeddingProvider.dimension();

        List<Chunk> result = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            result.add(withEmbedding(chunks.get(i), vectors.get(i), model, dim));
        }
        return result;
    }

    private static Chunk withEmbedding(Chunk c, float[] vector, String model, int dim) {
        return new Chunk(
                c.chunkId(), c.documentGroup(), c.nodeUuid(), c.projectUuid(), c.project(),
                c.branch(), c.language(), c.schema(), c.path(), c.title(), c.chunkText(),
                c.chunkIndex(), c.fieldPath(), vector, model, dim, c.meshVersion(), c.contentHash(),
                c.publishStatus(), c.validFrom(), c.validTo(), c.createdDate(), c.editedDate(),
                c.readRoleUuids(), c.sourceUrl(), c.referencedNodes(), c.editorialTags());
    }
}
