package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.event.EventQueueBatch;

@FunctionalInterface
public interface CreateAction<T> {

	T create(Tx tx, InternalActionContext ac, EventQueueBatch batch, String uuid);

}
