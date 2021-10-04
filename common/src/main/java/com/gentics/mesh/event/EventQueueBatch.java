package com.gentics.mesh.event;

import java.util.List;
import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.MeshEventModel;

/**
 * A batch of event queue entries.
 */
public interface EventQueueBatch {
	/**
	 * Name of the event header identifying the Mesh instance, which is sending the event
	 */
	public final static String SENDER_HEADER = "mesh.sender";

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
	 * Return a list of all actions in this batch.
	 * 
	 * @return
	 */
	List<Runnable> getActions();

	/**
	 * Dispatch events for all entries in the batch.
	 */
	void dispatch();

	/**
	 * Clear all entries.
	 */
	default void clear() {
		getEntries().clear();
		getActions().clear();
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
		getEntries().add(event);
		return this;
	}

	default EventQueueBatch add(Runnable action) {
		Objects.requireNonNull(action);
		getActions().add(action);
		return this;
	}

	default void addAll(EventQueueBatch containerBatch) {
		getEntries().addAll(containerBatch.getEntries());
	}

	/**
	 * Return the root info that was assigned to the batch. Each batch has a root action which created the batch.
	 * 
	 * @return
	 */
	EventCauseInfo getCause();

	/**
	 * Set a basic root cause for all events in the batch.
	 * 
	 * @param type
	 * @param uuid
	 * @param action
	 */
	void setCause(ElementType type, String uuid, EventCauseAction action);

	/**
	 * Set the root cause info.
	 * 
	 * @param cause
	 */
	void setCause(EventCauseInfo cause);

}
