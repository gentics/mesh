package com.gentics.mesh.contentrag.common.spi;

import com.gentics.mesh.contentrag.common.model.RagAnswer;
import com.gentics.mesh.contentrag.common.model.RagQuery;
import com.gentics.mesh.contentrag.common.model.RetrievedChunk;

import java.util.List;

/**
 * Abstraktion ueber das generative LLM (Konzept 5.5/5.8). Deployment-Profile:
 * <ul>
 *   <li>"Cloud/Hybrid" (Default): EU-gehostetes Managed-LLM (Azure OpenAI EU, Mistral EU, ...)</li>
 *   <li>"Sovereign/On-Prem": vollstaendig lokal (Ollama/vLLM), GPU/RAM noetig</li>
 * </ul>
 * Antwort ausschliesslich aus den uebergebenen Chunks (Groundedness, V5).
 */
public interface LlmProvider {

    /** Kennung des verwendeten Modells/Profils (Audit/Monitoring). */
    String modelId();

    /**
     * Erzeugt eine quellenbelegte Antwort aus dem bereitgestellten Kontext.
     *
     * @param query   urspruengliche Anfrage (Sprache steuert Prompt/Nicht-Antwort-Text)
     * @param context bereits gefilterte, re-gerankte Chunks (nur belegte Inhalte)
     * @return Antwort mit Quellen; bei leerer/zu schwacher Basis eine Nicht-Antwort
     */
    RagAnswer generate(RagQuery query, List<RetrievedChunk> context);
}
