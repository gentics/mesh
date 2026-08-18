package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.bridge.FullSyncTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SCAFFOLD-Platzhalter fuer den Full-Sync-Trigger (Konzept K6/7).
 * Spaeter: Durchreichen an Mesh' {@code POST /api/v2/search/sync} (Reuse statt eigener Reconciler).
 * Abhaengig von O8 (Hook-Variante).
 */
@Component
public class StubFullSyncTrigger implements FullSyncTrigger {

    private static final Logger log = LoggerFactory.getLogger(StubFullSyncTrigger.class);

    @Override
    public void triggerFullSync(String projectUuid) {
        log.warn("SCAFFOLD: Full-Sync fuer Projekt {} angefordert – Mesh-Anbindung noch offen (O8).",
                projectUuid);
    }
}
