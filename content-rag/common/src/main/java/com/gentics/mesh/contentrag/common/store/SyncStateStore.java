package com.gentics.mesh.contentrag.common.store;

import com.gentics.mesh.contentrag.common.model.ChunkState;
import com.gentics.mesh.contentrag.common.model.DocumentGroupId;

import java.util.Optional;

/**
 * Persistenz des schlanken Chunk-State (Konzept 6.1). Default-Backend: eigener
 * Elasticsearch-Index; optionales JPA-Backend auf die Mesh-RDBMS. Es genuegt
 * dokumentweise optimistische Sperre (if_seq_no/if_primary_term) pro Document-Group.
 */
public interface SyncStateStore {

    Optional<ChunkState> find(DocumentGroupId groupId);

    /** Idempotentes Upsert des State (read-after-write bei kritischen Schritten via refresh=wait_for). */
    void save(ChunkState state);

    void delete(DocumentGroupId groupId);
}
