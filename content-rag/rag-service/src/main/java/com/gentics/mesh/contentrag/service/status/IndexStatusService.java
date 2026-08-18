package com.gentics.mesh.contentrag.service.status;

import com.gentics.mesh.contentrag.service.web.IndexStatus;
import org.springframework.stereotype.Service;

/**
 * Liefert Index-/Sync-Status (Konzept 5.10). SCAFFOLD: liefert noch Platzhalterwerte;
 * spaeter Aggregation ueber Chunk-Index und Chunk-State (Elasticsearch-Aggregationen).
 */
@Service
public class IndexStatusService {

    /**
     * Liefert den aktuellen Index-/Sync-Status.
     *
     * @param projectUuid optionaler Mandant; {@code null} fuer den Gesamtstatus
     * @return Statuswerte (Zaehler, Fehler, letzter Sync)
     */
    public IndexStatus current(String projectUuid) {
        // TODO: Counts/Fehler aus Chunk-Index und SyncStateStore aggregieren.
        return new IndexStatus("scaffold", 0L, 0L, 0L, null);
    }
}
