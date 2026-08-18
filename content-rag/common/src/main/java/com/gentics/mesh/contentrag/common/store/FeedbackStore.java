package com.gentics.mesh.contentrag.common.store;

import com.gentics.mesh.contentrag.common.model.Feedback;

/**
 * Persistenz des Nutzerfeedbacks (Konzept 5.10/6.1). Default-Backend:
 * eigener Elasticsearch-Index; optional JPA.
 */
public interface FeedbackStore {

    void save(Feedback feedback);
}
