package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.store.ConfigStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SCAFFOLD: In-Memory-Implementierung der mandantenspezifischen Konfiguration (Konzept 6.1).
 * Produktiv-Default: eigener Elasticsearch-Index; optional JPA.
 */
@Component
public class InMemoryConfigStore implements ConfigStore {

    private final Map<String, Map<String, String>> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String projectUuid, String key) {
        return Optional.ofNullable(store.getOrDefault(projectUuid, Map.of()).get(key));
    }

    @Override
    public void put(String projectUuid, String key, String value) {
        store.computeIfAbsent(projectUuid, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    @Override
    public Map<String, String> all(String projectUuid) {
        return Map.copyOf(store.getOrDefault(projectUuid, Map.of()));
    }
}
