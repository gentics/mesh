package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;

public interface GraphNodeField extends ListableReferencingGraphField, MicroschemaListableGraphField {

	/**
	 * Returns the node for this field.
	 * 
	 * @return Node for this field when set, otherwise null.
	 */
	Node getNode();

}
