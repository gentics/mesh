package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

public interface IndexedVertex {

	SearchQueueBatch addIndexBatch(SearchQueueEntryAction action);

	void addRelatedEntries(SearchQueueBatch batch);

}
