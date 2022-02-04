package com.gentics.mesh.core.db;

import com.gentics.mesh.event.EventQueueBatch;

@FunctionalInterface
public interface TxEventAction0 {

	void handle(EventQueueBatch batch, Tx tx) throws Exception;

}
