package com.gentics.mesh.core.db;

import com.gentics.mesh.event.EventQueueBatch;

@FunctionalInterface
/**
 * like {@link java.util.function.BiFunction<EventQueueBatch, Tx, T>}, but it can throw checked exceptions
 */
public interface TxEventAction<T> {

	/**
	 * Applies the function to the given arguments
	 * @param batch
	 * @param tx
	 * @return the function result
	 * @throws Exception
	 */
	T handle(EventQueueBatch batch, Tx tx) throws Exception;

}
