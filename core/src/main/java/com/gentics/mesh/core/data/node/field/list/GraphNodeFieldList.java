package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

public interface GraphNodeFieldList extends GraphListField<GraphNodeField, NodeFieldList> {

	public static final String TYPE = "node";

	GraphNodeField createNode(String key, Node node);

}
