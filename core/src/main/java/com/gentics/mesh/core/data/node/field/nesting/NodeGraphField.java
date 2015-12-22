package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

public interface NodeGraphField extends ListableReferencingGraphField, MicroschemaListableGraphField {

	/**
	 * Returns the node for this field.
	 * 
	 * @return Node for this field when set, otherwise null.
	 */
	Node getNode();

	/**
	 * Transform the graph field into a rest field.
	 * 
	 * @param ac
	 * @param fieldKey
	 */
	Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey);

}
