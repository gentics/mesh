package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;

public interface GraphNodeFieldList extends GraphListField<GraphNodeField> {

	public static final String TYPE = "node";

	GraphNodeField createNode(String key, Node node);

}
