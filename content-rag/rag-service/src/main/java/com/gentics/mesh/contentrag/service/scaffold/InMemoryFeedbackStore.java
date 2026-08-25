package com.gentics.mesh.contentrag.service.scaffold;

import com.gentics.mesh.contentrag.common.model.Feedback;
import com.gentics.mesh.contentrag.common.store.FeedbackStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SCAFFOLD: In-Memory-Feedback (Konzept 5.10). Produktiv-Default: eigener Elasticsearch-Index.
 */
@Component
public class InMemoryFeedbackStore implements FeedbackStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryFeedbackStore.class);
    private final List<Feedback> store = new CopyOnWriteArrayList<>();

    @Override
    public void save(Feedback feedback) {
        store.add(feedback);
        log.info("FEEDBACK auditEntry={} helpful={}", feedback.auditEntryId(), feedback.helpful());
    }
}
