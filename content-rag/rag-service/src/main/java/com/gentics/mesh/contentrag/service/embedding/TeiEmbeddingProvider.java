package com.gentics.mesh.contentrag.service.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.contentrag.common.spi.EmbeddingProvider;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedding-Provider gegen HuggingFace Text Embeddings Inference (TEI), lokal/CPU-faehig
 * (Konzept 5.5, Default-Profil). Spricht den {@code POST /embed}-Endpunkt an.
 *
 * <p>Beruecksichtigt die asymmetrischen e5-Prefixe: Passagen werden mit {@code "passage: "},
 * Anfragen mit {@code "query: "} versehen, sofern das Modell ein e5-Modell ist.
 */
@Component
public class TeiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(TeiEmbeddingProvider.class);
    private static final int MAX_ATTEMPTS = 3;

    private final RagProperties.Embedding config;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final boolean e5Prefixes;

    /**
     * @param properties RAG-Konfiguration (Embedding-Modell/Endpunkt/Batchgroesse)
     * @param mapper     Jackson-ObjectMapper fuer Request/Response-JSON
     */
    public TeiEmbeddingProvider(RagProperties properties, ObjectMapper mapper) {
        this.config = properties.getEmbedding();
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.e5Prefixes = config.getModel() != null && config.getModel().toLowerCase().contains("e5");
    }

    @Override
    public String modelId() {
        return config.getModel();
    }

    @Override
    public int dimension() {
        return config.getDimension();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        int batch = Math.max(1, config.getBatchSize());
        for (int i = 0; i < texts.size(); i += batch) {
            result.addAll(callTei(texts.subList(i, Math.min(i + batch, texts.size()))));
        }
        return result;
    }

    @Override
    public List<float[]> embedDocuments(List<String> texts) {
        return embed(withPrefix(texts, "passage: "));
    }

    @Override
    public float[] embedQuery(String text) {
        return embed(withPrefix(List.of(text), "query: ")).get(0);
    }

    private List<String> withPrefix(List<String> texts, String prefix) {
        if (!e5Prefixes) {
            return texts;
        }
        List<String> out = new ArrayList<>(texts.size());
        for (String t : texts) {
            out.add(prefix + t);
        }
        return out;
    }

    private List<float[]> callTei(List<String> batch) {
        ObjectNode body = mapper.createObjectNode();
        ArrayNode inputs = body.putArray("inputs");
        batch.forEach(inputs::add);
        body.put("normalize", true);
        body.put("truncate", true);

        String json = serialize(body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(config.getEndpoint() + "/embed"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response =
                        http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 != 2) {
                    throw new IllegalStateException(
                            "TEI HTTP " + response.statusCode() + ": " + response.body());
                }
                return parseVectors(response.body());
            } catch (Exception e) {
                last = new IllegalStateException("TEI-Embedding fehlgeschlagen (Versuch "
                        + attempt + "/" + MAX_ATTEMPTS + ")", e);
                log.warn("{}", last.getMessage());
                sleepBackoff(attempt);
            }
        }
        throw last;
    }

    private List<float[]> parseVectors(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            List<float[]> vectors = new ArrayList<>();
            for (JsonNode row : root) {
                float[] vec = new float[row.size()];
                for (int i = 0; i < row.size(); i++) {
                    vec[i] = (float) row.get(i).asDouble();
                }
                vectors.add(vec);
            }
            return vectors;
        } catch (Exception e) {
            throw new IllegalStateException("TEI-Antwort konnte nicht geparst werden", e);
        }
    }

    private String serialize(ObjectNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("JSON-Serialisierung fehlgeschlagen", e);
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
