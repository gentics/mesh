package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.model.RagAnswer;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;
import com.gentics.mesh.contentrag.common.spi.LlmProvider;
import com.gentics.mesh.contentrag.service.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SCAFFOLD-Platzhalter fuer das generative LLM (Konzept 5.5/5.8).
 * Profil-Default "cloud-hybrid" (EU-gehostet); Anbindung folgt nach Modellwahl (O4).
 */
@Component
public class StubLlmProvider implements LlmProvider {

    private final RagProperties properties;

    /**
     * @param properties RAG-Konfiguration (LLM-Profil/Modellkennung)
     */
    public StubLlmProvider(RagProperties properties) {
        this.properties = properties;
    }

    @Override
    public String modelId() {
        return properties.getLlm().getModel();
    }

    @Override
    public RagAnswer generate(RagQuery query, List<RetrievedChunk> context) {
        throw new UnsupportedOperationException(
                "SCAFFOLD: LlmProvider noch nicht implementiert (EU-Managed/lokal, Konzept 5.5).");
    }
}
