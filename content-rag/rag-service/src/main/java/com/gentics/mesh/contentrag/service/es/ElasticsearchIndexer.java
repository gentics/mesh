package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.spi.EmbeddingProvider;
import com.gentics.mesh.contentrag.common.spi.Indexer;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch-Indexer (Konzept 5.6). Topologie Index-pro-Projekt (V3). Idempotenz ueber
 * Group-Delete-and-Replace (K5): Delete-by-Query auf {@code documentGroup} + Bulk-Index.
 */
@Component
public class ElasticsearchIndexer implements Indexer {

    private final ElasticsearchGateway es;
    private final ObjectMapper mapper;
    private final String prefix;
    private final int dimension;

    /**
     * @param es                ES-Gateway
     * @param embeddingProvider liefert die Vektordimension fuer das Index-Mapping
     * @param properties        RAG-Konfiguration (Index-Praefix)
     */
    public ElasticsearchIndexer(ElasticsearchGateway es, EmbeddingProvider embeddingProvider,
                                RagProperties properties) {
        this.es = es;
        this.mapper = es.mapper();
        this.prefix = properties.getElasticsearch().getIndexPrefix();
        this.dimension = embeddingProvider.dimension();
    }

    @Override
    public void replaceGroup(DocumentGroupId groupId, List<Chunk> chunks) {
        if (chunks.isEmpty()) {
            deleteGroup(groupId);
            return;
        }
        String projectUuid = chunks.get(0).projectUuid();
        String index = ChunkIndexSupport.indexName(prefix, projectUuid);

        es.createIndex(index, ChunkIndexSupport.buildMapping(mapper, dimension));
        es.deleteByQuery(index, termQuery("documentGroup", groupId.asKey()));

        List<ObjectNode> docs = new ArrayList<>(chunks.size());
        for (Chunk c : chunks) {
            docs.add(ChunkIndexSupport.toDocument(mapper, c));
        }
        es.bulkIndex(index, docs);
    }

    @Override
    public void deleteGroup(DocumentGroupId groupId) {
        // Projektunabhaengig ueber Wildcard, da die Gruppe das Projekt nicht traegt.
        es.deleteByQuery(ChunkIndexSupport.wildcard(prefix), termQuery("documentGroup", groupId.asKey()));
    }

    @Override
    public void deleteProject(String projectUuid) {
        es.deleteIndex(ChunkIndexSupport.indexName(prefix, projectUuid));
    }

    private ObjectNode termQuery(String field, String value) {
        ObjectNode q = mapper.createObjectNode();
        q.putObject("term").put(field, value);
        return q;
    }
}
