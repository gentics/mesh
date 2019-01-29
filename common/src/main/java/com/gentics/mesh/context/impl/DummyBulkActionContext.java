package com.gentics.mesh.context.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.event.EventQueueBatch;

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
	public EventQueueBatch batch() {
		return new DummyEventQueueBatch();
	}

	@Override
	public void dropIndex(String composeIndexName) {

	}

}
