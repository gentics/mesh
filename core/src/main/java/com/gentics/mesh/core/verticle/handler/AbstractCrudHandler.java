package com.gentics.mesh.core.verticle.handler;

import com.gentics.mesh.handler.ActionContext;

public abstract class AbstractCrudHandler extends AbstractHandler{

	abstract public void handleCreate(ActionContext ac);

	abstract public void handleDelete(ActionContext ac);

	abstract public void handleUpdate(ActionContext ac);

	abstract public void handleRead(ActionContext ac);

	abstract public void handleReadList(ActionContext ac);

}
