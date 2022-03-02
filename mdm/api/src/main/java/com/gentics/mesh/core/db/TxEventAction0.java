package com.gentics.mesh.core.db;

import com.gentics.mesh.event.EventQueueBatch;

/**
 * like {@link java.util.function.BiConsumer<EventQueueBatch, Tx>}, but it can throw checked exceptions
 */
@FunctionalInterface
public interface TxEventAction0 {

	/**
	 * Applies the function to the given arguments
	 * @param batch
	 * @param tx
	 * @throws Exception
	 */
	void handle(EventQueueBatch batch, Tx tx) throws Exception;

}
