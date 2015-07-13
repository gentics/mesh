package com.gentics.mesh.core.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import com.gentics.mesh.core.data.service.transformation.TransformationParameters;
import com.gentics.mesh.core.rest.common.AbstractRestModel;

public interface TransformableNode<T extends AbstractRestModel> {

	GenericNode<T> transformToRest(MeshAuthUser requestUser, Handler<AsyncResult<T>> handler, TransformationParameters... parameters);

}
