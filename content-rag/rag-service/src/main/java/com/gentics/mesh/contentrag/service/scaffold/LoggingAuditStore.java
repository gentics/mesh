package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.model.AuditEntry;
import com.gentics.mesh.contentrag.common.store.AuditStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SCAFFOLD: protokolliert Audit-Eintraege (Konzept 5.10). Produktiv-Default:
 * eigener Elasticsearch-Index (Aggregationen fuer Dashboards), DSGVO-Aufbewahrungsfristen.
 */
@Component
public class LoggingAuditStore implements AuditStore {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditStore.class);

    @Override
    public void append(AuditEntry entry) {
        log.info("AUDIT id={} project={} sources={} roles={}",
                entry.id(), entry.projectUuid(), entry.sourceChunkIds().size(),
                entry.checkedRoleUuids().size());
    }
}
