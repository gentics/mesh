package com.gentics.mesh.event;

import java.util.List;
import java.util.Objects;

import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;

/**
 * A batch of event queue entries.
 */
public interface EventQueueBatch {

	/**
	 * Return the id of the batch.
	 * 
	 * @return
	 */
	String getBatchId();

	/**
	 * Return a list of entries for this batch.
	 *
	 * @return
	 */
	List<MeshEventModel> getEntries();

	/**
	 * Dispatch events for all entries in the batch.
	 */
	void dispatch();

	/**
	 * Create a new event queue batch.
	 * 
	 * @return
	 */
	static EventQueueBatch create() {
		return new EventQueueBatchImpl();
	}

	/**
	 * Clear all entries.
	 */
	default void clear() {
		getEntries().clear();
	}

	/**
	 * Return the current size of the batch.
	 */
	default int size() {
		return getEntries().size();
	}

	/**
	 * Add the event to the batch.
	 * 
	 * @param event
	 * @param Fluent
	 *            API
	 */
	default EventQueueBatch add(MeshEventModel event) {
		Objects.requireNonNull(event);
		Objects.requireNonNull(event.getEvent(), "The event model does not contain the event info");
		event.setCause(getCause());
		getEntries().add(event);
		return this;
	}

	default void addAll(EventQueueBatch containerBatch) {
		getEntries().addAll(containerBatch.getEntries());
	}

	void setRootCause(String type, String uuid, String action);

	/**
	 * Return the root info that was assigned to the batch. Each batch has a root action which created the batch.
	 * 
	 * @return
	 */
	EventCauseInfo getCause();

}
