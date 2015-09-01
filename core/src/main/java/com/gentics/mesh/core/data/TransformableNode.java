package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface TransformableNode<T extends RestModel> {

	GenericVertex<T> transformToRest(ActionContext ac, Handler<AsyncResult<T>> handler);

}
