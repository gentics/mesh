package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface BasicGraphField<T extends Field> extends MicroschemaListableGraphField {

	/**
	 * Transform the graph field to the rest field.
	 * 
	 * @param ac
	 * @param handler
	 */
	void transformToRest(ActionContext ac, Handler<AsyncResult<T>> handler);

}
