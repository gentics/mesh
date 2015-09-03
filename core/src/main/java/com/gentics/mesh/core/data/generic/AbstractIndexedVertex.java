package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.IndexedVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.util.UUIDUtil;

public abstract class AbstractIndexedVertex<T extends RestModel> extends AbstractGenericVertex<T>implements IndexedVertex {

	@Override
	public SearchQueueBatch addIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		// TODO use a dedicated uuid or timestamp for batched to avoid collisions
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		batch.addEntry(this, action);
		addRelatedEntries(batch);
		return batch;
	}

}
