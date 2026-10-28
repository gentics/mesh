package com.gentics.mesh.contentrag.common.store;

import com.gentics.mesh.contentrag.common.model.AuditEntry;

/**
 * Append-lastige Persistenz des Audit-Logs (Konzept 5.10/6.1). Profitiert von
 * Elasticsearch-Aggregationen (Dashboards). DSGVO-Aufbewahrungs-/Loeschfristen beachten.
 */
public interface AuditStore {

    void append(AuditEntry entry);
}
