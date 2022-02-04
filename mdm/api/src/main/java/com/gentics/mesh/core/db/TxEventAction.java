package com.gentics.mesh.core.db;

import com.gentics.mesh.event.EventQueueBatch;

@FunctionalInterface
public interface TxEventAction<T> {

	T handle(EventQueueBatch batch, Tx tx) throws Exception;

}
