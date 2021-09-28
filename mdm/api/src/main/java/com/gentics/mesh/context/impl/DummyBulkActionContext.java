package com.gentics.mesh.context.impl;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.event.EventQueueBatch;
import io.reactivex.Completable;

/**
 * Test bulk action context which does not process entries.
 */
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
	public void setRootCause(ElementType type, String uuid, EventCauseAction action) {
	}

	@Override
	public void add(Completable action) {
	}

}
