package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.event.EventQueueBatch;

@FunctionalInterface
public interface UpdateAction<T> {

	boolean update(Tx tx, T element, InternalActionContext ac, EventQueueBatch batch);
}
