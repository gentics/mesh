package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.model.ChunkState;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;
import com.gentics.mesh.contentrag.common.store.SyncStateStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SCAFFOLD: In-Memory-Implementierung des Chunk-State (Konzept 6.1/K6) fuer Entwicklung/Tests.
 * Produktiv-Default: eigener Elasticsearch-Index (optimistische Sperre via if_seq_no/if_primary_term).
 */
@Component
public class InMemorySyncStateStore implements SyncStateStore {

    private final Map<String, ChunkState> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ChunkState> find(DocumentGroupId groupId) {
        return Optional.ofNullable(store.get(groupId.asKey()));
    }

    @Override
    public void save(ChunkState state) {
        store.put(state.groupId().asKey(), state);
    }

    @Override
    public void delete(DocumentGroupId groupId) {
        store.remove(groupId.asKey());
    }
}
