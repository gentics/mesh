package com.gentics.mesh.context.impl;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.EventCauseInfoImpl;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.event.EventQueueBatch;

import java.util.List;

/**
 * Dummy search queue batch which can be used to avoid creation of unwanted batch entries. This is useful if a drop index is more efficient then removing each
 * entry individually. (e.g.: project deletion)
 */
public class DummyEventQueueBatch implements EventQueueBatch {

	@Override
	public String getBatchId() {
		return null;
	}

	@Override
	public void dispatch() {

	}

	@Override
	public List<MeshEventModel> getEntries() {
		return null;
	}

	@Override
	public List<Runnable> getActions() {
		return null;
	}

	@Override
	public EventCauseInfoImpl getCause() {
		return null;
	}

	@Override
	public void setCause(ElementType type, String uuid, EventCauseAction action) {

	}

	@Override
	public void setCause(EventCauseInfo cause) {

	}

	@Override
	public EventQueueBatch add(MeshEventModel event) {
		return this;
	}

	@Override
	public void addAll(EventQueueBatch containerBatch) {

	}

	@Override
	public void clear() {

	}

	@Override
	public int size() {
		return 0;
	}
}