package com.gentics.mesh.event;

import java.util.List;

import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;

import io.reactivex.Completable;

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
	Completable dispatch();

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
		getEntries().add(event);
		return this;
	}

	default void addAll(EventQueueBatch containerBatch) {
		getEntries().addAll(containerBatch.getEntries());
	}

}
