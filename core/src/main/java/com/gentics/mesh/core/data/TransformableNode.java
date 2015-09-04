package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * A {@link TransformableNode} is a node that can be transformed into a rest model response.
 *
 * @param <T>
 *            RestModel response class
 */
public interface TransformableNode<T extends RestModel> {

	/**
	 * Transform the node into the matching rest model response.
	 * 
	 * @param ac
	 *            Context of the calling action
	 * @param handler
	 *            Handler that is invoked when transformation failed or succeeded.
	 * @return
	 */
	GenericVertex<T> transformToRest(ActionContext ac, Handler<AsyncResult<T>> handler);

}
