package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.handler.InternalActionContext;

public abstract class AbstractCrudHandler extends AbstractHandler {

	abstract public void handleCreate(InternalActionContext ac);

	abstract public void handleDelete(InternalActionContext ac);

	abstract public void handleUpdate(InternalActionContext ac);

	abstract public void handleRead(InternalActionContext ac);

	abstract public void handleReadList(InternalActionContext ac);

}
