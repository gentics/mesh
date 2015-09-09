package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

public interface NodeGraphFieldList extends ListGraphField<NodeGraphField, NodeFieldList> {

	public static final String TYPE = "node";

	NodeGraphField createNode(String key, Node node);

}
