package com.gentics.mesh.core.db;

import java.util.Optional;

import com.gentics.mesh.dagger.BaseMeshComponent;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A developer extension API for {@link TxData}.
 * 
 * @author plyhun
 *
 */
public interface CommonTxData extends TxData {

	/**
	 * Root Mesh component access.
	 * 
	 * @return
	 */
	BaseMeshComponent mesh();

	/**
	 * Install a queue batch into the transaction.
	 * 
	 * @param batch
	 */
	void setEventQueueBatch(EventQueueBatch batch);

	/**
	 * Get a queue batch, if previously set.
	 * 
	 * @return
	 */
	Optional<EventQueueBatch> maybeGetEventQueueBatch();

	/**
	 * Check if Vert.x is settled
	 * 
	 * @return
	 */
	boolean isVertxReady();

	/**
	 * Remove the queue batch from the transaction, if there was any.
	 * 
	 * @param batch
	 */
	default void suppressEventQueueBatch() {
		setEventQueueBatch(null);
	}

	/**
	 * Get a queue batch, if previously set. Otherwise create and set a new batch.
	 * 
	 * @return
	 */
	default EventQueueBatch getOrCreateEventQueueBatch() {
		return maybeGetEventQueueBatch().orElseGet(() -> {
			EventQueueBatch batch = mesh().batchProvider().get();
			setEventQueueBatch(batch);
			return batch;
		});
	}
}
