package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.NodeFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;

public class NodeFieldListImpl extends AbstractReferencingFieldList<NodeField> implements NodeFieldList {

	@Override
	public NodeField createNode(String key, Node node) {
		return addItem(key, node);
	}

	@Override
	public Class<? extends NodeField> getListType() {
		return NodeFieldImpl.class;
	}

}
