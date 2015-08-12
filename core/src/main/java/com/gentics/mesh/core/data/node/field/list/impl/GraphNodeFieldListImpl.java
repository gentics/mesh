package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.GraphNodeFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;

public class GraphNodeFieldListImpl extends AbstractReferencingGraphFieldList<GraphNodeField> implements GraphNodeFieldList {

	@Override
	public GraphNodeField createNode(String key, Node node) {
		return addItem(key, node);
	}

	@Override
	public Class<? extends GraphNodeField> getListType() {
		return GraphNodeFieldImpl.class;
	}
	
	@Override
	public void delete() {
		// TODO Auto-generated method stub
		
	}

}
