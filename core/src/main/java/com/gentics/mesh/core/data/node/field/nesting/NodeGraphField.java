package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface NodeGraphField extends ListableReferencingGraphField, MicroschemaListableGraphField {

	/**
	 * Returns the node for this field.
	 * 
	 * @param fieldKey
	 *            The key for this field
	 * @return Node for this field when set, otherwise null.
	 */
	Node getNode();

	/**
	 * Transform the graph field into a rest field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param handler
	 */
	void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<Field>> handler);

}
