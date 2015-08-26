package com.gentics.mesh.core.verticle.handler;

import io.vertx.ext.web.RoutingContext;

public abstract class AbstractCrudHandler extends AbstractHandler{

	abstract public void handleCreate(RoutingContext rc);

	abstract public void handleDelete(RoutingContext rc);

	abstract public void handleUpdate(RoutingContext rc);

	abstract public void handleRead(RoutingContext rc);

	abstract public void handleReadList(RoutingContext rc);

}
