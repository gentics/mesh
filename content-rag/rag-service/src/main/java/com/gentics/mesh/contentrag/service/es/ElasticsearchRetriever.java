package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;
import com.gentics.mesh.contentrag.common.spi.EmbeddingProvider;
import com.gentics.mesh.contentrag.common.spi.Retriever;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hybrid-Retrieval (Konzept 5.7): BM25 + kNN, kombiniert per manueller Reciprocal Rank Fusion
 * (RRF, robust gegenueber unterschiedlichen Score-Skalen, lizenzunabhaengig). Permission-Filterung
 * exakt nach dem dokumentierten Mesh-Modell: terms-Filter auf {@code readRoleUuids} (+ "public"),
 * ohne Mesh-Roundtrip (K3).
 */
@Component
public class ElasticsearchRetriever implements Retriever {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchRetriever.class);
    private static final int RRF_K = 60;
    private static final String PUBLIC_ROLE = "public";

    private final ElasticsearchGateway es;
    private final EmbeddingProvider embeddingProvider;
    private final ObjectMapper mapper;
    private final String prefix;
    private final int defaultTopK;

    /**
     * @param es                ES-Gateway
     * @param embeddingProvider erzeugt das Query-Embedding fuer den kNN-Teil
     * @param properties        RAG-Konfiguration (Index-Praefix, Default-topK)
     */
    public ElasticsearchRetriever(ElasticsearchGateway es, EmbeddingProvider embeddingProvider,
                                  RagProperties properties) {
        this.es = es;
        this.embeddingProvider = embeddingProvider;
        this.mapper = es.mapper();
        this.prefix = properties.getElasticsearch().getIndexPrefix();
        this.defaultTopK = properties.getRetrieval().getTopK();
    }

    @Override
    public List<RetrievedChunk> retrieve(RagQuery query) {
        int topK = query.topK() != null && query.topK() > 0 ? query.topK() : defaultTopK;
        String indexExpr = indexExpression(query);
        ArrayNode filters = buildFilters(query);

        List<Hit> bm25 = runBm25(indexExpr, query.question(), filters, topK);

        List<Hit> knn = List.of();
        try {
            float[] vector = embeddingProvider.embedQuery(query.question());
            knn = runKnn(indexExpr, vector, filters, topK);
        } catch (RuntimeException e) {
            log.warn("kNN-Teil uebersprungen (Embedding/ES-Fehler): {} – nutze nur BM25.",
                    e.getMessage());
        }

        return fuse(bm25, knn, topK);
    }

    private String indexExpression(RagQuery query) {
        if (query.projectFilter().isEmpty()) {
            return ChunkIndexSupport.wildcard(prefix);
        }
        return query.projectFilter().stream()
                .map(p -> ChunkIndexSupport.indexName(prefix, p))
                .collect(Collectors.joining(","));
    }

    private ArrayNode buildFilters(RagQuery query) {
        ArrayNode filters = mapper.createArrayNode();

        // MVP: nur veroeffentlichte Inhalte.
        filters.add(term("publishStatus", "published"));

        if (!query.schemaFilter().isEmpty()) {
            filters.add(terms("schema", query.schemaFilter()));
        }
        if (query.language() != null && !query.language().isBlank()) {
            filters.add(term("language", query.language()));
        }
        if (query.pathPrefix() != null && !query.pathPrefix().isBlank()) {
            ObjectNode pre = mapper.createObjectNode();
            pre.putObject("prefix").put("path", query.pathPrefix());
            filters.add(pre);
        }

        // Gueltigkeitszeitraum zur Query-Zeit (V1): validFrom <= now (oder fehlend), validTo >= now (oder fehlend).
        String now = Instant.now().toString();
        filters.add(rangeOrMissing("validFrom", "lte", now));
        filters.add(rangeOrMissing("validTo", "gte", now));

        // Permission-Filter (K3): Nutzerrollen ODER public.
        filters.add(permissionFilter(query.userRoleUuids()));
        return filters;
    }

    private ObjectNode permissionFilter(List<String> roleUuids) {
        ArrayNode should = mapper.createArrayNode();
        if (roleUuids != null && !roleUuids.isEmpty()) {
            should.add(terms("readRoleUuids", roleUuids));
        }
        should.add(term("readRoleUuids", PUBLIC_ROLE));

        ObjectNode bool = mapper.createObjectNode();
        ObjectNode inner = bool.putObject("bool");
        inner.set("should", should);
        inner.put("minimum_should_match", 1);
        return bool;
    }

    private List<Hit> runBm25(String indexExpr, String question, ArrayNode filters, int topK) {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", topK);
        ObjectNode bool = body.putObject("query").putObject("bool");
        ObjectNode mm = mapper.createObjectNode();
        ObjectNode mmInner = mm.putObject("multi_match");
        mmInner.put("query", question);
        ArrayNode fields = mmInner.putArray("fields");
        fields.add("title^2");
        fields.add("chunkText");
        bool.set("must", mm);
        bool.set("filter", filters);
        return parseHits(es.search(indexExpr, body));
    }

    private List<Hit> runKnn(String indexExpr, float[] vector, ArrayNode filters, int topK) {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", topK);
        ObjectNode knn = body.putObject("knn");
        knn.put("field", "embedding");
        ArrayNode qv = knn.putArray("query_vector");
        for (float f : vector) {
            qv.add(f);
        }
        knn.put("k", topK);
        knn.put("num_candidates", Math.max(topK * 10, 100));
        knn.putObject("filter").putObject("bool").set("filter", filters.deepCopy());
        return parseHits(es.search(indexExpr, body));
    }

    private List<Hit> parseHits(JsonNode response) {
        List<Hit> hits = new ArrayList<>();
        JsonNode arr = response.path("hits").path("hits");
        if (arr.isArray()) {
            for (JsonNode h : arr) {
                hits.add(new Hit(h.path("_id").asText(), h.path("_score").asDouble(0.0),
                        h.path("_source")));
            }
        }
        return hits;
    }

    /** Reciprocal Rank Fusion ueber beide Trefferlisten; Score auf den Spitzenwert normalisiert. */
    private List<RetrievedChunk> fuse(List<Hit> bm25, List<Hit> knn, int topK) {
        Map<String, Double> rrf = new LinkedHashMap<>();
        Map<String, JsonNode> sources = new LinkedHashMap<>();

        accumulate(rrf, sources, bm25);
        accumulate(rrf, sources, knn);

        if (rrf.isEmpty()) {
            return List.of();
        }
        double max = rrf.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        return rrf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> new RetrievedChunk(
                        ChunkIndexSupport.fromSource(sources.get(e.getKey())),
                        max > 0 ? e.getValue() / max : 0.0))
                .collect(Collectors.toList());
    }

    private void accumulate(Map<String, Double> rrf, Map<String, JsonNode> sources, List<Hit> hits) {
        for (int i = 0; i < hits.size(); i++) {
            Hit hit = hits.get(i);
            rrf.merge(hit.id(), 1.0 / (RRF_K + i + 1), Double::sum);
            sources.putIfAbsent(hit.id(), hit.source());
        }
    }

    private ObjectNode term(String field, String value) {
        ObjectNode n = mapper.createObjectNode();
        n.putObject("term").put(field, value);
        return n;
    }

    private ObjectNode terms(String field, List<String> values) {
        ObjectNode n = mapper.createObjectNode();
        ArrayNode arr = n.putObject("terms").putArray(field);
        values.forEach(arr::add);
        return n;
    }

    private ObjectNode rangeOrMissing(String field, String op, String value) {
        ObjectNode wrapper = mapper.createObjectNode();
        ObjectNode bool = wrapper.putObject("bool");
        ArrayNode should = bool.putArray("should");

        ObjectNode range = mapper.createObjectNode();
        range.putObject("range").putObject(field).put(op, value);
        should.add(range);

        ObjectNode missing = mapper.createObjectNode();
        missing.putObject("bool").putObject("must_not").putObject("exists").put("field", field);
        should.add(missing);

        bool.put("minimum_should_match", 1);
        return wrapper;
    }

    private record Hit(String id, double score, JsonNode source) {
    }
}
