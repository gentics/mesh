package com.gentics.mesh.contentrag.common.store;

import java.util.Map;
import java.util.Optional;

/**
 * Persistenz der mandantenspezifischen Konfiguration (Konzept 6.1): Projekt-/Schema-/Feldauswahl,
 * No-RAG-Konvention, Chunking-/Retrieval-Parameter, Provider-Profile, Prompts.
 * Default-Backend: eigener Elasticsearch-Index; optional JPA.
 */
public interface ConfigStore {

    Optional<String> get(String projectUuid, String key);

    void put(String projectUuid, String key, String value);

    /** Alle Konfigurationswerte eines Mandanten. */
    Map<String, String> all(String projectUuid);
}
