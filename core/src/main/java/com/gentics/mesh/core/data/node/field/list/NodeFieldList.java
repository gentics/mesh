package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;

public interface NodeFieldList extends ListField<NodeField> {

	NodeField createNode(String key, Node node);

}
