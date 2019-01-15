package com.gentics.mesh.context.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.search.SearchQueueBatch;

public class DummyBulkActionContext implements BulkActionContext {

	@Override
	public long inc() {
		return 0;
	}

	@Override
	public void process() {

	}

	@Override
	public void process(boolean force) {

	}

	@Override
	public SearchQueueBatch batch() {
		return new DummySearchQueueBatch();
	}

	@Override
	public void dropIndex(String composeIndexName) {

	}

}
