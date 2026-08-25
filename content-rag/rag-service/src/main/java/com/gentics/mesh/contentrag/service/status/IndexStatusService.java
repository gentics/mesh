package com.gentics.mesh.contentrag.service.status;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.contentrag.index.RagChunkFields;
import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import com.gentics.mesh.contentrag.service.es.ElasticsearchGateway;
import com.gentics.mesh.contentrag.service.web.IndexStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Index-Status (Konzept 5.10). Da Mesh den Sync-Fortschritt selbst kennt, beschraenkt sich der
 * Service auf das, was er im Index tatsaechlich sieht: wie viele Chunks vorliegen und wie viele
 * davon keinen Vektor haben.
 *
 * <p>Die zweite Zahl ist die wichtigere: sie zaehlt Chunks, bei denen die Ingest-Pipeline den
 * Vektor nicht erzeugen konnte. Solche Chunks sind ueber BM25 auffindbar, fehlen aber der
 * Vektorsuche – ein Zustand, der ohne diese Zahl unbemerkt bliebe, weil das System weiter
 * Antworten liefert, nur schlechtere.
 */
@Service
public class IndexStatusService {

    private static final Logger log = LoggerFactory.getLogger(IndexStatusService.class);

    private final ElasticsearchGateway es;
    private final String prefix;

    /**
     * @param es         ES-Gateway
     * @param properties RAG-Konfiguration (Index-Praefix)
     */
    public IndexStatusService(ElasticsearchGateway es, RagProperties properties) {
        this.es = es;
        this.prefix = properties.getElasticsearch().getIndexPrefix();
    }

    /**
     * Liefert den aktuellen Index-Status.
     *
     * @param projectUuid optionaler Mandant; {@code null} fuer alle Projekte
     * @return Zaehler ueber die veroeffentlichten RAG-Chunk-Indizes
     */
    public IndexStatus current(String projectUuid) {
        String indexExpr = projectUuid == null || projectUuid.isBlank()
                ? RagIndexNaming.allIndices(prefix, RagIndexNaming.PUBLISHED)
                : RagIndexNaming.projectIndices(prefix, projectUuid, RagIndexNaming.PUBLISHED);
        try {
            long chunks = es.count(indexExpr, null);
            long withoutVector = es.count(indexExpr, missingEmbedding());
            String status = chunks == 0 ? "leer" : withoutVector == 0 ? "ok" : "unvollstaendig";
            return new IndexStatus(status, groupCount(indexExpr), chunks, withoutVector, null);
        } catch (RuntimeException e) {
            log.warn("Statusabfrage fuer {} fehlgeschlagen: {}", indexExpr, e.getMessage());
            return new IndexStatus("nicht verfuegbar", 0L, 0L, 0L, null);
        }
    }

    /** Anzahl unterschiedlicher Document-Groups ueber eine Cardinality-Aggregation (Naeherung). */
    private long groupCount(String indexExpr) {
        ObjectNode body = es.mapper().createObjectNode();
        body.put("size", 0);
        body.putObject("aggs").putObject("groups").putObject("cardinality")
                .put("field", RagChunkFields.DOCUMENT_GROUP);
        return es.search(indexExpr, body).path("aggregations").path("groups")
                .path("value").asLong(0L);
    }

    private ObjectNode missingEmbedding() {
        ObjectNode query = es.mapper().createObjectNode();
        query.putObject("bool").putObject("must_not").putObject("exists")
                .put("field", RagChunkFields.EMBEDDING);
        return query;
    }
}
