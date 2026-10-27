package com.gentics.mesh.contentrag.service.setup;

import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.contentrag.index.RagIndexTemplate;
import com.gentics.mesh.contentrag.index.RagInferenceSetup;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import com.gentics.mesh.contentrag.service.es.ElasticsearchGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Legt beim Start die drei Cluster-Objekte an, die die Vektorisierung in Elasticsearch tragen:
 * Inference-Endpunkt, Ingest-Pipeline und Index-Template.
 *
 * <p>Warum hier und nicht in Mesh: es ist einmalige Elasticsearch-Administration, die vom
 * Embedding-Modell abhaengt – also von RAG-Wissen, das im CMS nichts verloren hat. Mesh legt
 * seine Indizes weiterhin so an wie bisher; das Template greift automatisch, sobald ein
 * Indexname auf das Pattern passt, und bringt Mapping und Pipeline mit. Damit braucht der
 * Sync-Pfad in Mesh kein Mapping-Wissen.
 *
 * <p>Die Reihenfolge ist bindend: Endpunkt vor Pipeline (die Pipeline referenziert ihn), und
 * beides vor dem Template. Ist ein Objekt bereits vorhanden, wird es nicht ueberschrieben –
 * ein Modell- oder Dimensionswechsel ist ein bewusster Eingriff samt Neuaufbau des Bestands
 * und darf nicht als Nebenwirkung eines Neustarts passieren.
 *
 * <p>Schlaegt der Bootstrap fehl, startet der Service trotzdem: der BM25-Teil des Retrievals
 * funktioniert auch ohne Vektoren, und ein Cluster, der beim Start kurz nicht erreichbar ist,
 * soll den Service nicht in eine Absturzschleife schicken.
 */
@Component
public class RagIndexBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RagIndexBootstrap.class);

    private final ElasticsearchGateway es;
    private final RagProperties properties;

    /**
     * @param es         ES-Gateway
     * @param properties RAG-Konfiguration
     */
    public RagIndexBootstrap(ElasticsearchGateway es, RagProperties properties) {
        this.es = es;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        RagProperties.Inference inference = properties.getInference();
        if (!inference.isBootstrap()) {
            log.info("Bootstrap deaktiviert – Inference-Endpunkt, Pipeline und Index-Template "
                    + "werden als vorhanden vorausgesetzt.");
            return;
        }
        try {
            ensureInferenceEndpoint(inference);
            ensurePipeline(inference);
            ensureIndexTemplate(inference);
        } catch (RuntimeException e) {
            log.warn("Bootstrap der RAG-Index-Infrastruktur fehlgeschlagen: {}. "
                    + "Retrieval laeuft bis zur Behebung ohne Vektorsuche.", e.getMessage());
        }
    }

    private void ensureInferenceEndpoint(RagProperties.Inference inference) {
        String path = "/_inference/text_embedding/" + inference.getId();
        if (es.existsAt(path)) {
            log.debug("Inference-Endpunkt {} vorhanden.", inference.getId());
            return;
        }
        Map<String, Object> body =
                RagInferenceSetup.inferenceEndpoint(inference.getUrl(), inference.getApiKey());
        es.put(path, es.mapper().valueToTree(body));
        log.info("Inference-Endpunkt {} angelegt (Ziel: {}).", inference.getId(), inference.getUrl());
    }

    private void ensurePipeline(RagProperties.Inference inference) {
        String path = "/_ingest/pipeline/" + inference.getPipelineId();
        if (es.existsAt(path)) {
            log.debug("Ingest-Pipeline {} vorhanden.", inference.getPipelineId());
            return;
        }
        Map<String, Object> body = RagInferenceSetup.ingestPipeline(inference.getId());
        es.put(path, es.mapper().valueToTree(body));
        log.info("Ingest-Pipeline {} angelegt.", inference.getPipelineId());
    }

    private void ensureIndexTemplate(RagProperties.Inference inference) {
        String prefix = properties.getElasticsearch().getIndexPrefix();
        String name = RagIndexNaming.templateName(prefix);
        String path = "/_index_template/" + name;
        if (es.existsAt(path)) {
            log.debug("Index-Template {} vorhanden.", name);
            return;
        }
        Map<String, Object> body = RagIndexTemplate.indexTemplate(prefix,
                inference.getPipelineId(), inference.getDimension(),
                inference.getShards(), inference.getReplicas());
        es.put(path, es.mapper().valueToTree(body));
        log.info("Index-Template {} fuer Pattern {} angelegt.", name,
                RagIndexNaming.templatePattern(prefix));
    }
}
