package com.gentics.mesh.context;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.event.EventQueueBatch;
import io.reactivex.Completable;

public interface BulkActionContext {

	/**
	 * Create a new context.
	 * 
	 * @return
	 */
	static BulkActionContext create() {
		return new BulkActionContextImpl();
	}

	/**
	 * Increment the counter which tracks deleted elements.
	 * 
	 * @return
	 */
	long inc();

	/**
	 * Process the deletion by processing the batch and committing the transaction.
	 */
	void process();

	/**
	 * Process the actions by processing the batch and committing the transaction. The method will also automatically increase the counter.
	 * 
	 * @param force
	 *            Force the commit / process even if the batch is not yet full
	 */
	void process(boolean force);

	/**
	 * Return the batch of this context.
	 * 
	 * @return
	 */
	EventQueueBatch batch();

	/**
	 * Shortcut for {@link #batch()#add(MeshEventModel)}
	 * 
	 * @param event
	 */
	default void add(MeshEventModel event) {
		batch().add(event);
	}

	/**
	 * Set the root cause of the action being invoked.
	 * 
	 * @param type
	 * @param uuid
	 * @param action
	 */
	default void setRootCause(ElementType type, String uuid, EventCauseAction action) {
		batch().setRootCause(type, uuid, action);
	}

	/**
	 * Add action which will be invoked once the context will be processed.
	 * 
	 * @param action
	 */
	void add(Completable action);

}
