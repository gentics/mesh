package com.gentics.mesh.contentrag.index;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Definitionen fuer die Vektorisierung <em>innerhalb</em> von Elasticsearch.
 *
 * <p>Warum dort und nicht beim Schreiber: Mesh soll seinen Standard-Sync unveraendert fahren.
 * Wuerde Mesh die Vektoren selbst erzeugen, haenge der Sync-Durchsatz am Embedder – bei einer
 * Schema-Migration ueber Millionen Nodes waere das der limitierende Faktor. Erzeugt stattdessen
 * eine Ingest-Pipeline den Vektor beim Indexieren, schreibt Mesh nur Text und bleibt schnell.
 *
 * <p>Bewusst <strong>Ingest-Pipeline statt {@code semantic_text}</strong>: {@code semantic_text}
 * wuerde den Text ein zweites Mal chunken (wort-/satzbasiert) und damit das strukturbasierte
 * Chunking entwerten, das entlang von Ueberschriften und Feldgrenzen schneidet. Die Pipeline
 * vektorisiert genau den Chunk, den wir uebergeben.
 *
 * <p>Rueckfallebene: faellt die Inference-Seite von Elasticsearch aus Lizenzgruenden weg, wird
 * ausschliesslich diese Klasse ersetzt – das Dokumentformat, das Mapping und die Query-Seite
 * bleiben unveraendert. Der Vektor kaeme dann vom Schreiber, das Feld {@link RagChunkFields#EMBEDDING}
 * bliebe dasselbe.
 */
public final class RagInferenceSetup {

    private RagInferenceSetup() {
    }

    /**
     * Body fuer {@code PUT _inference/text_embedding/<inferenceId>}: ein Endpunkt, der auf einen
     * selbst betriebenen Text-Embeddings-Inference-Server (TEI) zeigt.
     *
     * @param teiUrl vollstaendige URL des TEI-Embed-Endpunkts, z. B. {@code http://tei:80/embed}
     * @param apiKey Schluessel; von TEI ignoriert, von der Elasticsearch-Konfiguration jedoch
     *               verlangt – bei lokalem Betrieb genuegt ein Platzhalter
     * @return Definition des Inference-Endpunkts
     */
    public static Map<String, Object> inferenceEndpoint(String teiUrl, String apiKey) {
        Map<String, Object> serviceSettings = new LinkedHashMap<>();
        serviceSettings.put("url", teiUrl);
        serviceSettings.put("api_key", apiKey == null || apiKey.isBlank() ? "not-used" : apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "hugging_face");
        body.put("service_settings", serviceSettings);
        return body;
    }

    /**
     * Body fuer {@code PUT _ingest/pipeline/<pipelineId>}: haengt an jedes eingehende
     * Chunk-Dokument den Vektor an.
     *
     * <p>{@code ignore_missing} laesst Dokumente ohne Text unveraendert passieren,
     * {@code on_failure} sorgt dafuer, dass ein Fehler des Embedders das Dokument nicht
     * verwirft – es wird ohne Vektor indexiert und ist damit weiterhin ueber BM25 auffindbar.
     * Das Feld {@code embeddingError} macht solche Faelle auswertbar.
     *
     * @param inferenceId ID des Inference-Endpunkts
     * @return Definition der Ingest-Pipeline
     */
    public static Map<String, Object> ingestPipeline(String inferenceId) {
        Map<String, Object> inputOutput = new LinkedHashMap<>();
        inputOutput.put("input_field", RagChunkFields.CHUNK_TEXT);
        inputOutput.put("output_field", RagChunkFields.EMBEDDING);

        Map<String, Object> inference = new LinkedHashMap<>();
        inference.put("model_id", inferenceId);
        inference.put("input_output", List.of(inputOutput));
        inference.put("ignore_missing", true);

        Map<String, Object> setError = new LinkedHashMap<>();
        setError.put("field", "embeddingError");
        setError.put("value", "{{ _ingest.on_failure_message }}");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "Vektorisiert RAG-Chunks beim Indexieren (Gentics Mesh Content RAG).");
        body.put("processors", List.of(Map.of("inference", inference)));
        body.put("on_failure", List.of(Map.of("set", setError)));
        return body;
    }

    /**
     * Query-seitiges Gegenstueck: laesst Elasticsearch den Frage-Vektor mit
     * <em>demselben</em> Endpunkt erzeugen. Dadurch gibt es genau eine Stelle, an der das
     * Embedding-Modell konfiguriert ist – ein Modellwechsel kann Index- und Query-Seite nicht
     * mehr auseinanderlaufen lassen.
     *
     * @param inferenceId ID des Inference-Endpunkts
     * @param question    Fragetext
     * @return Wert fuer das Feld {@code query_vector_builder} einer kNN-Suche
     */
    public static Map<String, Object> queryVectorBuilder(String inferenceId, String question) {
        Map<String, Object> textEmbedding = new LinkedHashMap<>();
        textEmbedding.put("model_id", inferenceId);
        textEmbedding.put("model_text", question);
        return Map.of("text_embedding", textEmbedding);
    }
}
