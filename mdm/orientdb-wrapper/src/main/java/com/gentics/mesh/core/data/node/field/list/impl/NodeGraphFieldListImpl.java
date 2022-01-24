package com.gentics.mesh.core.data.node.field.list.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;

/**
 * @see NodeGraphFieldList
 */
public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<HibNodeField, NodeFieldList, HibNode> implements NodeGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodeGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibNodeField createNode(String key, HibNode node) {
		return addItem(key, toGraph(node));
	}

	@Override
	public Class<? extends HibNodeField> getListType() {
		return NodeGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext context) {
		// We only need to remove the vertex. The entry are edges which will automatically be removed.
		getElement().remove();
	}
}
