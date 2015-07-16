package com.gentics.mesh.core.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public interface TransformableNode<T extends AbstractRestModel> {

	GenericVertex<T> transformToRest(RoutingContext rc, Handler<AsyncResult<T>> handler);

}
