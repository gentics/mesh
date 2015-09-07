package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

public interface GraphNodeField extends ListableReferencingGraphField, MicroschemaListableGraphField {

	/**
	 * Returns the node for this field.
	 * 
	 * @param fieldKey The key for this field
	 * @return Node for this field when set, otherwise null.
	 */
	Node getNode();

	Field transformToRest(ActionContext ac, String fieldKey);

}
