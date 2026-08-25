package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;
import com.gentics.mesh.contentrag.common.spi.Retriever;
import com.gentics.mesh.contentrag.index.RagChunkFields;
import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.contentrag.index.RagInferenceSetup;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hybrid-Retrieval (Konzept 5.7): BM25 und kNN, kombiniert per Reciprocal Rank Fusion. RRF ist
 * gegenueber den sehr unterschiedlichen Score-Skalen beider Verfahren robust und kommt ohne
 * lizenzpflichtige Elasticsearch-Funktionen aus.
 *
 * <p>Den Frage-Vektor erzeugt Elasticsearch selbst ueber {@code query_vector_builder} – mit
 * demselben Inference-Endpunkt, der beim Indexieren die Chunk-Vektoren erzeugt. Dadurch kann
 * ein Modellwechsel Index- und Query-Seite nicht mehr auseinanderlaufen lassen, und der Service
 * braucht keinen eigenen Embedding-Client.
 *
 * <p>Der Permission-Filter arbeitet auf {@link RagChunkFields#ROLE_UUIDS} und damit auf
 * demselben Feld und derselben Semantik wie die Mesh-Volltextsuche (K3) – ohne Mesh-Roundtrip.
 */
@Component
public class ElasticsearchRetriever implements Retriever {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchRetriever.class);
    private static final int RRF_K = 60;

    private final ElasticsearchGateway es;
    private final ObjectMapper mapper;
    private final String prefix;
    private final String inferenceId;
    private final int defaultTopK;

    /**
     * @param es         ES-Gateway
     * @param properties RAG-Konfiguration (Index-Praefix, Inference-Endpunkt, Default-topK)
     */
    public ElasticsearchRetriever(ElasticsearchGateway es, RagProperties properties) {
        this.es = es;
        this.mapper = es.mapper();
        this.prefix = properties.getElasticsearch().getIndexPrefix();
        this.inferenceId = properties.getInference().getId();
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
            knn = runKnn(indexExpr, query.question(), filters, topK);
        } catch (RuntimeException e) {
            // Faellt die Vektorsuche aus (Inference-Endpunkt nicht erreichbar oder nicht
            // lizenziert), bleibt die Volltextsuche als brauchbare Rueckfallebene.
            log.warn("kNN-Teil uebersprungen ({}) – Antwort basiert nur auf BM25.", e.getMessage());
        }

        return fuse(bm25, knn, topK);
    }

    /**
     * Zielindizes: nur veroeffentlichte Inhalte (MVP-Abgrenzung). Die Einschraenkung erfolgt
     * ueber den Indexnamen statt ueber einen Filter – Mesh fuehrt Draft und Published ohnehin
     * in getrennten Indizes, und was gar nicht erst durchsucht wird, kostet auch nichts.
     */
    private String indexExpression(RagQuery query) {
        if (query.projectFilter().isEmpty()) {
            return RagIndexNaming.allIndices(prefix, RagIndexNaming.PUBLISHED);
        }
        return query.projectFilter().stream()
                .map(p -> RagIndexNaming.projectIndices(prefix, p, RagIndexNaming.PUBLISHED))
                .collect(Collectors.joining(","));
    }

    private ArrayNode buildFilters(RagQuery query) {
        ArrayNode filters = mapper.createArrayNode();

        if (!query.schemaFilter().isEmpty()) {
            filters.add(terms(RagChunkFields.SCHEMA, query.schemaFilter()));
        }
        if (query.language() != null && !query.language().isBlank()) {
            filters.add(term(RagChunkFields.LANGUAGE, query.language()));
        }
        if (query.pathPrefix() != null && !query.pathPrefix().isBlank()) {
            ObjectNode prefixFilter = mapper.createObjectNode();
            prefixFilter.putObject("prefix").put(RagChunkFields.PATH, query.pathPrefix());
            filters.add(prefixFilter);
        }

        // Gueltigkeitszeitraum zur Query-Zeit (V1): validFrom <= jetzt, validTo >= jetzt,
        // fehlende Angaben gelten als unbeschraenkt.
        String now = Instant.now().toString();
        filters.add(rangeOrMissing(RagChunkFields.VALID_FROM, "lte", now));
        filters.add(rangeOrMissing(RagChunkFields.VALID_TO, "gte", now));

        filters.add(permissionFilter(query.userRoleUuids()));
        return filters;
    }

    private ObjectNode permissionFilter(List<String> roleUuids) {
        ArrayNode should = mapper.createArrayNode();
        if (roleUuids != null && !roleUuids.isEmpty()) {
            should.add(terms(RagChunkFields.ROLE_UUIDS, roleUuids));
        }
        should.add(term(RagChunkFields.ROLE_UUIDS, RagChunkFields.PUBLIC_ROLE));

        ObjectNode wrapper = mapper.createObjectNode();
        ObjectNode bool = wrapper.putObject("bool");
        bool.set("should", should);
        bool.put("minimum_should_match", 1);
        return wrapper;
    }

    private List<Hit> runBm25(String indexExpr, String question, ArrayNode filters, int topK) {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", topK);
        body.set("_source", sourceExcludes());
        ObjectNode bool = body.putObject("query").putObject("bool");

        ObjectNode multiMatch = mapper.createObjectNode();
        ObjectNode inner = multiMatch.putObject("multi_match");
        inner.put("query", question);
        ArrayNode fields = inner.putArray("fields");
        fields.add(RagChunkFields.TITLE + "^2");
        fields.add(RagChunkFields.CHUNK_TEXT);

        bool.set("must", multiMatch);
        bool.set("filter", filters);
        return parseHits(es.search(indexExpr, body));
    }

    private List<Hit> runKnn(String indexExpr, String question, ArrayNode filters, int topK) {
        ObjectNode body = mapper.createObjectNode();
        body.put("size", topK);
        body.set("_source", sourceExcludes());

        ObjectNode knn = body.putObject("knn");
        knn.put("field", RagChunkFields.EMBEDDING);
        knn.set("query_vector_builder",
                mapper.valueToTree(RagInferenceSetup.queryVectorBuilder(inferenceId, question)));
        knn.put("k", topK);
        knn.put("num_candidates", Math.max(topK * 10, 100));
        knn.putObject("filter").putObject("bool").set("filter", filters.deepCopy());
        return parseHits(es.search(indexExpr, body));
    }

    /** Der Vektor wird nie zurueckgeliefert – er waere der groesste Teil der Antwort. */
    private ObjectNode sourceExcludes() {
        ObjectNode source = mapper.createObjectNode();
        source.putArray("excludes").add(RagChunkFields.EMBEDDING);
        return source;
    }

    private List<Hit> parseHits(JsonNode response) {
        List<Hit> hits = new ArrayList<>();
        JsonNode array = response.path("hits").path("hits");
        if (array.isArray()) {
            for (JsonNode hit : array) {
                hits.add(new Hit(hit.path("_id").asText(), hit.path("_source")));
            }
        }
        return hits;
    }

    /** Reciprocal Rank Fusion ueber beide Trefferlisten; Score auf den Spitzenwert normalisiert. */
    private List<RetrievedChunk> fuse(List<Hit> bm25, List<Hit> knn, int topK) {
        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, JsonNode> sources = new LinkedHashMap<>();

        accumulate(scores, sources, bm25);
        accumulate(scores, sources, knn);

        if (scores.isEmpty()) {
            return List.of();
        }
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> new RetrievedChunk(
                        ChunkSources.fromSource(sources.get(entry.getKey())),
                        max > 0 ? entry.getValue() / max : 0.0))
                .collect(Collectors.toList());
    }

    private void accumulate(Map<String, Double> scores, Map<String, JsonNode> sources,
                            List<Hit> hits) {
        for (int rank = 0; rank < hits.size(); rank++) {
            Hit hit = hits.get(rank);
            scores.merge(hit.id(), 1.0 / (RRF_K + rank + 1), Double::sum);
            sources.putIfAbsent(hit.id(), hit.source());
        }
    }

    private ObjectNode term(String field, String value) {
        ObjectNode node = mapper.createObjectNode();
        node.putObject("term").put(field, value);
        return node;
    }

    private ObjectNode terms(String field, List<String> values) {
        ObjectNode node = mapper.createObjectNode();
        ArrayNode array = node.putObject("terms").putArray(field);
        values.forEach(array::add);
        return node;
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

    private record Hit(String id, JsonNode source) {
    }
}
